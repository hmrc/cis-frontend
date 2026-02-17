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

package services

import config.FrontendAppConfig
import connectors.ConstructionIndustrySchemeConnector
import models.UserAnswers
import models.monthlyreturns.{CisTaxpayer, InactivityRequest}
import models.requests.SendSuccessEmailRequest
import models.submission.*
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, InactivityRequestPage, SuccessEmailSentPage}
import pages.submission.*
import play.api.Logging
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.TypeUtils.*

import java.time.{Instant, YearMonth}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class SubmissionService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  appConfig: FrontendAppConfig,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def create(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[CreateSubmissionResponse] =
    for {
      req      <- buildCreateRequest(ua)
      response <- cisConnector.createSubmission(req)
    } yield response

  def submitToChrisAndPersist(
    submissionId: String,
    ua: UserAnswers,
    isAgent: Boolean
  )(implicit
    hc: HeaderCarrier
  ): Future[ChrisSubmissionResponse] =
    for {
      taxpayer <- if (isAgent) cisConnector.getAgentClientTaxpayer("123", "AB001") else cisConnector.getCisTaxpayer()
      csr       = buildChrisSubmissionRequest(ua, taxpayer, isAgent)
      response <- cisConnector.submitToChris(submissionId, csr)
      _        <- writeToFeMongo(ua, submissionId, response)
    } yield response

  def getPollInterval(userAnswers: UserAnswers): Int =
    userAnswers.get(PollIntervalPage).getOrElse(appConfig.submissionPollDefaultIntervalSeconds)

  def checkAndUpdateSubmissionStatus(
    userAnswers: UserAnswers
  )(using HeaderCarrier): Future[String] = {
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

        for {
          pollUrl       <- userAnswers.get(PollUrlPage).toFuture
          correlationId <- userAnswers.get(CorrelationIdPage).toFuture
          result        <- cisConnector.getSubmissionStatus(pollUrl, correlationId)
          newStatus      = result.status
          timedOut       = Instant.now().isAfter(timeoutDateTime) && (newStatus == "ACCEPTED" || newStatus == "PENDING")
          newDetails     = submissionDetails.copy(status = newStatus)
          ua1           <- Future.fromTry(userAnswers.set(SubmissionDetailsPage, newDetails))
          ua2           <- Future.fromTry(ua1.set(SubmissionStatusTimedOutPage(submissionDetails.id), timedOut))
          ua3           <- result.pollUrl.map(url => ua2.set(PollUrlPage, url)).getOrElse(Try(ua2)).toFuture
          ua4           <- result.intervalSeconds.map(i => ua3.set(PollIntervalPage, i)).getOrElse(Try(ua3)).toFuture
          _             <- sessionRepository.set(ua4)
        } yield newStatus
    }
  }

  def updateSubmission(submissionId: String, ua: UserAnswers, chrisResp: ChrisSubmissionResponse)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {

    val instanceId = ua
      .get(CisIdPage)
      .getOrElse(throw new RuntimeException("CIS ID missing"))
    val ym         = ua
      .get(DateConfirmNilPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month/Year not selected"))
    val email      = ua.get(ConfirmEmailAddressPage)

    val update = UpdateSubmissionRequest(
      instanceId = instanceId,
      taxYear = ym.getYear,
      taxMonth = ym.getMonthValue,
      hmrcMarkGenerated = Some(chrisResp.hmrcMarkGenerated),
      submittableStatus = chrisResp.status,
      acceptedTime = chrisResp.gatewayTimestamp,
      emailRecipient = email,
      govtalkErrorCode = chrisResp.error.flatMap(js => (js \ "number").asOpt[String]),
      govtalkErrorType = chrisResp.error.flatMap(js => (js \ "type").asOpt[String]),
      govtalkErrorMessage = chrisResp.error.flatMap(js => (js \ "text").asOpt[String])
    )

    cisConnector.updateSubmission(submissionId, update)
  }

  def sendSuccessEmail(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
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
      val email = userAnswers
        .get(ConfirmEmailAddressPage)
        .getOrElse(throw new IllegalStateException("Email address missing"))

      val yearMonth = userAnswers
        .get(DateConfirmNilPaymentsPage)
        .map(YearMonth.from)
        .getOrElse(throw new IllegalStateException("Month/Year not selected"))

      val request = SendSuccessEmailRequest(
        email = email,
        month = yearMonth.getMonthValue.toString,
        year = yearMonth.getYear.toString
      )

      cisConnector
        .sendSuccessfulEmail(submissionId, request)
        .flatMap(_ =>
          Future
            .fromTry(userAnswers.set(SuccessEmailSentPage(submissionId), true))
            .flatMap { updatedUa =>
              sessionRepository.set(updatedUa).map(_ => updatedUa)
            }
        )
    }
  }

  private def buildCreateRequest(ua: UserAnswers): Future[CreateSubmissionRequest] = {
    val instanceId = ua.get(CisIdPage).toRight(new RuntimeException("CIS ID missing")).toTry.get
    val ym         = ua
      .get(DateConfirmNilPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month/Year not selected"))
    val email      = ua.get(ConfirmEmailAddressPage)

    Future.successful(
      CreateSubmissionRequest(
        instanceId = instanceId,
        taxYear = ym.getYear,
        taxMonth = ym.getMonthValue,
        emailRecipient = email
      )
    )
  }

  private def buildChrisSubmissionRequest(
    ua: UserAnswers,
    taxpayer: CisTaxpayer,
    isAgent: Boolean
  ): ChrisSubmissionRequest = {
    val utr = taxpayer.utr
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer UTR missing"))

    val aoDistrict = taxpayer.aoDistrict
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoDistrict missing"))

    val aoPayType = taxpayer.aoPayType
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoPayType missing"))

    val aoCheckCode = taxpayer.aoCheckCode
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoCheckCode missing"))

    val aoReference = taxpayer.aoReference
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer aoReference missing"))

    // AO reference = aoDistrict + aoPayType + aoCheckCode + aoReference
    val accountsOfficeRef: String =
      List(aoDistrict, aoPayType, aoCheckCode, aoReference).flatten.mkString

    val inactivity = ua.get(InactivityRequestPage).contains(InactivityRequest.Option1)
    val ym         = ua
      .get(DateConfirmNilPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month/Year not selected"))
    val email      = ua.get(ConfirmEmailAddressPage).get

    ChrisSubmissionRequest.from(
      utr = utr,
      aoReference = accountsOfficeRef,
      informationCorrect = true,
      inactivity = inactivity,
      monthYear = ym,
      email = email,
      isAgent = isAgent,
      clientTaxOfficeNumber = taxpayer.taxOfficeNumber,
      clientTaxOfficeRef = taxpayer.taxOfficeRef
    )
  }

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
