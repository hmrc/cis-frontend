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

import base.SpecBase
import config.FrontendAppConfig
import connectors.ConstructionIndustrySchemeConnector
import models.UserAnswers
import models.monthlyreturns.{CisTaxpayer, InactivityRequest}
import models.submission.*
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.ArgumentCaptor
import org.scalatest.TryValues
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, InactivityRequestPage}
import pages.submission.{CorrelationIdPage, PollIntervalPage, PollUrlPage, SubmissionDetailsPage}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDate, YearMonth}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class SubmissionServiceSpec extends SpecBase with TryValues {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  private val taxpayer =
    CisTaxpayer(
      uniqueId = "123",
      taxOfficeNumber = "123",
      taxOfficeRef = "AB456",
      employerName1 = Some("TEST LTD"),
      utr = Some("1234567890"),
      aoReference = Some("123/AB456"),
      aoDistrict = None,
      aoPayType = None,
      aoCheckCode = None,
      validBusinessAddr = None,
      correlation = None,
      ggAgentId = None,
      employerName2 = None,
      agentOwnRef = None,
      schemeName = None,
      enrolledSig = None
    )

  "create" - {
    "build request from UserAnswers and return BE response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val expectedReq = CreateSubmissionRequest(
        instanceId = "123",
        taxYear = 2025,
        taxMonth = 10,
        emailRecipient = Some("test@test.com")
      )

      val beResp = CreateSubmissionResponse("sub-123")
      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))

      val out = service.create(uaBase).futureValue
      out mustBe beResp

      val cap: ArgumentCaptor[CreateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[CreateSubmissionRequest])
      verify(connector).createSubmission(cap.capture())(any[HeaderCarrier]())
      cap.getValue mustBe expectedReq
    }

    "fail when CIS ID is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua = emptyUserAnswers.set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 10, 5)).success.value

      val ex = intercept[RuntimeException] {
        service.create(ua).futureValue
      }
      ex.getMessage must include("CIS ID missing")
      verifyNoInteractions(connector)
    }

    "fail when Month/Year is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua = emptyUserAnswers.set(CisIdPage, "123").success.value

      val ex = intercept[RuntimeException] {
        service.create(ua).futureValue
      }
      ex.getMessage must include("Month/Year not selected")
      verifyNoInteractions(connector)
    }
  }

  "submitToChrisAndPersist" - {
    "fetch taxpayer, build ChrisSubmissionRequest and return BE response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val expectedCsr = ChrisSubmissionRequest.from(
        utr = "1234567890",
        aoReference = "123/AB456",
        informationCorrect = true,
        inactivity = true,
        monthYear = YearMonth.parse("2025-10")
      )

      val beResp = mkChrisResp()

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))

      val out = service.submitToChrisAndPersist("sub-123", uaWithInactivityYes).futureValue
      out mustBe beResp

      val cap: ArgumentCaptor[ChrisSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[ChrisSubmissionRequest])
      verify(connector).submitToChris(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])
      cap.getValue mustBe expectedCsr
    }

    "fail when taxpayer UTR is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val badTaxpayer = taxpayer.copy(utr = None)
      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(badTaxpayer))

      val ex = intercept[RuntimeException] {
        service.submitToChrisAndPersist("sub-123", uaBase).futureValue
      }
      ex.getMessage must include("CIS taxpayer UTR missing")
      verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }

    "fail when taxpayer AO reference is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val badTaxpayer = taxpayer.copy(aoReference = None)
      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(badTaxpayer))

      val ex = intercept[RuntimeException] {
        service.submitToChrisAndPersist("sub-123", uaBase).futureValue
      }
      ex.getMessage must include("CIS taxpayer AOref missing")
      verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }

    "fail when Month/Year not selected" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val uaNoYM = emptyUserAnswers.set(CisIdPage, "123").success.value

      val ex = intercept[RuntimeException] {
        service.submitToChrisAndPersist("sub-123", uaNoYM).futureValue
      }
      ex.getMessage must include("Month/Year not selected")
      verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }

    "persists poll endpoint details when response.endpoint is present" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val beRespWithEndpoint = ChrisSubmissionResponse(
        submissionId = "sub-123",
        status = "SUBMITTED",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        correlationId = Some("CID-123"),
        responseEndPoint = Some(
          ResponseEndPointDto(
            url = "https://poll.example.test/cis/sub-123",
            pollIntervalSeconds = 15
          )
        ),
        gatewayTimestamp = Some("2025-01-01T00:00:00Z"),
        error = None
      )

      val uaCaptor: ArgumentCaptor[UserAnswers] =
        ArgumentCaptor.forClass(classOf[UserAnswers])

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beRespWithEndpoint))

      val out   = service.submitToChrisAndPersist("sub-123", uaWithInactivityYes).futureValue
      out mustBe beRespWithEndpoint
      verify(sessionRepository).set(uaCaptor.capture())
      val saved = uaCaptor.getValue
      saved.get(PollUrlPage).value mustBe "https://poll.example.test/cis/sub-123"
      saved.get(PollIntervalPage).value mustBe 15
    }

    "fails fast and does not persist if updating UserAnswers fails" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val beResp = mkChrisResp()

      when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))

      val ua    = uaWithInactivityYes
      val uaSpy = spy(ua)

      doReturn(Failure(new RuntimeException("boom: cannot write submission id")))
        .when(uaSpy)
        .set(eqTo(SubmissionDetailsPage), any)(any())

      val ex = intercept[RuntimeException] {
        service.submitToChrisAndPersist("sub-123", uaSpy).futureValue
      }
      ex.getMessage must include("boom: cannot write submission id")
      verify(sessionRepository, never()).set(any[UserAnswers])
    }
  }

  "updateSubmission" - {
    "translate chris response to UpdateSubmissionRequest and call connector" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val ua = uaBase

      val chrisResp = mkChrisResp(
        status = "DEPARTMENTAL_ERROR",
        ts = "2025-02-02T10:20:30Z",
        err = Some(Json.obj("number" -> "123", "type" -> "business", "text" -> "oops"))
      )

      service.updateSubmission("sub-123", ua, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      val upd = cap.getValue
      upd.instanceId mustBe "123"
      upd.taxYear mustBe 2025
      upd.taxMonth mustBe 10
      upd.hmrcMarkGenerated mustBe Some("Dj5TVJDyRYCn9zta5EdySeY4fyA=")
      upd.submittableStatus mustBe "DEPARTMENTAL_ERROR"
      upd.acceptedTime mustBe Some("2025-02-02T10:20:30Z")
      upd.emailRecipient mustBe Some("test@test.com")
      upd.govtalkErrorCode mustBe Some("123")
      upd.govtalkErrorType mustBe Some("business")
      upd.govtalkErrorMessage mustBe Some("oops")
    }

    "fail when CIS ID missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua        = emptyUserAnswers.set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 10, 5)).success.value
      val chrisResp = mkChrisResp()

      val ex = intercept[RuntimeException] {
        service.updateSubmission("sub-123", ua, chrisResp).futureValue
      }
      ex.getMessage must include("CIS ID missing")
      verifyNoInteractions(connector)
    }

    "fail when Month/Year not selected" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua        = emptyUserAnswers.set(CisIdPage, "123").success.value
      val chrisResp = mkChrisResp()

      val ex = intercept[RuntimeException] {
        service.updateSubmission("sub-123", ua, chrisResp).futureValue
      }
      ex.getMessage must include("Month/Year not selected")
      verifyNoInteractions(connector)
    }
  }

  "getPollInterval" - {
    "return the poll interval from UserAnswers when present" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "10"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua = uaBase.set(PollIntervalPage, 25).success.value

      val result = service.getPollInterval(ua)

      result mustBe 25
    }

    "return the default poll interval from config when not present in UserAnswers" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "15"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua = uaBase

      val result = service.getPollInterval(ua)

      result mustBe 15
    }
  }

  "checkAndUpdateSubmissionStatus" - {
    "fail when SubmissionDetailsPage is not present" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      val ua = uaBase

      when(connector.getSubmissionStatus(any, any)(any))
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("someUrl"), None)))

      val result = service.checkAndUpdateSubmissionStatus(ua).failed.futureValue

      result mustBe an[IllegalStateException]
      result.getMessage must include("No submission details present")

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "return TIMED_OUT when SubmissionStatusTimedOutPage is already true" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(SubmissionStatusTimedOutPage("sub-123"), true)
        .success
        .value

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "TIMED_OUT"
      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "update status and save to repository when submission is not timed out" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "3600"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = Instant.now().minusSeconds(60)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(CorrelationIdPage, "123")
        .success
        .value
        .set(PollUrlPage, "someUrl")
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("someUrl"), None)))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "SUBMITTED"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "SUBMITTED"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe false
    }

    "update status and save PollUrl and PollInterval when present in poll response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "3600"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = Instant.now().minusSeconds(60)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(CorrelationIdPage, "123")
        .success
        .value
        .set(PollUrlPage, "oldUrl")
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("PENDING", Some("newPollUrl"), Some(30))))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "PENDING"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "PENDING"
      savedUa.get(PollUrlPage).value mustBe "newPollUrl"
      savedUa.get(PollIntervalPage).value mustBe 30
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe false
    }

    "mark as timed out when timeout exceeded and status is still PENDING" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = Instant.now().minusSeconds(120)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(CorrelationIdPage, "123")
        .success
        .value
        .set(PollUrlPage, "someUrl")
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("PENDING", Some("someurl"), None)))

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "PENDING"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "PENDING"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe true
    }

    "mark as timed out when timeout exceeded and status is ACCEPTED" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = Instant.now().minusSeconds(120)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "ACCEPTED",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(CorrelationIdPage, "123")
        .success
        .value
        .set(PollUrlPage, "someUrl")
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("ACCEPTED", Some("someurl"), None)))

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "ACCEPTED"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "ACCEPTED"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe true
    }

    "not mark as timed out when timeout exceeded but status is SUBMITTED" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = Instant.now().minusSeconds(120)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(CorrelationIdPage, "123")
        .success
        .value
        .set(PollUrlPage, "someUrl")
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("someUrl"), None)))

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "SUBMITTED"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "SUBMITTED"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe false
    }

    "not mark as timed out when timeout exceeded but status is FATAL_ERROR" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val service                                        = new SubmissionService(connector, appConfig, sessionRepository)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = Instant.now().minusSeconds(120)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val ua = uaBase
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value
        .set(CorrelationIdPage, "123")
        .success
        .value
        .set(PollUrlPage, "someUrl")
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("FATAL_ERROR", Some("someurl"), None)))

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "FATAL_ERROR"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "FATAL_ERROR"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe false
    }
  }

  private def uaBase: UserAnswers =
    emptyUserAnswers
      .set(CisIdPage, "123")
      .success
      .value
      .set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 10, 5))
      .success
      .value
      .set(ConfirmEmailAddressPage, "test@test.com")
      .success
      .value

  private def uaWithInactivityYes: UserAnswers =
    uaBase.set(InactivityRequestPage, InactivityRequest.Option1).success.value

  private def mkChrisResp(
    status: String = "SUBMITTED",
    irmark: String = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
    corr: String = "CID123",
    ts: String = "2025-01-01T00:00:00Z",
    err: Option[JsObject] = None
  ): ChrisSubmissionResponse =
    ChrisSubmissionResponse(
      submissionId = "sub-123",
      status = status,
      hmrcMarkGenerated = irmark,
      correlationId = Some(corr),
      responseEndPoint = None,
      gatewayTimestamp = Some(ts),
      error = err
    )

}
