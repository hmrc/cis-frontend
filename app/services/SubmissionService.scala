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

import connectors.ConstructionIndustrySchemeConnector
import models.UserAnswers
import models.monthlyreturns.{CisTaxpayer, InactivityRequest}
import models.submission.*
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, InactivityRequestPage}
import pages.submission.*
import play.api.Logging
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.YearMonth
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class SubmissionService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def create(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[CreateSubmissionResponse] =
    for {
      req      <- buildCreateRequest(ua)
      response <- cisConnector.createSubmission(req)
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
    val email      = ua.get(ConfirmEmailAddressPage).get

    ChrisSubmissionRequest.from(
      utr = utr,
      aoReference = ao,
      informationCorrect = true,
      inactivity = inactivity,
      monthYear = ym,
      email = email
    )
  }

  private def writeToFeMongo(
    ua: UserAnswers,
    submissionId: String,
    response: ChrisSubmissionResponse
  ): Future[Boolean] = {
    val updatedUa: Try[UserAnswers] = for {
      ua1 <- ua.set(SubmissionIdPage, submissionId)
      ua2 <- ua1.set(SubmissionStatusPage, response.status)
      ua3 <- ua2.set(IrMarkPage, response.hmrcMarkGenerated)
      ua4 <- response.responseEndPoint match {
               case Some(endpoint) =>
                 for {
                   u1 <- ua3.set(PollUrlPage, endpoint.url)
                   u2 <- u1.set(PollIntervalPage, endpoint.pollIntervalSeconds)
                 } yield u2
               case None           =>
                 Success(ua3)
             }
    } yield ua4

    updatedUa.fold(
      { err =>
        logger.error(s"[writeToFeMongo] Failed to update UserAnswers: ${err.getMessage}", err)
        Future.failed(err)
      },
      sessionRepository.set
    )
  }

}
