/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.submission

import config.FrontendAppConfig
import connectors.ConstructionIndustrySchemeConnector
import models.monthlyreturns.{CisTaxpayer, UpdateMonthlyReturnItemRequest}
import models.requests.*
import models.submission.*
import models.UserAnswers
import pages.agent.AgentClientDataPage
import pages.amend.AmendmentDetailsPage
import pages.monthlyreturns.*
import pages.submission.*
import play.api.Logging
import play.api.i18n.Lang
import play.api.libs.json.JsValue
import play.api.mvc.AnyContent
import repositories.SessionRepository
import services.MonthlyReturnItemPayloadBuilder
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeFormats
import utils.TypeUtils.*

import java.time.{Clock, Instant, LocalDateTime, YearMonth, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

@Singleton
class SubmissionService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  appConfig: FrontendAppConfig,
  sessionRepository: SessionRepository,
  chrisRequestBuilder: ChrisSubmissionRequestBuilder,
  itemPayloadBuilder: MonthlyReturnItemPayloadBuilder,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends Logging {

  private val ukZone: ZoneId = ZoneId.of("Europe/London")

  def getOrCreateSubmissionForChris(
    ua: UserAnswers
  )(implicit hc: HeaderCarrier): Future[(String, UserAnswers, Boolean)] =
    ua.get(ResubmissionIdPage) match {
      case Some(resubmissionId) =>
        Future.successful((resubmissionId.toString, ua, true))
      case None                 =>
        existingCreatedSubmissionId(ua) match {
          case Some(submissionId) =>
            // The submission was already created, so this is a revisit or a retry. The return has left STARTED, so
            // item updates would be rejected, and a second create would break the unique submission key. Reuse it.
            logger.warn(
              s"[SubmissionService][getOrCreateSubmissionForChris] - Submission already created, reusing submissionId: $submissionId"
            )
            Future.successful((submissionId, ua, false))
          case None               =>
            refreshMonthlyReturnItems(ua)
              .flatMap(_ => create(ua))
              .map { case (created, updatedUa) =>
                (created.submissionId, updatedUa, false)
              }
        }
    }

  // Multi-tab: Get or create a submission for the ID-scoped journey.
  def getOrCreateSubmissionForChris(
    ua: UserAnswers,
    id: String
  )(implicit hc: HeaderCarrier): Future[(String, UserAnswers, Boolean)] =
    ua.get(ResubmissionIdPage(id)) match {
      case Some(resubmissionId) =>
        Future.successful((resubmissionId.toString, ua, true))
      case None                 =>
        existingCreatedSubmissionId(ua, id) match {
          case Some(submissionId) =>
            logger.warn(
              s"[SubmissionService][getOrCreateSubmissionForChris] - Submission already created, reusing submissionId: $submissionId"
            )
            Future.successful((submissionId, ua, false))
          case None               =>
            refreshMonthlyReturnItems(ua, id)
              .flatMap(_ => create(ua, id))
              .map { case (created, updatedUa) =>
                (created.submissionId, updatedUa, false)
              }
        }
    }

  private def existingCreatedSubmissionId(ua: UserAnswers): Option[String] = {
    val period = selectedYearMonth(ua).toString
    if (ua.get(SubmissionCreatedPage(period)).contains(true)) ua.get(CreatedSubmissionIdPage(period)) else None
  }

  // Multi-tab: Get an existing submission ID for the ID-scoped journey.
  private def existingCreatedSubmissionId(ua: UserAnswers, id: String): Option[String] = {
    val period = selectedYearMonth(ua, id).toString
    if (ua.get(SubmissionCreatedForFlowPage(id, period)).contains(true))
      ua.get(CreatedSubmissionIdForFlowPage(id, period))
    else None
  }

  /** Writes the subcontractor name and verification number onto every stored monthly return item before the return is
    * submitted.
    *
    * A return started in the legacy service stores payment details on its items but not the name or verification
    * number, which the legacy service only adds at submission. When such a return is continued in this service, the
    * check/change answers pages (the only places the item update is called) can be skipped, so the stored items would
    * stay blank. The submission sent to ChRIS is built from the user's answers and is not affected, but the stored
    * items feed the submitted return details page. Rewriting each item here, using the same payload as those pages,
    * covers every route to submission.
    *
    * Items are refreshed one at a time, not in parallel. Each backend item update also bumps a version counter on the
    * scheme and fails with a lost-update error if another update to the same scheme overlaps it, which would roll back
    * the item's name. Running the updates concurrently would therefore lose all but one of them.
    *
    * Every item must be refreshed. A failed update is retried once; if it fails again, or the payload cannot be built,
    * the returned future fails and no submission is created, so a return is never submitted with blank item names.
    * Stopping at the first such item also avoids making hundreds of calls that would all fail when the backend rejects
    * the return (its stored procedure only accepts a STARTED return).
    */
  def refreshMonthlyReturnItems(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] = {
    val indexes: Seq[Int] =
      ua.get(SelectedSubcontractorPage.all).map(_.keys.toSeq.sorted).getOrElse(Seq.empty)

    indexes.foldLeft(Future.unit)((previous, index) => previous.flatMap(_ => refreshMonthlyReturnItem(ua, index)))
  }

  // Multi-tab: Refresh monthly return items for the ID-scoped journey.
  def refreshMonthlyReturnItems(ua: UserAnswers, id: String)(implicit hc: HeaderCarrier): Future[Unit] = {
    val indexes: Seq[Int] =
      ua.get(SelectedSubcontractorPage.all(id)).map(_.keys.toSeq.sorted).getOrElse(Seq.empty)

    indexes.foldLeft(Future.unit)((previous, index) => previous.flatMap(_ => refreshMonthlyReturnItem(ua, id, index)))
  }

  private def refreshMonthlyReturnItem(ua: UserAnswers, index: Int)(implicit hc: HeaderCarrier): Future[Unit] =
    itemPayloadBuilder.build(ua, index) match {
      case None          =>
        val message = s"Could not build item payload, so the item was not refreshed, index: $index"
        logger.error(s"[SubmissionService][refreshMonthlyReturnItem] - $message")
        Future.failed(new IllegalStateException(message))
      case Some(payload) =>
        cisConnector.updateMonthlyReturnItem(payload).recoverWith { case NonFatal(first) =>
          logger.warn(
            s"[SubmissionService][refreshMonthlyReturnItem] - Item update failed, retrying once. ${describe(payload, index)}, message: ${first.getMessage}"
          )
          cisConnector.updateMonthlyReturnItem(payload).recoverWith { case NonFatal(second) =>
            val message =
              s"Failed to refresh monthly return item. ${describe(payload, index)}, message: ${second.getMessage}"
            logger.error(s"[SubmissionService][refreshMonthlyReturnItem] - $message")
            Future.failed(new IllegalStateException(message, second))
          }
        }
    }

  // Multi-tab: Refresh a monthly return item for the ID-scoped journey.
  private def refreshMonthlyReturnItem(ua: UserAnswers, id: String, index: Int)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    itemPayloadBuilder.build(ua, id, index) match {
      case None =>
        val message = s"Could not build item payload, so the item was not refreshed, index: $index"
        logger.error(s"[SubmissionService][refreshMonthlyReturnItem] - $message")
        Future.failed(new IllegalStateException(message))

      case Some(payload) =>
        cisConnector.updateMonthlyReturnItem(payload).recoverWith { case NonFatal(first) =>
          logger.warn(
            s"[SubmissionService][refreshMonthlyReturnItem] - Item update failed, retrying once. ${describe(payload, index)}, message: ${first.getMessage}"
          )
          cisConnector.updateMonthlyReturnItem(payload).recoverWith { case NonFatal(second) =>
            val message =
              s"Failed to refresh monthly return item. ${describe(payload, index)}, message: ${second.getMessage}"
            logger.error(s"[SubmissionService][refreshMonthlyReturnItem] - $message")
            Future.failed(new IllegalStateException(message, second))
          }
        }
    }

  private def describe(payload: UpdateMonthlyReturnItemRequest, index: Int): String =
    s"instanceId: ${payload.instanceId}, taxYear: ${payload.taxYear}, taxMonth: ${payload.taxMonth}, " +
      s"amendment: ${payload.amendment}, index: $index, subcontractorId: ${payload.subcontractorId}"

  def create(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[(CreateSubmissionResponse, UserAnswers)] =
    for {
      req            <- buildCreateRequest(ua)
      response       <- cisConnector.createSubmission(req)
      period          = selectedYearMonth(ua).toString
      withCreated    <- Future.fromTry(ua.set(SubmissionCreatedPage(period), true))
      updatedAnswers <- Future.fromTry(withCreated.set(CreatedSubmissionIdPage(period), response.submissionId))
      _              <- sessionRepository.set(updatedAnswers)
    } yield (response, updatedAnswers)

  // Multi-tab: Create a submission for the ID-scoped journey.
  def create(ua: UserAnswers, id: String)(implicit hc: HeaderCarrier): Future[(CreateSubmissionResponse, UserAnswers)] =
    for {
      req            <- buildCreateRequest(ua, id)
      response       <- cisConnector.createSubmission(req)
      period          = selectedYearMonth(ua, id).toString
      withCreated    <- Future.fromTry(ua.set(SubmissionCreatedForFlowPage(id, period), true))
      updatedAnswers <-
        Future.fromTry(withCreated.set(CreatedSubmissionIdForFlowPage(id, period), response.submissionId))
      _              <- sessionRepository.set(updatedAnswers)
    } yield (response, updatedAnswers)

  def submitToChrisAndPersist(
    submissionId: String,
    ua: UserAnswers,
    isAgent: Boolean,
    isResubmission: Boolean
  )(implicit hc: HeaderCarrier): Future[ChrisSubmissionResponse] =

    val taxpayerFut: Future[CisTaxpayer] =
      if (isAgent)
        ua.get(AgentClientDataPage) match {
          case Some(agentClientData) =>
            cisConnector.getAgentClientTaxpayer(
              agentClientData.taxOfficeNumber,
              agentClientData.taxOfficeReference
            )
          case None                  =>
            Future.failed(new RuntimeException("Agent client data missing"))
        }
      else
        cisConnector.getCisTaxpayer()

    val requiredAnswers = for {
      cisId      <- ua.get(CisIdPage)
      taxDate    <- ua.get(DateConfirmPaymentsPage)
      isAmendment = ua.get(AmendmentDetailsPage).isDefined
    } yield (cisId, taxDate.getMonthValue, taxDate.getYear, isAmendment)

    for {
      taxpayer                                <- taxpayerFut
      (cisId, taxMonth, taxYear, isAmendment) <-
        requiredAnswers.fold(
          Future.failed[(String, Int, Int, Boolean)](
            new RuntimeException("Month and year of return missing")
          )
        )(Future.successful)
      monthlyReturn                           <- cisConnector.retrieveMonthlyReturnForEditDetails(
                                                   GetMonthlyReturnForEditRequest(cisId, taxMonth, taxYear, isAmendment)
                                                 )
      csr                                      = chrisRequestBuilder.build(ua, taxpayer, isAgent, monthlyReturn, isResubmission)
      response                                <- cisConnector.submitToChris(submissionId, csr)
      amendment                                = monthlyReturn.monthlyReturn.headOption.flatMap(_.amendment)
      _                                       <- writeToFeMongo(ua, submissionId, response, amendment)
    } yield response

  // Multi-tab: Submit the ID-scoped monthly return to ChRIS.
  def submitToChrisAndPersist(
    submissionId: String,
    ua: UserAnswers,
    id: String,
    isAgent: Boolean,
    isResubmission: Boolean
  )(implicit hc: HeaderCarrier): Future[ChrisSubmissionResponse] = {

    val taxpayerFut: Future[CisTaxpayer] =
      if (isAgent)
        ua.get(AgentClientDataPage(id)) match {
          case Some(agentClientData) =>
            cisConnector.getAgentClientTaxpayer(
              agentClientData.taxOfficeNumber,
              agentClientData.taxOfficeReference
            )
          case None                  =>
            Future.failed(new RuntimeException("Agent client data missing"))
        }
      else
        cisConnector.getCisTaxpayer()

    val requiredAnswers = for {
      cisId      <- ua.get(CisIdPage(id))
      taxDate    <- ua.get(DateConfirmPaymentsPage(id))
      returnType <- ua.get(ReturnTypePage(id))
      isAmendment = returnType.amendmentFlag == "Y"
    } yield (cisId, taxDate.getMonthValue, taxDate.getYear, isAmendment)

    for {
      taxpayer                                <- taxpayerFut
      (cisId, taxMonth, taxYear, isAmendment) <-
        requiredAnswers.fold(
          Future.failed[(String, Int, Int, Boolean)](
            new RuntimeException("CIS ID, month or year of return missing")
          )
        )(Future.successful)
      monthlyReturn                           <- cisConnector.retrieveMonthlyReturnForEditDetails(
                                                   GetMonthlyReturnForEditRequest(cisId, taxMonth, taxYear, isAmendment)
                                                 )
      csr                                      = chrisRequestBuilder.build(ua, id, taxpayer, isAgent, monthlyReturn, isResubmission)
      response                                <- cisConnector.submitToChris(submissionId, csr)
      amendment                                = monthlyReturn.monthlyReturn.headOption.flatMap(_.amendment)
      _                                       <- writeToFeMongo(ua, id, submissionId, response, amendment)
    } yield response
  }

  def updateSubmissionFromChrisResponse(
    submissionId: String,
    ua: UserAnswers,
    chrisResp: ChrisSubmissionResponse
  )(implicit req: CisIdDataRequest[AnyContent], hc: HeaderCarrier): Future[Unit] = updateSubmission(
    submissionId,
    ua,
    chrisResp.hmrcMarkGenerated,
    chrisResp.status,
    chrisResp.acceptedTime,
    None,
    chrisResp.error,
    chrisResp.govTalkErrorStatus
  )

  // Multi-tab: Update submission using the ID-scoped ChRIS response.
  def updateSubmissionFromChrisResponse(
    submissionId: String,
    ua: UserAnswers,
    id: String,
    chrisResp: ChrisSubmissionResponse
  )(implicit req: DataRequest[AnyContent], hc: HeaderCarrier): Future[Unit] =
    updateSubmissionWithId(
      submissionId,
      ua,
      id,
      chrisResp.hmrcMarkGenerated,
      chrisResp.status,
      chrisResp.acceptedTime,
      None,
      chrisResp.error,
      chrisResp.govTalkErrorStatus
    )

  def updateSubmission(
    submissionId: String,
    ua: UserAnswers,
    hmrcMarkGenerated: String,
    status: String,
    acceptedTime: Option[String],
    irMarkReceived: Option[String] = None,
    error: Option[JsValue] = None,
    govTalkErrorStatus: Option[GovTalkErrorStatus] = None
  )(implicit req: CisIdDataRequest[AnyContent], hc: HeaderCarrier): Future[Unit] = {
    val ukNow = ukLocalDateTimeNow

    val acceptedTimestamp = Option.when(status == "SUBMITTED" || status == "SUBMITTED_NO_RECEIPT") {
      acceptedTime
        .flatMap(chrisAcceptedTimeToUkLocal)
        .getOrElse(ukNow)
        .toString
    }

    val instanceId = ua.get(CisIdPage).getOrElse(throw new RuntimeException("CIS ID missing"))
    val ym         = selectedYearMonth(ua)
    val email      = ua.get(EnterYourEmailAddressPage)
    val returnType = ua.get(ReturnTypePage).getOrElse(throw new RuntimeException("Return type missing"))

    val errorCode    = error.flatMap(js => (js \ "number").asOpt[String])
    val errorType    = error.flatMap(js => (js \ "type").asOpt[String])
    val errorMessage = error.flatMap(js => (js \ "text").asOpt[String])

    val resolvedGovTalkStatus = govTalkErrorStatus.getOrElse(
      GovTalkErrorClassifier.classify(status, errorCode, errorMessage.getOrElse(""))
    )

    val update = UpdateSubmissionRequest(
      instanceId = instanceId,
      hmrcMarkGenerated = Some(hmrcMarkGenerated),
      hmrcMarkGgis = irMarkReceived,
      emailRecipient = email,
      agentId = req.agentReference,
      taxYear = ym.getYear,
      taxMonth = ym.getMonthValue,
      submittableStatus = status,
      amendment = returnType.amendmentFlag,
      acceptedTime = acceptedTimestamp,
      submissionRequestDate = Some(ukNow),
      govtalkErrorCode = errorCode,
      govtalkErrorType = errorType,
      govtalkErrorMessage = errorMessage,
      govTalkResponse = Some(resolvedGovTalkStatus)
    )

    cisConnector.updateSubmission(submissionId, update)
  }

  // Multi-tab: Update the backend submission using ID-scoped answers.
  def updateSubmissionWithId(
    submissionId: String,
    ua: UserAnswers,
    id: String,
    hmrcMarkGenerated: String,
    status: String,
    acceptedTime: Option[String],
    irMarkReceived: Option[String] = None,
    error: Option[JsValue] = None,
    govTalkErrorStatus: Option[GovTalkErrorStatus] = None
  )(implicit req: DataRequest[AnyContent], hc: HeaderCarrier): Future[Unit] = {
    val ukNow = ukLocalDateTimeNow

    val acceptedTimestamp = Option.when(status == "SUBMITTED" || status == "SUBMITTED_NO_RECEIPT") {
      acceptedTime
        .flatMap(chrisAcceptedTimeToUkLocal)
        .getOrElse(ukNow)
        .toString
    }

    val instanceId = ua.get(CisIdPage(id)).getOrElse(throw new RuntimeException("CisId missing"))
    val ym         = selectedYearMonth(ua, id)
    val email      = ua.get(EnterYourEmailAddressPage(id))
    val returnType = ua.get(ReturnTypePage(id)).getOrElse(throw new RuntimeException("Return type missing"))

    val errorCode    = error.flatMap(js => (js \ "number").asOpt[String])
    val errorType    = error.flatMap(js => (js \ "type").asOpt[String])
    val errorMessage = error.flatMap(js => (js \ "text").asOpt[String])

    val resolvedGovTalkStatus = govTalkErrorStatus.getOrElse(
      GovTalkErrorClassifier.classify(status, errorCode, errorMessage.getOrElse(""))
    )

    val update = UpdateSubmissionRequest(
      instanceId = instanceId,
      hmrcMarkGenerated = Some(hmrcMarkGenerated),
      hmrcMarkGgis = irMarkReceived,
      emailRecipient = email,
      agentId = req.agentReference,
      taxYear = ym.getYear,
      taxMonth = ym.getMonthValue,
      submittableStatus = status,
      amendment = returnType.amendmentFlag,
      acceptedTime = acceptedTimestamp,
      submissionRequestDate = Some(ukNow),
      govtalkErrorCode = errorCode,
      govtalkErrorType = errorType,
      govtalkErrorMessage = errorMessage,
      govTalkResponse = Some(resolvedGovTalkStatus)
    )

    cisConnector.updateSubmission(submissionId, update)
  }

  // Polling

  def getPollInterval(userAnswers: UserAnswers): Int =
    userAnswers.get(PollIntervalPage).getOrElse(appConfig.submissionPollDefaultIntervalSeconds)

  // Multi-tab: Get the polling interval for the ID-scoped journey.
  def getPollInterval(userAnswers: UserAnswers, id: String): Int =
    userAnswers.get(PollIntervalPage(id)).getOrElse(appConfig.submissionPollDefaultIntervalSeconds)

  // Fixed cadence at which the "sending" page re-checks; the ChRIS poll interval
  // (getPollInterval) only throttles whether an actual poll is sent on each check.
  def getRefreshInterval: Int =
    appConfig.submissionPollDefaultIntervalSeconds

  def checkAndUpdateSubmissionStatusIfAllowed(
    userAnswers: UserAnswers
  )(using HeaderCarrier, CisIdDataRequest[AnyContent]): Future[PollDecision] =
    userAnswers.get(LastMessageDatePage) match {
      case Some(receivedAt) =>
        val pollInterval      = getPollInterval(userAnswers)
        val nextPollAllowedAt = receivedAt.plusSeconds(pollInterval)

        if (Instant.now().isAfter(nextPollAllowedAt)) {
          checkAndUpdateSubmissionStatus(userAnswers).map(PollDecision.Polled.apply)
        } else {
          Future.successful(PollDecision.Skip)
        }

      case None =>
        // No poll has happened yet: the first poll must wait one poll interval after the
        // submission to ChRIS, rather than polling immediately.
        val pollInterval = getPollInterval(userAnswers)
        userAnswers.get(SubmissionDetailsPage) match {
          case Some(details) if LocalDateTime.now().isBefore(details.submittedAt.plusSeconds(pollInterval)) =>
            Future.successful(PollDecision.Skip)
          case _                                                                                            =>
            checkAndUpdateSubmissionStatus(userAnswers).map(PollDecision.Polled.apply)
        }
    }

  // Multi-tab: Check whether polling is allowed for the ID-scoped journey.
  def checkAndUpdateSubmissionStatusIfAllowed(
    userAnswers: UserAnswers,
    id: String
  )(using HeaderCarrier, DataRequest[AnyContent]): Future[PollDecision] =
    userAnswers.get(LastMessageDatePage(id)) match {
      case Some(receivedAt) =>
        val pollInterval      = getPollInterval(userAnswers, id)
        val nextPollAllowedAt = receivedAt.plusSeconds(pollInterval)

        if (Instant.now().isAfter(nextPollAllowedAt)) {
          checkAndUpdateSubmissionStatus(userAnswers, id).map(PollDecision.Polled.apply)
        } else {
          Future.successful(PollDecision.Skip)
        }

      case None =>
        // No poll has happened yet: the first poll must wait one poll interval after the
        // submission to ChRIS, rather than polling immediately.
        val pollInterval = getPollInterval(userAnswers, id)

        userAnswers.get(SubmissionDetailsPage(id)) match {
          case Some(details) if LocalDateTime.now().isBefore(details.submittedAt.plusSeconds(pollInterval)) =>
            Future.successful(PollDecision.Skip)
          case _                                                                                            =>
            checkAndUpdateSubmissionStatus(userAnswers, id).map(PollDecision.Polled.apply)
        }
    }

  def checkAndUpdateSubmissionStatus(
    userAnswers: UserAnswers
  )(using HeaderCarrier, CisIdDataRequest[AnyContent]): Future[String] = {
    val timeout = appConfig.submissionPollTimeoutSeconds

    userAnswers.get(SubmissionDetailsPage) match {
      case None                    => Future.failed(new IllegalStateException("No submission details present"))
      case Some(submissionDetails)
          if userAnswers
            .get(SubmissionStatusTimedOutPage(submissionDetails.id))
            .contains(true) =>
        Future.successful("TIMED_OUT")
      case Some(submissionDetails) =>
        val timeoutDateTime = submissionDetails.submittedAt.plusSeconds(timeout)
        val now             = LocalDateTime.now()

        if (now.isAfter(timeoutDateTime)) {
          for {
            ua1 <- Future.fromTry(userAnswers.set(SubmissionStatusTimedOutPage(submissionDetails.id), true))
            _   <- sessionRepository.set(ua1)
          } yield "TIMED_OUT"
        } else {
          for {
            pollUrl      <- userAnswers.get(PollUrlPage).toFuture
            submissionId <- userAnswers.get(SubmissionDetailsPage).map(_.id).toFuture
            result       <- cisConnector.getSubmissionStatus(pollUrl, submissionId)
            _            <- updateSubmission(
                              submissionDetails.id,
                              userAnswers,
                              submissionDetails.irMark,
                              result.status,
                              result.acceptedTime,
                              result.irMarkReceived,
                              result.error,
                              result.govTalkErrorStatus
                            )
            newStatus     = result.status
            timedOut      =
              LocalDateTime.now().isAfter(timeoutDateTime) && (newStatus == "ACCEPTED" || newStatus == "PENDING")
            finalStatus   = if (timedOut) "TIMED_OUT" else newStatus
            newDetails    = submissionDetails.copy(
                              status = newStatus,
                              hmrcMarkGgis = result.irMarkReceived
                            )
            ua1          <- Future.fromTry(userAnswers.set(SubmissionDetailsPage, newDetails))
            ua2          <- Future.fromTry(ua1.set(SubmissionStatusTimedOutPage(submissionDetails.id), timedOut))
            ua3          <- result.pollUrl.map(url => ua2.set(PollUrlPage, url)).getOrElse(Try(ua2)).toFuture
            ua4          <- result.intervalSeconds.map(i => ua3.set(PollIntervalPage, i)).getOrElse(Try(ua3)).toFuture
            ua5          <- result.lastMessageDate match {
                              case Some(ts) => Future.fromTry(ua4.set(LastMessageDatePage, Instant.parse(ts)))
                              case None     => Future.successful(ua4)
                            }
            _            <- sessionRepository.set(ua5)
          } yield finalStatus
        }
    }
  }

  // Multi-tab: Poll ChRIS and update the ID-scoped submission state.
  def checkAndUpdateSubmissionStatus(
    userAnswers: UserAnswers,
    id: String
  )(using HeaderCarrier, DataRequest[AnyContent]): Future[String] = {
    val timeout = appConfig.submissionPollTimeoutSeconds

    userAnswers.get(SubmissionDetailsPage(id)) match {
      case None                    => Future.failed(new IllegalStateException("No submission details present"))
      case Some(submissionDetails)
          if userAnswers
            .get(SubmissionStatusTimedOutPage(submissionDetails.id))
            .contains(true) =>
        Future.successful("TIMED_OUT")
      case Some(submissionDetails) =>
        val timeoutDateTime = submissionDetails.submittedAt.plusSeconds(timeout)
        val now             = LocalDateTime.now()

        if (now.isAfter(timeoutDateTime)) {
          for {
            ua1 <- Future.fromTry(userAnswers.set(SubmissionStatusTimedOutPage(submissionDetails.id), true))
            _   <- sessionRepository.set(ua1)
          } yield "TIMED_OUT"
        } else {
          for {
            pollUrl      <- userAnswers.get(PollUrlPage(id)).toFuture
            submissionId <- userAnswers.get(SubmissionDetailsPage(id)).map(_.id).toFuture
            result       <- cisConnector.getSubmissionStatus(pollUrl, submissionId)
            _            <- updateSubmissionWithId(
                              submissionDetails.id,
                              userAnswers,
                              id,
                              submissionDetails.irMark,
                              result.status,
                              result.acceptedTime,
                              result.irMarkReceived,
                              result.error,
                              result.govTalkErrorStatus
                            )
            newStatus     = result.status
            timedOut      =
              LocalDateTime.now().isAfter(timeoutDateTime) && (newStatus == "ACCEPTED" || newStatus == "PENDING")
            finalStatus   = if (timedOut) "TIMED_OUT" else newStatus
            newDetails    = submissionDetails.copy(status = newStatus, hmrcMarkGgis = result.irMarkReceived)
            ua1          <- Future.fromTry(userAnswers.set(SubmissionDetailsPage(id), newDetails))
            ua2          <- Future.fromTry(ua1.set(SubmissionStatusTimedOutPage(submissionDetails.id), timedOut))
            ua3          <- result.pollUrl.map(url => ua2.set(PollUrlPage(id), url)).getOrElse(Try(ua2)).toFuture
            ua4          <- result.intervalSeconds.map(i => ua3.set(PollIntervalPage(id), i)).getOrElse(Try(ua3)).toFuture
            ua5          <- result.lastMessageDate match {
                              case Some(ts) => Future.fromTry(ua4.set(LastMessageDatePage(id), Instant.parse(ts)))
                              case None     => Future.successful(ua4)
                            }
            _            <- sessionRepository.set(ua5)
          } yield finalStatus
        }
    }
  }

// Email

  def sendSuccessEmail(userAnswers: UserAnswers, langCode: String)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    val submissionId = userAnswers
      .get(SubmissionDetailsPage)
      .map(_.id)
      .getOrElse(throw new IllegalStateException("Submission details missing"))

    val alreadySent = userAnswers
      .get(SuccessEmailSentPage(submissionId))
      .getOrElse(false)

    if (alreadySent) {
      Future.successful(userAnswers)
    } else {
      val yearMonth = userAnswers
        .get(DateConfirmPaymentsPage)
        .map(YearMonth.from)
        .getOrElse(throw new IllegalStateException("Month/Year not selected"))

      val emailOpt = userAnswers.get(EnterYourEmailAddressPage).map(_.trim).filter(_.nonEmpty)

      emailOpt match {
        case None =>
          for {
            latestUa  <- sessionRepository.get(userAnswers.id).map(_.getOrElse(userAnswers))
            updatedUa <- Future.fromTry(latestUa.set(SuccessEmailSentPage(submissionId), true))
            _         <- sessionRepository.set(updatedUa)
          } yield updatedUa

        case Some(email) =>
          val locale: Locale = Lang.get(langCode).map(_.locale).getOrElse(Locale.UK)
          val request        = SendSuccessEmailRequest(
            email = email,
            month = yearMonth.format(DateTimeFormats.monthFormatter(locale)),
            year = yearMonth.getYear.toString,
            language = langCode
          )

          for {
            _         <- cisConnector.sendSuccessfulEmail(submissionId, request)
            latestUa  <- sessionRepository.get(userAnswers.id).map(_.getOrElse(userAnswers))
            updatedUa <- Future.fromTry(latestUa.set(SuccessEmailSentPage(submissionId), true))
            _         <- sessionRepository.set(updatedUa)
          } yield updatedUa
      }
    }
  }

  // Multi-tab: Send the success email for the ID-scoped journey.
  def sendSuccessEmail(
    userAnswers: UserAnswers,
    id: String,
    langCode: String
  )(implicit hc: HeaderCarrier): Future[UserAnswers] = {

    val submissionId = userAnswers
      .get(SubmissionDetailsPage(id))
      .map(_.id)
      .getOrElse(throw new IllegalStateException("Submission details missing"))

    val alreadySent = userAnswers
      .get(SuccessEmailSentPage(submissionId))
      .getOrElse(false)

    if (alreadySent) {
      Future.successful(userAnswers)
    } else {
      val yearMonth = userAnswers
        .get(DateConfirmPaymentsPage(id))
        .map(YearMonth.from)
        .getOrElse(throw new IllegalStateException("Month/Year not selected"))

      val emailOpt = userAnswers.get(EnterYourEmailAddressPage(id)).map(_.trim).filter(_.nonEmpty)

      emailOpt match {
        case None =>
          for {
            latestUa  <- sessionRepository.get(userAnswers.id).map(_.getOrElse(userAnswers))
            updatedUa <- Future.fromTry(latestUa.set(SuccessEmailSentPage(submissionId), true))
            _         <- sessionRepository.set(updatedUa)
          } yield updatedUa

        case Some(email) =>
          val locale: Locale = Lang.get(langCode).map(_.locale).getOrElse(Locale.UK)
          val request        = SendSuccessEmailRequest(
            email = email,
            month = yearMonth.format(DateTimeFormats.monthFormatter(locale)),
            year = yearMonth.getYear.toString,
            language = langCode
          )

          for {
            _         <- cisConnector.sendSuccessfulEmail(submissionId, request)
            latestUa  <- sessionRepository.get(userAnswers.id).map(_.getOrElse(userAnswers))
            updatedUa <- Future.fromTry(latestUa.set(SuccessEmailSentPage(submissionId), true))
            _         <- sessionRepository.set(updatedUa)
          } yield updatedUa
      }
    }
  }

  def isAlreadySubmitted(userAnswers: UserAnswers): Boolean = {
    val ym = selectedYearMonth(userAnswers)

    userAnswers
      .get(SubmissionCreatedPage(ym.toString))
      .getOrElse(false)
  }

  // Multi-tab: Check whether a submission has been created for the ID-scoped journey.
  def isAlreadySubmitted(userAnswers: UserAnswers, id: String): Boolean = {
    val ym = selectedYearMonth(userAnswers, id)

    userAnswers
      .get(SubmissionCreatedForFlowPage(id, ym.toString))
      .getOrElse(false)
  }

  // UserAnswer helpers

  private def buildCreateRequest(ua: UserAnswers): Future[CreateSubmissionRequest] = {
    val instanceId = ua.get(CisIdPage).toRight(new RuntimeException("CIS ID missing")).toTry.get
    val ym         = selectedYearMonth(ua)
    val email      = ua.get(EnterYourEmailAddressPage)
    val returnType = ua.get(ReturnTypePage).getOrElse(throw new RuntimeException("Return type missing"))

    Future.successful(
      CreateSubmissionRequest(
        instanceId = instanceId,
        taxYear = ym.getYear,
        taxMonth = ym.getMonthValue,
        amendment = returnType.amendmentFlag,
        emailRecipient = email
      )
    )
  }

  // Multi-tab: Build the create-submission request using ID-scoped answers.
  private def buildCreateRequest(ua: UserAnswers, id: String): Future[CreateSubmissionRequest] = {
    val requiredAnswers = for {
      instanceId <- ua.get(CisIdPage(id))
      taxDate    <- ua.get(DateConfirmPaymentsPage(id))
      returnType <- ua.get(ReturnTypePage(id))
    } yield (instanceId, taxDate, returnType)

    requiredAnswers match {
      case Some((instanceId, taxDate, returnType)) =>
        Future.successful(
          CreateSubmissionRequest(
            instanceId = instanceId,
            taxYear = taxDate.getYear,
            taxMonth = taxDate.getMonthValue,
            amendment = returnType.amendmentFlag,
            emailRecipient = ua.get(EnterYourEmailAddressPage(id))
          )
        )

      case None =>
        Future.failed(new RuntimeException("Monthly return details missing"))
    }
  }

  private def selectedYearMonth(ua: UserAnswers): YearMonth =
    ua.get(DateConfirmPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(
        throw new RuntimeException("Date of return missing for monthly return")
      )

  // Multi-tab: Get the selected return period for the ID-scoped journey.
  private def selectedYearMonth(ua: UserAnswers, id: String): YearMonth =
    ua.get(DateConfirmPaymentsPage(id))
      .map(YearMonth.from)
      .getOrElse(
        throw new RuntimeException("Date of return missing for monthly return")
      )

  private def ukLocalDateTimeNow: LocalDateTime =
    ZonedDateTime.now(clock).withZoneSameInstant(ukZone).toLocalDateTime

  private def chrisAcceptedTimeToUkLocal(acceptedTime: String): Option[LocalDateTime] =
    parseChrisUtcTimestamp(acceptedTime).map(_.atZone(ukZone).toLocalDateTime)

  private def parseChrisUtcTimestamp(timestamp: String): Option[Instant] =
    Try(Instant.parse(timestamp))
      .orElse(Try(LocalDateTime.parse(timestamp).atZone(ZoneOffset.UTC).toInstant))
      .toOption

  private def writeToFeMongo(
    ua: UserAnswers,
    submissionId: String,
    response: ChrisSubmissionResponse,
    amendment: Option[String]
  ): Future[Boolean] = {
    val updatedUa: Try[UserAnswers] = for {
      ua1 <- ua.set(
               SubmissionDetailsPage,
               SubmissionDetails(
                 id = submissionId,
                 status = response.status,
                 irMark = response.hmrcMarkGenerated,
                 submittedAt = LocalDateTime.now(),
                 amendment = amendment,
                 hmrcMarkGgis = None
               )
             )
      ua2 <- response.responseEndPoint match {
               case Some(endpoint) =>
                 for {
                   u1 <- ua1.set(PollUrlPage, endpoint.url)
                   u2 <- u1.set(PollIntervalPage, endpoint.pollIntervalSeconds)
                 } yield u2
               case None           =>
                 Success(ua1)
             }
      ua3 <- response.correlationId.toTry.flatMap(c => ua2.set(CorrelationIdPage, c))
    } yield ua3

    updatedUa.fold(
      { err =>
        logger.error(
          s"[writeToFeMongo] Failed to update UserAnswers: ${err.getMessage} correlationId=${response.correlationId
              .getOrElse("unknown")}",
          err
        )
        Future.failed(err)
      },
      sessionRepository.set
    )
  }

  // Multi-tab: Persist ChRIS submission details for the ID-scoped journey.
  private def writeToFeMongo(
    ua: UserAnswers,
    id: String,
    submissionId: String,
    response: ChrisSubmissionResponse,
    amendment: Option[String]
  ): Future[Boolean] = {

    val updatedUa: Try[UserAnswers] = for {
      ua1 <- ua.set(
               SubmissionDetailsPage(id),
               SubmissionDetails(
                 id = submissionId,
                 status = response.status,
                 irMark = response.hmrcMarkGenerated,
                 submittedAt = LocalDateTime.now(),
                 amendment = amendment,
                 hmrcMarkGgis = None
               )
             )

      ua2 <- response.responseEndPoint match {
               case Some(endpoint) =>
                 for {
                   u1 <- ua1.set(PollUrlPage(id), endpoint.url)
                   u2 <- u1.set(PollIntervalPage(id), endpoint.pollIntervalSeconds)
                 } yield u2

               case None =>
                 Success(ua1)
             }

      ua3 <- response.correlationId.toTry.flatMap { correlationId =>
               ua2.set(CorrelationIdPage(id), correlationId)
             }

    } yield ua3

    updatedUa.fold(
      { err =>
        logger.error(
          "[SubmissionService][writeToFeMongo] Failed to update scoped UserAnswers",
          err
        )
        Future.failed(err)
      },
      sessionRepository.set
    )
  }

}
