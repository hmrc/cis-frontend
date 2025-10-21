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
import connectors.ConstructionIndustrySchemeConnector
import models.UserAnswers
import models.monthlyreturns.{CisTaxpayer, InactivityRequest}
import models.submission.{ChrisSubmissionRequest, ChrisSubmissionResponse, CreateAndTrackSubmissionRequest, CreateAndTrackSubmissionResponse, UpdateSubmissionRequest}
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.ArgumentCaptor
import org.scalatest.TryValues
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, InactivityRequestPage}
import play.api.libs.json.{JsObject, Json}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, YearMonth}
import scala.concurrent.{ExecutionContext, Future}

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

  "createAndTrack" - {
    "build request from UserAnswers and return BE response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

      val expectedReq = CreateAndTrackSubmissionRequest(
        instanceId = "123",
        taxYear = 2025,
        taxMonth = 10,
        emailRecipient = Some("test@test.com")
      )

      val beResp = CreateAndTrackSubmissionResponse("sub-123")
      when(connector.createAndTrackSubmission(any[CreateAndTrackSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))

      val out = service.createAndTrack(uaBase).futureValue
      out mustBe beResp

      val cap: ArgumentCaptor[CreateAndTrackSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[CreateAndTrackSubmissionRequest])
      verify(connector).createAndTrackSubmission(cap.capture())(any[HeaderCarrier]())
      cap.getValue mustBe expectedReq
    }

    "fail when CIS ID is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

      val ua = emptyUserAnswers.set(DateConfirmNilPaymentsPage, LocalDate.of(2025, 10, 5)).success.value

      val ex = intercept[RuntimeException] {
        service.createAndTrack(ua).futureValue
      }
      ex.getMessage must include("CIS ID missing")
      verifyNoInteractions(connector)
    }

    "fail when Month/Year is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

      val ua = emptyUserAnswers.set(CisIdPage, "123").success.value

      val ex = intercept[RuntimeException] {
        service.createAndTrack(ua).futureValue
      }
      ex.getMessage must include("Month/Year not selected")
      verifyNoInteractions(connector)
    }
  }

  "submitToChrisAndPersist" - {
    "fetch taxpayer, build ChrisSubmissionRequest and return BE response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

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
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

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
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

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
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

      when(connector.getCisTaxpayer()(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val uaNoYM = emptyUserAnswers.set(CisIdPage, "123").success.value

      val ex = intercept[RuntimeException] {
        service.submitToChrisAndPersist("sub-123", uaNoYM).futureValue
      }
      ex.getMessage must include("Month/Year not selected")
      verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
    }
  }

  "updateSubmission" - {
    "translate chris response to UpdateSubmissionRequest and call connector" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

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
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

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
      val sessionRepository                              = mock(classOf[SessionRepository])
      val service                                        = new SubmissionService(connector, sessionRepository)

      val ua        = emptyUserAnswers.set(CisIdPage, "123").success.value
      val chrisResp = mkChrisResp()

      val ex = intercept[RuntimeException] {
        service.updateSubmission("sub-123", ua, chrisResp).futureValue
      }
      ex.getMessage must include("Month/Year not selected")
      verifyNoInteractions(connector)
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
