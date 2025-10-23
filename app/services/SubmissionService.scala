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
import models.submission.*
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, InactivityRequestPage}
import pages.submission.*
import play.api.Logging
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

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

  def createAndTrack(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[CreateAndTrackSubmissionResponse] =
    for {
      req      <- buildCreateAndTrackRequest(ua)
      response <- cisConnector.createAndTrackSubmission(req)
    } yield response

  def submitToChrisAndPersist(submissionId: String, ua: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[ChrisSubmissionResponse] =
    for {
      taxpayer <- cisConnector.getCisTaxpayer()
      csr       = buildChrisSubmissionRequest(ua, taxpayer)
      response <- cisConnector.submitToChris(submissionId, csr)
      _        <- writeToFeMongo(ua, submissionId, response)
    } yield response

  def checkAndUpdateSubmissionStatus(
    userAnswers: UserAnswers
  ): Future[String] = {
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
          newStatus <- cisConnector.getSubmissionStatus(submissionDetails.submittedAt)
          timedOut   = Instant.now().isAfter(timeoutDateTime) && (newStatus == "ACCEPTED" || newStatus == "PENDING")
          newDetails = submissionDetails.copy(status = newStatus)
          ua1       <- Future.fromTry(userAnswers.set(SubmissionDetailsPage, newDetails))
          ua2       <- Future.fromTry(ua1.set(SubmissionStatusTimedOutPage(submissionDetails.id), timedOut))
          _         <- sessionRepository.set(ua2)
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

  private def buildCreateAndTrackRequest(ua: UserAnswers): Future[CreateAndTrackSubmissionRequest] = {
    val instanceId = ua.get(CisIdPage).toRight(new RuntimeException("CIS ID missing")).toTry.get
    val ym         = ua
      .get(DateConfirmNilPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month/Year not selected"))
    val email      = ua.get(ConfirmEmailAddressPage)

    Future.successful(
      CreateAndTrackSubmissionRequest(
        instanceId = instanceId,
        taxYear = ym.getYear,
        taxMonth = ym.getMonthValue,
        emailRecipient = email
      )
    )
  }

  private def buildChrisSubmissionRequest(ua: UserAnswers, taxpayer: CisTaxpayer): ChrisSubmissionRequest = {
    val utr = taxpayer.utr
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer UTR missing"))
    val ao  = taxpayer.aoReference
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("CIS taxpayer AOref missing"))

    val inactivity = ua.get(InactivityRequestPage).contains(InactivityRequest.Option1)
    val ym         = ua
      .get(DateConfirmNilPaymentsPage)
      .map(YearMonth.from)
      .getOrElse(throw new RuntimeException("Month/Year not selected"))

    ChrisSubmissionRequest.from(
      utr = utr,
      aoReference = ao,
      informationCorrect = true,
      inactivity = inactivity,
      monthYear = ym
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
    } yield ua2

    updatedUa.fold(
      { err =>
        logger.error(s"[writeToFeMongo] Failed to update UserAnswers: ${err.getMessage}", err)
        Future.failed(err)
      },
      sessionRepository.set
    )
  }

}
