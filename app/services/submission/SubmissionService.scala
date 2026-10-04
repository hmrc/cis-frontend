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
import models.UserAnswers
import models.monthlyreturns.CisTaxpayer
import models.requests.*
import models.submission.*
import pages.amend.AmendmentDetailsPage
import pages.monthlyreturns.*
import pages.submission.*
import play.api.Logging
import play.api.i18n.Lang
import play.api.libs.json.JsValue
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import utils.DateTimeFormats
import utils.TypeUtils.*

import java.time.*
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class SubmissionService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  appConfig: FrontendAppConfig,
  sessionRepository: SessionRepository,
  chrisRequestBuilder: ChrisSubmissionRequestBuilder,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends Logging
    with FrontendHeaderCarrierProvider {

  private val ukZone: ZoneId = ZoneId.of("Europe/London")

  def getOrCreateSubmissionForChris(cisId: String, ua: UserAnswers)(using
    HeaderCarrier
  ): Future[(String, UserAnswers, Boolean)] =
    ua.get(ResubmissionIdPage) match {
      case Some(resubmissionId) =>
        Future.successful((resubmissionId.toString, ua, true))
      case None                 =>
        create(cisId, ua).map { case (created, updatedUa) =>
          (created.submissionId, updatedUa, false)
        }
    }

  def create(cisId: String, ua: UserAnswers)(using HeaderCarrier): Future[(CreateSubmissionResponse, UserAnswers)] =
    for {
      req            <- buildCreateRequest(cisId, ua)
      response       <- cisConnector.createSubmission(req)
      updatedAnswers <- Future.fromTry(ua.set(SubmissionCreatedPage(selectedYearMonth(ua).toString), true))
      _              <- sessionRepository.set(updatedAnswers)
    } yield (response, updatedAnswers)

  def submitToChrisAndPersist(
    submissionId: String,
    cisTaxpayer: CisTaxpayer,
    ua: UserAnswers,
    isAgent: Boolean,
    isResubmission: Boolean
  )(implicit hc: HeaderCarrier): Future[ChrisSubmissionResponse] =

    val requiredAnswers = for {
      taxDate    <- ua.get(DateConfirmPaymentsPage)
      isAmendment = ua.get(AmendmentDetailsPage).isDefined
    } yield (taxDate.getMonthValue, taxDate.getYear, isAmendment)

    for {
      (taxMonth, taxYear, isAmendment) <- requiredAnswers.fold(
                                            Future.failed[(Int, Int, Boolean)](
                                              new RuntimeException("Month and year of return missing")
                                            )
                                          )(Future.successful)
      monthlyReturn                    <- cisConnector.retrieveMonthlyReturnForEditDetails(
                                            GetMonthlyReturnForEditRequest(cisTaxpayer.uniqueId, taxMonth, taxYear, isAmendment)
                                          )
      csr                               = chrisRequestBuilder.build(ua, cisTaxpayer, isAgent, monthlyReturn, isResubmission)
      response                         <- cisConnector.submitToChris(submissionId, csr)
      amendment                         = monthlyReturn.monthlyReturn.headOption.flatMap(_.amendment)
      _                                <- writeToFeMongo(ua, submissionId, response, amendment)
    } yield response

  def updateSubmissionFromChrisResponse(submissionId: String, ua: UserAnswers, chrisResp: ChrisSubmissionResponse)(using
    SchemeRequest[?]
  ): Future[Unit] = updateSubmission(
    submissionId,
    ua,
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
  )(using req: SchemeRequest[?]): Future[Unit] = {
    val ukNow = ukLocalDateTimeNow

    val acceptedTimestamp = Option.when(status == "SUBMITTED" || status == "SUBMITTED_NO_RECEIPT") {
      acceptedTime
        .flatMap(chrisAcceptedTimeToUkLocal)
        .getOrElse(ukNow)
        .toString
    }

    val ym         = selectedYearMonth(ua)
    val email      = ua.get(EnterYourEmailAddressPage)
    val returnType = ua.get(ReturnTypePage).getOrElse(throw new RuntimeException("Return type missing"))

    val resolvedGovTalkStatus = govTalkErrorStatus.getOrElse(GovTalkErrorClassifier.classify(status, error))

    val update = UpdateSubmissionRequest(
      instanceId = req.cisId,
      hmrcMarkGenerated = Some(hmrcMarkGenerated),
      hmrcMarkGgis = irMarkReceived,
      emailRecipient = email,
      agentId = req.identifier.agentReference,
      taxYear = ym.getYear,
      taxMonth = ym.getMonthValue,
      submittableStatus = status,
      amendment = returnType.amendmentFlag,
      acceptedTime = acceptedTimestamp,
      submissionRequestDate = Some(ukNow),
      govtalkErrorCode = error.flatMap(js => (js \ "number").asOpt[String]),
      govtalkErrorType = error.flatMap(js => (js \ "type").asOpt[String]),
      govtalkErrorMessage = error.flatMap(js => (js \ "text").asOpt[String]),
      govTalkResponse = Some(resolvedGovTalkStatus)
    )

    cisConnector.updateSubmission(submissionId, update)
  }

  // Polling

  def getPollInterval(userAnswers: UserAnswers): Int =
    userAnswers.get(PollIntervalPage).getOrElse(appConfig.submissionPollDefaultIntervalSeconds)

  def checkAndUpdateSubmissionStatusIfAllowed(userAnswers: UserAnswers)(using SchemeRequest[?]): Future[PollDecision] =
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
        logger.warn("[checkAndUpdateSubmissionStatusIfAllowed] Missing lastMessageDate, allowing poll by default")
        checkAndUpdateSubmissionStatus(userAnswers).map(PollDecision.Polled.apply)
    }

  def checkAndUpdateSubmissionStatus(userAnswers: UserAnswers)(using SchemeRequest[?]): Future[String] = {
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
            year = yearMonth.getYear.toString
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

  // UserAnswer helpers

  private def buildCreateRequest(cisId: String, ua: UserAnswers): Future[CreateSubmissionRequest] = {
    val ym         = selectedYearMonth(ua)
    val email      = ua.get(EnterYourEmailAddressPage)
    val returnType = ua.get(ReturnTypePage).getOrElse(throw new RuntimeException("Return type missing"))

    Future.successful(
      CreateSubmissionRequest(
        instanceId = cisId,
        taxYear = ym.getYear,
        taxMonth = ym.getMonthValue,
        amendment = returnType.amendmentFlag,
        emailRecipient = email
      )
    )
  }

  private def selectedYearMonth(ua: UserAnswers): YearMonth =
    ua.get(DateConfirmPaymentsPage)
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
                 submittedAt = response.gatewayTimestamp
                   .flatMap(t => Try(LocalDateTime.parse(t)).toOption)
                   .getOrElse(LocalDateTime.now),
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
        logger.error(s"[writeToFeMongo] Failed to update UserAnswers: ${err.getMessage}", err)
        Future.failed(err)
      },
      sessionRepository.set
    )
  }
}
