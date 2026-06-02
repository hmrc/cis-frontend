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
import models.monthlyreturns.CisTaxpayer
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
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeFormats
import utils.TypeUtils.*

import java.time.{Clock, Instant, LocalDateTime, YearMonth, ZoneId, ZoneOffset, ZonedDateTime}
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
    extends Logging {

  private val ukZone: ZoneId = ZoneId.of("Europe/London")

  def create(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[(CreateSubmissionResponse, UserAnswers)] =
    for {
      req            <- buildCreateRequest(ua)
      response       <- cisConnector.createSubmission(req)
      updatedAnswers <- Future.fromTry(ua.set(SubmissionCreatedPage(selectedYearMonth(ua).toString), true))
      _              <- sessionRepository.set(updatedAnswers)
    } yield (response, updatedAnswers)

  def submitToChrisAndPersist(
    submissionId: String,
    ua: UserAnswers,
    isAgent: Boolean
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

    for {
      taxpayer  <- taxpayerFut
      csr       <- chrisRequestBuilder.build(ua, taxpayer, isAgent)(hc)
      response  <- cisConnector.submitToChris(submissionId, csr)
      amendment <- fetchAmendmentFlag(ua)
      _         <- writeToFeMongo(ua, submissionId, response, amendment)
    } yield response

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
    chrisResp.error
  )

  def updateSubmission(
    submissionId: String,
    ua: UserAnswers,
    hmrcMarkGenerated: String,
    status: String,
    acceptedTime: Option[String],
    irMarkReceived: Option[String] = None,
    error: Option[JsValue] = None
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

    val update = UpdateSubmissionRequest(
      instanceId = instanceId,
      hmrcMarkGenerated = Some(hmrcMarkGenerated),
      hmrcMarkGgis = irMarkReceived,
      emailRecipient = email,
      agentId = req.agentReference,
      taxYear = ym.getYear,
      taxMonth = ym.getMonthValue,
      submittableStatus = status,
      acceptedTime = acceptedTimestamp,
      submissionRequestDate = Some(ukNow),
      govtalkErrorCode = error.flatMap(js => (js \ "number").asOpt[String]),
      govtalkErrorType = error.flatMap(js => (js \ "type").asOpt[String]),
      govtalkErrorMessage = error.flatMap(js => (js \ "text").asOpt[String])
    )

    cisConnector.updateSubmission(submissionId, update)
  }

  // Polling

  def getPollInterval(userAnswers: UserAnswers): Int =
    userAnswers.get(PollIntervalPage).getOrElse(appConfig.submissionPollDefaultIntervalSeconds)

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
        logger.warn("[checkAndUpdateSubmissionStatusIfAllowed] Missing lastMessageDate, allowing poll by default")
        checkAndUpdateSubmissionStatus(userAnswers).map(PollDecision.Polled.apply)
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
                              result.error
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

  private def fetchAmendmentFlag(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val instanceId  = ua.get(CisIdPage).getOrElse(throw new RuntimeException("CIS ID missing"))
    val ym          = selectedYearMonth(ua)
    val isAmendment = ua.get(AmendmentDetailsPage).isDefined
    cisConnector
      .retrieveMonthlyReturnForEditDetails(
        GetMonthlyReturnForEditRequest(instanceId, ym.getMonthValue, ym.getYear, isAmendment)
      )
      .map(_.monthlyReturn.headOption.flatMap(_.amendment))
      .recover { case ex =>
        logger.warn("[fetchAmendmentFlag] Failed to retrieve amendment flag, defaulting to None", ex)
        None
      }
  }

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
