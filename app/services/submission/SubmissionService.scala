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
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.{ReturnType, UserAnswers}
import models.monthlyreturns.CisTaxpayer
import models.requests.{DataRequest, SendSuccessEmailRequest}
import models.submission.*
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.*
import pages.submission.*
import play.api.Logging
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{AnyContent, Request}
import play.api.i18n.Lang
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeFormats
import utils.TypeUtils.*

import java.time.format.DateTimeFormatter
import java.time.{Instant, YearMonth}
import java.util.{TimeZone, Locale}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class SubmissionService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  appConfig: FrontendAppConfig,
  sessionRepository: SessionRepository,
  chrisRequestBuilder: ChrisSubmissionRequestBuilder
)(implicit ec: ExecutionContext)
    extends Logging {

  private val dateFormatter =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
      .withZone(TimeZone.getTimeZone("GMT").toZoneId)

  // Orchestration

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
      taxpayer <- taxpayerFut
      csr      <- chrisRequestBuilder.build(ua, taxpayer, isAgent)(hc)
      response <- cisConnector.submitToChris(submissionId, csr)
      _        <- writeToFeMongo(ua, submissionId, response)
    } yield response

  def updateSubmissionFromChrisResponse(
    submissionId: String,
    ua: UserAnswers,
    chrisResp: ChrisSubmissionResponse
  )(implicit req: DataRequest[AnyContent], hc: HeaderCarrier): Future[Unit] = updateSubmission(
    submissionId,
    ua,
    chrisResp.hmrcMarkGenerated,
    chrisResp.status,
    chrisResp.gatewayTimestamp,
    chrisResp.error
  )

  def updateSubmission(
    submissionId: String,
    ua: UserAnswers,
    hmrcMarkGenerated: String,
    status: String,
    gatewayTimestamp: Option[String],
    error: Option[JsValue] = None
  )(implicit req: DataRequest[AnyContent], hc: HeaderCarrier): Future[Unit] = {

    val instanceId = ua.get(CisIdPage).getOrElse(throw new RuntimeException("CIS ID missing"))
    val ym         = selectedYearMonth(ua)
    val email      = ua.get(EnterYourEmailAddressPage)

    val update = UpdateSubmissionRequest(
      instanceId = instanceId,
      hmrcMarkGenerated = Some(hmrcMarkGenerated),
      emailRecipient = email,
      agentId = req.agentReference,
      taxYear = ym.getYear,
      taxMonth = ym.getMonthValue,
      submittableStatus = status,
      acceptedTime = gatewayTimestamp,
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
  )(using HeaderCarrier): Future[PollDecision] =
    userAnswers.get(LastMessageDatePage) match {
      case Some(receivedAt) =>
        val pollInterval      = getPollInterval(userAnswers)
        val nextPollAllowedAt = receivedAt.plusSeconds(pollInterval)
        val now               = Instant.now() // TODO - val added to support logs for testing, to be deleted after verification

        if (Instant.now().isAfter(nextPollAllowedAt)) {
          // TODO - logs used for testing, to be deleted after verification
          logger.info(
            s"[checkAndUpdateSubmissionStatusIfAllowed] POLL ALLOWED " +
              s"lastMessageRecieved=$receivedAt," +
              s"pollIntervalSeconds=$pollInterval, " +
              s"nextPollAllowed=$nextPollAllowedAt, " +
              s"now=$now"
          )
          checkAndUpdateSubmissionStatus(userAnswers).map(PollDecision.Polled.apply)
        } else {
          // TODO - logs used for testing, to be deleted after verification
          logger.info(
            s"[checkAndUpdateSubmissionStatusIfAllowed] POLL SKIPPED " +
              s"lastMessageRecieved=$receivedAt," +
              s"pollIntervalSeconds=$pollInterval, " +
              s"nextPollAllowed=$nextPollAllowedAt, " +
              s"now=$now"
          )
          Future.successful(PollDecision.Skip)
        }

      case None =>
        logger.warn("[checkAndUpdateSubmissionStatusIfAllowed] Missing lastMessageDate, allowing poll by default")
        checkAndUpdateSubmissionStatus(userAnswers).map(PollDecision.Polled.apply)
    }

  def checkAndUpdateSubmissionStatus(
    userAnswers: UserAnswers
  )(using HeaderCarrier, DataRequest[AnyContent]): Future[String] = {
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
        val now             = Instant.now()

        if (now.isAfter(timeoutDateTime)) {
          for {
            ua1 <- Future.fromTry(userAnswers.set(SubmissionStatusTimedOutPage(submissionDetails.id), true))
            _   <- sessionRepository.set(ua1)
          } yield "TIMED_OUT"
        } else {
          for {
            cisId             <- userAnswers.get(CisIdPage).toFuture
          pollUrl           <- userAnswers.get(PollUrlPage).toFuture
          submissionDetails <- userAnswers.get(SubmissionDetailsPage).toFuture
          submissionId     <- userAnswers.get(SubmissionDetailsPage).map(_.id).toFuture
          result            <- cisConnector.getSubmissionStatus(pollUrl, submissionId)
          _                 <- updateSubmission(
                                 cisId,
                                 userAnswers,
                                 submissionDetails.irMark,
                                 result.status,
                                 Some(dateFormatter.format(submissionDetails.submittedAt)),
                                 None
                               )
          newStatus          = result.status
          timedOut           = Instant.now().isAfter(timeoutDateTime) && (newStatus == "ACCEPTED" || newStatus == "PENDING")finalStatus    = if (timedOut) "TIMED_OUT" else newStatus
          newDetails         = submissionDetails.copy(status = newStatus)
          ua1               <- Future.fromTry(userAnswers.set(SubmissionDetailsPage, newDetails))
          ua2               <- Future.fromTry(ua1.set(SubmissionStatusTimedOutPage(submissionDetails.id), timedOut))
          ua3               <- result.pollUrl.map(url => ua2.set(PollUrlPage, url)).getOrElse(Try(ua2)).toFuture
          ua4               <- result.intervalSeconds.map(i => ua3.set(PollIntervalPage, i)).getOrElse(Try(ua3)).toFuture
          ua5           <- result.lastMessageDate match {
                               case Some(ts) => Future.fromTry(ua4.set(LastMessageDatePage, Instant.parse(ts)))
                               case None     => Future.successful(ua4)
                             }_                 <- sessionRepository.set(ua5)
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

    val returnType = userAnswers
      .get(ReturnTypePage)
      .getOrElse(throw new IllegalStateException("Return type missing"))

    if (alreadySent) {
      Future.successful(userAnswers)
    } else {
      val yearMonth = userAnswers
        .get(DateConfirmPaymentsPage)
        .map(YearMonth.from)
        .getOrElse(throw new IllegalStateException("Month/Year not selected"))

      val emailOpt = returnType match {
        case MonthlyNilReturn | MonthlyStandardReturn =>
          userAnswers.get(EnterYourEmailAddressPage).map(_.trim).filter(_.nonEmpty)
      }

      emailOpt match {
        case None =>
          val updated = userAnswers.set(SuccessEmailSentPage(submissionId), true)
          Future
            .fromTry(updated)
            .flatMap(updatedUa => sessionRepository.set(updatedUa).map(_ => updatedUa))

        case Some(email) =>
          val locale: Locale = Lang.get(langCode).map(_.locale).getOrElse(Locale.UK)
          val request        = SendSuccessEmailRequest(
            email = email,
            month = yearMonth.format(DateTimeFormats.monthFormatter(locale)),
            year = yearMonth.getYear.toString
          )

          val updatedUaFuture = Future.fromTry(userAnswers.set(SuccessEmailSentPage(submissionId), true))

          for {
            _         <- cisConnector.sendSuccessfulEmail(submissionId, request)
            updatedUa <- updatedUaFuture
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

    Future.successful(
      CreateSubmissionRequest(
        instanceId = instanceId,
        taxYear = ym.getYear,
        taxMonth = ym.getMonthValue,
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

  private def writeToFeMongo(
    ua: UserAnswers,
    submissionId: String,
    response: ChrisSubmissionResponse
  ): Future[Boolean] = {
    val updatedUa: Try[UserAnswers] = for {
      ua1 <- ua.set(
               SubmissionDetailsPage,
               SubmissionDetails(
                 id = submissionId,
                 status = response.status,
                 irMark = response.hmrcMarkGenerated,
                 submittedAt = response.gatewayTimestamp
                   .flatMap(t => Try(Instant.parse(t)).toOption)
                   .getOrElse(Instant.now)
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
