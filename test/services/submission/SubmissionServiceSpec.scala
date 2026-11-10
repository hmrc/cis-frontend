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

import base.SpecBase
import config.FrontendAppConfig
import connectors.ConstructionIndustrySchemeConnector
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyNilReturn, MonthlyStandardReturn}
import models.{ReturnType, UserAnswers}
import models.agent.AgentClientData
import models.monthlyreturns.*
import models.requests.*
import models.submission.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.TryValues
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.*
import pages.submission.{CorrelationIdPage, CreatedSubmissionIdForFlowPage, CreatedSubmissionIdPage, LastMessageDatePage, PollIntervalPage, PollUrlPage, ResubmissionIdPage, SubmissionCreatedForFlowPage, SubmissionCreatedPage, SubmissionDetailsPage, SubmissionStatusTimedOutPage}
import play.api.Configuration
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import repositories.SessionRepository
import services.{MonthlyReturnItemPayloadBuilder, MonthlyReturnItemPayloadBuilderImpl}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant, LocalDate, LocalDateTime, YearMonth, ZoneOffset}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class SubmissionServiceSpec extends SpecBase with TryValues {

  implicit val hc: HeaderCarrier     = HeaderCarrier()
  implicit val ec: ExecutionContext  = scala.concurrent.ExecutionContext.global
  given CisIdDataRequest[AnyContent] = CisIdDataRequest(FakeRequest(), userAnswersId, emptyUserAnswers, "123")
  given DataRequest[AnyContent]      = DataRequest(FakeRequest(), userAnswersId, emptyUserAnswers)

  private val utcClock: Clock = Clock.systemUTC()

  private val itemPayloadBuilder: MonthlyReturnItemPayloadBuilder = new MonthlyReturnItemPayloadBuilderImpl()

  private def mkService(
    connector: ConstructionIndustrySchemeConnector,
    sessionRepository: SessionRepository,
    appConfig: FrontendAppConfig,
    chrisRequestBuilder: ChrisSubmissionRequestBuilder,
    clock: Clock = utcClock
  ): SubmissionService =
    new SubmissionService(connector, appConfig, sessionRepository, chrisRequestBuilder, itemPayloadBuilder, clock)

  private val taxpayer =
    CisTaxpayer(
      uniqueId = "123",
      taxOfficeNumber = "123",
      taxOfficeRef = "AB456",
      employerName1 = Some("TEST LTD"),
      utr = Some("1234567890"),
      aoReference = Some("12345678"),
      aoDistrict = Some("123"),
      aoPayType = Some("P"),
      aoCheckCode = Some("A"),
      validBusinessAddr = None,
      correlation = None,
      ggAgentId = None,
      employerName2 = None,
      agentOwnRef = None,
      schemeName = None,
      enrolledSig = None
    )

  "getOrCreateSubmissionForChris" - {

    "return existing resubmission id and not create a new submission when ResubmissionIdPage is present" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = uaBase
        .set(ResubmissionIdPage, 13180L)
        .success
        .value

      val result = service.getOrCreateSubmissionForChris(ua).futureValue

      result mustBe ("13180", ua, true)

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
      verifyNoInteractions(chrisRequestBuilder)
    }

    "create a new submission when ResubmissionIdPage is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val beResp = CreateSubmissionResponse("sub-123")

      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.getOrCreateSubmissionForChris(uaBase).futureValue

      result._1 mustBe "sub-123"
      result._3 mustBe false

      verify(connector).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
      verify(sessionRepository).set(any[UserAnswers])
      verifyNoInteractions(chrisRequestBuilder)
    }

    "refresh every selected subcontractor's item before creating the submission" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val service                                        = mkService(connector, sessionRepository, appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(CreateSubmissionResponse("sub-123")))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val result = service.getOrCreateSubmissionForChris(uaWithSelectedSubcontractors(7)).futureValue

      result._1 mustBe "sub-123"

      val captor = ArgumentCaptor.forClass(classOf[UpdateMonthlyReturnItemRequest])
      verify(connector, times(7)).updateMonthlyReturnItem(captor.capture())(any[HeaderCarrier])
      import scala.jdk.CollectionConverters.*
      captor.getAllValues.asScala.map(_.subcontractorId).toSet mustBe (1L to 7L).toSet

      val order = org.mockito.Mockito.inOrder(connector)
      order.verify(connector, times(7)).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier])
      order.verify(connector).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
    }

    "not create the submission when an item cannot be refreshed" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val service                                        = mkService(connector, sessionRepository, appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.getOrCreateSubmissionForChris(uaWithSelectedSubcontractors(2)).failed.futureValue mustBe an[
        IllegalStateException
      ]

      verify(connector, never()).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
      verifyNoInteractions(sessionRepository)
    }

    "reuse the stored submission id without refreshing items or creating again when the submission already exists" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val service                                        = mkService(connector, sessionRepository, appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      val ua = uaWithSelectedSubcontractors(2)
        .set(SubmissionCreatedPage("2026-03"), true)
        .success
        .value
        .set(CreatedSubmissionIdPage("2026-03"), "sub-123")
        .success
        .value

      service.getOrCreateSubmissionForChris(ua).futureValue mustBe ("sub-123", ua, false)

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "refresh and create when the created flag is set but no submission id is stored" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val service                                        = mkService(connector, sessionRepository, appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(CreateSubmissionResponse("sub-456")))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val ua = uaBase.set(SubmissionCreatedPage("2025-10"), true).success.value

      service.getOrCreateSubmissionForChris(ua).futureValue._1 mustBe "sub-456"

      verify(connector).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
    }

    "not refresh any item on a resubmission" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val service                                        = mkService(connector, sessionRepository, appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      val ua = uaWithSelectedSubcontractors(2).set(ResubmissionIdPage, 13180L).success.value

      service.getOrCreateSubmissionForChris(ua).futureValue mustBe ("13180", ua, true)

      verifyNoInteractions(connector)
    }
  }

  "refreshMonthlyReturnItems" - {

    "not call the connector when no subcontractors are selected" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val service                                        =
        mkService(connector, mock(classOf[SessionRepository]), appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      service.refreshMonthlyReturnItems(uaBase).futureValue mustBe (())

      verifyNoInteractions(connector)
    }

    "refresh the items one at a time, never in parallel" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val service                                        =
        mkService(connector, mock(classOf[SessionRepository]), appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      val inFlight    = new java.util.concurrent.atomic.AtomicInteger(0)
      val maxInFlight = new java.util.concurrent.atomic.AtomicInteger(0)

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenAnswer { _ =>
          val now = inFlight.incrementAndGet()
          maxInFlight.updateAndGet(m => math.max(m, now))
          Future {
            Thread.sleep(25)
            inFlight.decrementAndGet()
            ()
          }
        }

      service.refreshMonthlyReturnItems(uaWithSelectedSubcontractors(6)).futureValue mustBe (())

      verify(connector, times(6)).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier])
      maxInFlight.get() mustBe 1
    }

    "retry a failed item update once and succeed when the retry works" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val service                                        =
        mkService(connector, mock(classOf[SessionRepository]), appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("transient")))
        .thenReturn(Future.unit)

      service.refreshMonthlyReturnItems(uaWithSelectedSubcontractors(1)).futureValue mustBe (())

      verify(connector, times(2)).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier])
    }

    "fail, without refreshing later items, when an item update still fails after the retry" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val service                                        =
        mkService(connector, mock(classOf[SessionRepository]), appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result = service.refreshMonthlyReturnItems(uaWithSelectedSubcontractors(6)).failed.futureValue

      result mustBe an[IllegalStateException]
      result.getMessage must include("boom")
      verify(connector, times(2)).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier])
    }

    "fail, without calling the connector for it, when an item payload cannot be built" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val service                                        =
        mkService(connector, mock(classOf[SessionRepository]), appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val incomplete = SelectedSubcontractor(id = 9L, name = "Incomplete", None, None, None)
      val ua         = uaWithSelectedSubcontractors(1).set(SelectedSubcontractorPage(2), incomplete).success.value

      val result = service.refreshMonthlyReturnItems(ua).failed.futureValue

      result mustBe an[IllegalStateException]
      result.getMessage must include("index: 2")
      verify(connector, times(1)).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier])
    }
  }

  "create" - {
    "build request from UserAnswers and return BE response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val expectedReq = CreateSubmissionRequest(
        instanceId = "123",
        taxYear = 2025,
        taxMonth = 10,
        amendment = "N",
        emailRecipient = Some("test@test.com")
      )

      val beResp = CreateSubmissionResponse("sub-123")

      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val out = service.create(uaBase).futureValue
      out._1 mustBe beResp

      val cap: ArgumentCaptor[CreateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[CreateSubmissionRequest])
      verify(connector).createSubmission(cap.capture())(any[HeaderCarrier]())
      cap.getValue mustBe expectedReq
      verify(sessionRepository).set(any[UserAnswers])
    }

    "store the created submission id and the created flag in the session" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val service                                        = mkService(connector, sessionRepository, appConfig, mock(classOf[ChrisSubmissionRequestBuilder]))

      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(CreateSubmissionResponse("sub-123")))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val (_, updated) = service.create(uaBase).futureValue

      updated.get(SubmissionCreatedPage("2025-10")) mustBe Some(true)
      updated.get(CreatedSubmissionIdPage("2025-10")) mustBe Some("sub-123")
      verify(sessionRepository).set(updated)
    }

    "fail when CIS ID is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = emptyUserAnswers.set(DateConfirmPaymentsPage, LocalDate.of(2025, 10, 5)).success.value

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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = emptyUserAnswers
        .set(CisIdPage, "123")
        .success
        .value
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value

      val ex = intercept[RuntimeException] {
        service.create(ua).futureValue
      }
      ex.getMessage must include("Date of return missing for monthly return")
      verifyNoInteractions(connector)
    }

    "build request using DateConfirmPaymentsPage for monthly standard return" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = emptyUserAnswers
        .set(CisIdPage, "123")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 11, 10))
        .success
        .value
        .set(EnterYourEmailAddressPage, "test@test.com")
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value

      val beResp = CreateSubmissionResponse("sub-123")
      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(beResp))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      service.create(ua).futureValue

      val cap: ArgumentCaptor[CreateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[CreateSubmissionRequest])
      verify(connector).createSubmission(cap.capture())(any[HeaderCarrier]())
      cap.getValue.taxYear mustBe 2025
      cap.getValue.taxMonth mustBe 11

      verify(sessionRepository).set(any[UserAnswers])
    }

    "fail when Month/Year is missing for monthly standard return" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = emptyUserAnswers
        .set(CisIdPage, "123")
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value

      val ex = intercept[RuntimeException] {
        service.create(ua).futureValue
      }

      ex.getMessage mustBe "Date of return missing for monthly return"
      verifyNoInteractions(connector)
    }
  }

  "submitToChrisAndPersist" - {

    "contractor" - {

      "fetch taxpayer, build ChrisSubmissionRequest and return BE response" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val builtCsr = mock(classOf[ChrisSubmissionRequest])
        when(
          chrisRequestBuilder.build(
            any[UserAnswers],
            any[CisTaxpayer],
            eqTo(false),
            any[GetAllMonthlyReturnDetailsResponse],
            eqTo(false)
          )
        ).thenReturn(builtCsr)

        val beResp = mkChrisResp()

        when(sessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(beResp))

        stubRetrieveMonthlyReturnForEditDetails(connector)

        val out = service.submitToChrisAndPersist("sub-123", uaWithInactivityYes, false, false).futureValue
        out mustBe beResp

        val cap: ArgumentCaptor[ChrisSubmissionRequest] =
          ArgumentCaptor.forClass(classOf[ChrisSubmissionRequest])
        verify(connector).submitToChris(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])
        cap.getValue mustBe builtCsr
      }

      "fail when request builder fails" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val monthlyReturnResponse = GetAllMonthlyReturnDetailsResponse(
          scheme = Seq.empty,
          monthlyReturn = Seq.empty,
          subcontractors = Seq.empty,
          monthlyReturnItems = Seq.empty,
          submission = Seq.empty
        )

        when(connector.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(monthlyReturnResponse))

        when(
          chrisRequestBuilder
            .build(
              any[UserAnswers],
              any[CisTaxpayer],
              eqTo(false),
              any[GetAllMonthlyReturnDetailsResponse],
              eqTo(false)
            )
        ).thenThrow(new RuntimeException("boom-builder"))

        val ex = intercept[RuntimeException] {
          service.submitToChrisAndPersist("sub-123", uaBase, false, false).futureValue
        }
        ex.getMessage must include("boom-builder")

        verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
        verify(sessionRepository, never()).set(any[UserAnswers])
      }

      "fail when user answers missing for monthlyReturnForEdit api call" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val ex = intercept[RuntimeException] {
          service.submitToChrisAndPersist("sub-123", emptyUserAnswers, false, false).futureValue
        }
        ex.getMessage must include("Month and year of return missing")

        verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
        verify(sessionRepository, never()).set(any[UserAnswers])
      }

      "persists poll endpoint details but not lastMessageDate when response.endpoint is present" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        stubRetrieveMonthlyReturnForEditDetails(connector)

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
          gatewayTimestamp = Some("2025-01-01T00:00:00"),
          error = None
        )

        val uaCaptor: ArgumentCaptor[UserAnswers] =
          ArgumentCaptor.forClass(classOf[UserAnswers])

        when(sessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(beRespWithEndpoint))

        val builtCsr = mock(classOf[ChrisSubmissionRequest])
        when(
          chrisRequestBuilder
            .build(
              any[UserAnswers],
              any[CisTaxpayer],
              eqTo(false),
              any[GetAllMonthlyReturnDetailsResponse],
              eqTo(false)
            )
        )
          .thenReturn(builtCsr)

        val out = service.submitToChrisAndPersist("sub-123", uaWithInactivityYes, false, false).futureValue
        out mustBe beRespWithEndpoint

        verify(sessionRepository).set(uaCaptor.capture())
        val saved = uaCaptor.getValue
        saved.get(PollUrlPage).value mustBe "https://poll.example.test/cis/sub-123"
        saved.get(PollIntervalPage).value mustBe 15
        saved.get(LastMessageDatePage) mustBe None
      }

      "persist amendment flag as Some(Y) when monthly return has amendment Y" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration("submission-poll-timeout-seconds" -> "60")
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = new SubmissionService(
          connector,
          appConfig,
          sessionRepository,
          chrisRequestBuilder,
          itemPayloadBuilder,
          utcClock
        )

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val builtCsr = mock(classOf[ChrisSubmissionRequest])
        when(
          chrisRequestBuilder
            .build(
              any[UserAnswers],
              any[CisTaxpayer],
              eqTo(false),
              any[GetAllMonthlyReturnDetailsResponse],
              eqTo(false)
            )
        )
          .thenReturn(builtCsr)
        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(mkChrisResp()))

        val amendmentResponse = GetAllMonthlyReturnDetailsResponse(
          scheme = Seq.empty,
          monthlyReturn = Seq(MonthlyReturn(monthlyReturnId = 1, taxYear = 2025, taxMonth = 10, amendment = Some("Y"))),
          subcontractors = Seq.empty,
          monthlyReturnItems = Seq.empty,
          submission = Seq.empty
        )
        when(connector.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(amendmentResponse))

        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(sessionRepository.set(uaCaptor.capture())).thenReturn(Future.successful(true))

        service.submitToChrisAndPersist("sub-123", uaWithInactivityYes, false, false).futureValue

        val saved = uaCaptor.getValue
        saved.get(SubmissionDetailsPage).value.amendment mustBe Some("Y")
      }

      "persist amendment flag as None when monthly return list is empty" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration("submission-poll-timeout-seconds" -> "60")
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = new SubmissionService(
          connector,
          appConfig,
          sessionRepository,
          chrisRequestBuilder,
          itemPayloadBuilder,
          utcClock
        )

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val builtCsr = mock(classOf[ChrisSubmissionRequest])
        when(
          chrisRequestBuilder
            .build(
              any[UserAnswers],
              any[CisTaxpayer],
              eqTo(false),
              any[GetAllMonthlyReturnDetailsResponse],
              eqTo(false)
            )
        )
          .thenReturn(builtCsr)
        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(mkChrisResp()))

        val emptyResponse = GetAllMonthlyReturnDetailsResponse(
          scheme = Seq.empty,
          monthlyReturn = Seq.empty,
          subcontractors = Seq.empty,
          monthlyReturnItems = Seq.empty,
          submission = Seq.empty
        )
        when(connector.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(emptyResponse))

        val uaCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
        when(sessionRepository.set(uaCaptor.capture())).thenReturn(Future.successful(true))

        service.submitToChrisAndPersist("sub-123", uaWithInactivityYes, false, false).futureValue

        val saved = uaCaptor.getValue
        saved.get(SubmissionDetailsPage).value.amendment mustBe None
      }

      "fails fast and does not persist if updating UserAnswers fails" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val builtCsr = mock(classOf[ChrisSubmissionRequest])
        when(
          chrisRequestBuilder
            .build(
              any[UserAnswers],
              any[CisTaxpayer],
              eqTo(false),
              any[GetAllMonthlyReturnDetailsResponse],
              eqTo(false)
            )
        )
          .thenReturn(builtCsr)

        val beResp = mkChrisResp()

        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(beResp))

        stubRetrieveMonthlyReturnForEditDetails(connector)

        val ua    = uaWithInactivityYes
        val uaSpy = spy(ua)

        doReturn(Failure(new RuntimeException("boom: cannot write submission id")))
          .when(uaSpy)
          .set(eqTo(SubmissionDetailsPage), any)(any())

        val ex = intercept[RuntimeException] {
          service.submitToChrisAndPersist("sub-123", uaSpy, false, false).futureValue
        }
        ex.getMessage must include("boom: cannot write submission id")
        verify(sessionRepository, never()).set(any[UserAnswers])
      }

      "pass isResubmission=true to ChrisSubmissionRequestBuilder" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getCisTaxpayer()(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        stubRetrieveMonthlyReturnForEditDetails(connector)

        val builtCsr = mock(classOf[ChrisSubmissionRequest])

        when(
          chrisRequestBuilder.build(
            any[UserAnswers],
            any[CisTaxpayer],
            eqTo(false),
            any[GetAllMonthlyReturnDetailsResponse],
            eqTo(true)
          )
        ).thenReturn(builtCsr)

        val beResp = mkChrisResp()

        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(beResp))

        when(sessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        val result =
          service
            .submitToChrisAndPersist(
              submissionId = "sub-123",
              ua = uaWithInactivityYes,
              isAgent = false,
              isResubmission = true
            )
            .futureValue

        result mustBe beResp

        verify(chrisRequestBuilder).build(
          any[UserAnswers],
          any[CisTaxpayer],
          eqTo(false),
          any[GetAllMonthlyReturnDetailsResponse],
          eqTo(true)
        )

        verify(connector).submitToChris(eqTo("sub-123"), eqTo(builtCsr))(any[HeaderCarrier])
      }

    }

    "agent" - {

      "fetch client taxpayer, build ChrisSubmissionRequest and return BE response" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getAgentClientTaxpayer(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val builtCsr = mock(classOf[ChrisSubmissionRequest])
        when(
          chrisRequestBuilder
            .build(any[UserAnswers], any[CisTaxpayer], eqTo(true), any[GetAllMonthlyReturnDetailsResponse], eqTo(false))
        )
          .thenReturn(builtCsr)

        val beResp = mkChrisResp()

        when(sessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(beResp))

        stubRetrieveMonthlyReturnForEditDetails(connector)

        val uaWithAgentClientData = uaWithInactivityYes.set(AgentClientDataPage, agentDate).success.value
        val out                   = service.submitToChrisAndPersist("sub-123", uaWithAgentClientData, true, false).futureValue
        out mustBe beResp

        val cap: ArgumentCaptor[ChrisSubmissionRequest] =
          ArgumentCaptor.forClass(classOf[ChrisSubmissionRequest])
        verify(connector).submitToChris(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])
        cap.getValue mustBe builtCsr
      }

      "fail when agent client data is missing" in {
        val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
        val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
        val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
          Configuration(
            "submission-poll-timeout-seconds" -> "60"
          )
        )
        val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
        val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

        when(connector.getAgentClientTaxpayer(any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val beResp = mkChrisResp()

        when(sessionRepository.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(beResp))

        val ex = intercept[RuntimeException] {
          service.submitToChrisAndPersist("sub-123", uaBase, true, false).futureValue
        }
        ex.getMessage must include("Agent client data missing")
        verify(connector, never()).submitToChris(any[String], any[ChrisSubmissionRequest])(any[HeaderCarrier])
      }

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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val ua = uaBase

      val chrisResp = mkChrisResp(
        status = "DEPARTMENTAL_ERROR",
        ts = "2025-02-02T10:20:30",
        err = Some(Json.obj("number" -> "123", "type" -> "business", "text" -> "oops")),
        accepted = Some("2025-02-02T10:20:30")
      )

      service.updateSubmissionFromChrisResponse("sub-123", ua, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      val upd = cap.getValue
      upd.instanceId mustBe "123"
      upd.taxYear mustBe 2025
      upd.taxMonth mustBe 10
      upd.hmrcMarkGenerated mustBe Some("Dj5TVJDyRYCn9zta5EdySeY4fyA=")
      upd.submittableStatus mustBe "DEPARTMENTAL_ERROR"
      upd.acceptedTime mustBe None
      upd.emailRecipient mustBe Some("test@test.com")
      upd.govtalkErrorCode mustBe Some("123")
      upd.govtalkErrorType mustBe Some("business")
      upd.govtalkErrorMessage mustBe Some("oops")
      upd.govTalkResponse mustBe Some(GovTalkErrorStatus.DepartmentalError("123", "oops"))
    }

    "prefer a BE-supplied govTalkErrorStatus over local classification" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val chrisResp = mkChrisResp(
        status = "STARTED",
        govTalkErrorStatus = Some(GovTalkErrorStatus.ServerError(503))
      )

      service.updateSubmissionFromChrisResponse("sub-123", uaBase, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      cap.getValue.govTalkResponse mustBe Some(GovTalkErrorStatus.ServerError(503))
    }

    "fail when CIS ID missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua        = emptyUserAnswers.set(DateConfirmPaymentsPage, LocalDate.of(2025, 10, 5)).success.value
      val chrisResp = mkChrisResp()

      val ex = intercept[RuntimeException] {
        service.updateSubmissionFromChrisResponse("sub-123", ua, chrisResp).futureValue
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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua        = emptyUserAnswers
        .set(CisIdPage, "123")
        .success
        .value
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value
      val chrisResp = mkChrisResp()

      val ex = intercept[RuntimeException] {
        service.updateSubmissionFromChrisResponse("sub-123", ua, chrisResp).futureValue
      }
      ex.getMessage must include("Date of return missing for monthly return")
      verifyNoInteractions(connector)
    }

    "convert CHRIS acceptedTime from UTC to UK local time during BST" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val bstClock                                       = Clock.fixed(Instant.parse("2026-05-21T12:45:35Z"), ZoneOffset.UTC)
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder, bstClock)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val chrisResp = mkChrisResp(
        status = "SUBMITTED",
        accepted = Some("2026-05-21T12:45:35")
      )

      service.updateSubmissionFromChrisResponse("sub-123", uaBase, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      val upd = cap.getValue
      upd.acceptedTime.value mustBe "2026-05-21T13:45:35"
      upd.submissionRequestDate.value mustBe LocalDateTime.parse("2026-05-21T13:45:35")
    }

    "convert CHRIS acceptedTime with Z suffix from UTC to UK local time during BST" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val bstClock                                       = Clock.fixed(Instant.parse("2026-05-21T12:45:35Z"), ZoneOffset.UTC)
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder, bstClock)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val chrisResp = mkChrisResp(
        status = "SUBMITTED",
        accepted = Some("2026-05-21T12:45:35Z")
      )

      service.updateSubmissionFromChrisResponse("sub-123", uaBase, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      cap.getValue.acceptedTime.value mustBe "2026-05-21T13:45:35"
    }

    "not adjust CHRIS acceptedTime during GMT" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val gmtClock                                       = Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC)
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder, gmtClock)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val chrisResp = mkChrisResp(
        status = "SUBMITTED",
        accepted = Some("2026-01-15T12:00:00")
      )

      service.updateSubmissionFromChrisResponse("sub-123", uaBase, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      val upd = cap.getValue
      LocalDateTime.parse(upd.acceptedTime.value) mustBe LocalDateTime.parse("2026-01-15T12:00:00")
      upd.submissionRequestDate.value mustBe LocalDateTime.parse("2026-01-15T12:00:00")
    }

    "fall back to UK local time when CHRIS acceptedTime is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val bstClock                                       = Clock.fixed(Instant.parse("2026-05-21T12:45:35Z"), ZoneOffset.UTC)
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder, bstClock)

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val chrisResp = mkChrisResp(status = "SUBMITTED")

      service.updateSubmissionFromChrisResponse("sub-123", uaBase, chrisResp).futureValue

      val cap: ArgumentCaptor[UpdateSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), cap.capture())(any[HeaderCarrier])

      cap.getValue.acceptedTime.value mustBe "2026-05-21T13:45:35"
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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = uaBase

      val result = service.getPollInterval(ua)

      result mustBe 15
    }
  }

  "getRefreshInterval" - {
    "return the fixed default interval from config, ignoring the ChRIS poll interval in UserAnswers" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "10"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      service.getRefreshInterval mustBe 10
    }
  }

  "checkAndUpdateSubmissionStatusIfAllowed" - {

    "poll and return Polled when lastMessageDate is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "10"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submittedAt       = LocalDateTime.now().minusSeconds(60)
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
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("someUrl"), None, None, None, None, None)))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatusIfAllowed(ua).futureValue

      result mustBe PollDecision.Polled("TIMED_OUT")
      verify(sessionRepository).set(any[UserAnswers])
    }

    "skip polling when lastMessageDate is missing and the first poll interval has not elapsed" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "10"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "ACCEPTED",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.now()
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
        .set(PollIntervalPage, 10)
        .success
        .value

      val result = service.checkAndUpdateSubmissionStatusIfAllowed(ua).futureValue

      result mustBe PollDecision.Skip
      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "skip polling when lastMessageDate is present and interval has not elapsed" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "10"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = uaBase
        .set(LastMessageDatePage, Instant.now().minusSeconds(5))
        .success
        .value
        .set(PollIntervalPage, 10)
        .success
        .value

      val result = service.checkAndUpdateSubmissionStatusIfAllowed(ua).futureValue

      result mustBe PollDecision.Skip
      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "poll and return Polled when lastMessageDate is present and interval has elapsed" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds"          -> "60",
          "submission-poll-default-interval-seconds" -> "10"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submittedAt       = LocalDateTime.now().minusSeconds(10)
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
        .set(LastMessageDatePage, Instant.now().minusSeconds(60))
        .success
        .value
        .set(PollIntervalPage, 5)
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("ACCEPTED", Some("someUrl"), None, None, None, None, None)))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val result = service.checkAndUpdateSubmissionStatusIfAllowed(ua).futureValue

      result mustBe PollDecision.Polled("ACCEPTED")
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])
      verify(sessionRepository).set(any[UserAnswers])
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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ua = uaBase

      when(connector.getSubmissionStatus(any, any)(any))
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("someUrl"), None, None, None, None, None)))

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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
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

    "update status and save PollUrl, PollInterval and LastMessageDate when present in poll response" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "3600"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submittedAt       = LocalDateTime.now().minusSeconds(60)
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
        .thenReturn(
          Future.successful(
            ChrisPollResponse(
              "PENDING",
              Some("newPollUrl"),
              Some(30),
              Some(
                JsObject(Seq("number" -> JsString("5005"), "type" -> JsString("fatal"), "text" -> JsString("Boom")))
              ),
              Some("2342345asdfasdgf"),
              Some("2025-01-01T00:00:30Z"),
              Some("2025-01-01T00:00:30Z")
            )
          )
        )
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "PENDING"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])
      verify(connector).updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "PENDING"
      savedUa.get(PollUrlPage).value mustBe "newPollUrl"
      savedUa.get(PollIntervalPage).value mustBe 30
      savedUa.get(LastMessageDatePage).value mustBe Instant.parse("2025-01-01T00:00:30Z")
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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = LocalDateTime.now().minusSeconds(60)
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
        .thenReturn(
          Future.successful(ChrisPollResponse("PENDING", Some("newPollUrl"), Some(30), None, None, None, None))
        )
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "PENDING"
      verify(connector).getSubmissionStatus(any, any[String])(any[HeaderCarrier])
      verify(connector).updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier])

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "PENDING"
      savedUa.get(PollUrlPage).value mustBe "newPollUrl"
      savedUa.get(PollIntervalPage).value mustBe 30
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe false
    }

    "not update LastMessageDatePage when poll response does not contain lastMessageDate" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submittedAt       = LocalDateTime.now().minusSeconds(60)
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = submittedAt
      )

      val existingLastMessageDate = Instant.parse("2025-01-01T00:00:00Z")

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
        .set(LastMessageDatePage, existingLastMessageDate)
        .success
        .value

      when(connector.getSubmissionStatus(any, any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("newUrl"), Some(10), None, None, None, None)))
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "TIMED_OUT"

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(LastMessageDatePage).value mustBe existingLastMessageDate
    }

    "set hmrcMarkGgis to None when poll returns SUBMITTED without irMarkReceived" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "3600"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = new SubmissionService(
        connector,
        appConfig,
        sessionRepository,
        chrisRequestBuilder,
        itemPayloadBuilder,
        utcClock
      )

      val submittedAt       = LocalDateTime.now().minusSeconds(60)
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
        .thenReturn(Future.successful(ChrisPollResponse("SUBMITTED", Some("newUrl"), Some(10), None, None, None, None)))
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "SUBMITTED"

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())
      val saved                               = captor.getValue.get(SubmissionDetailsPage).value
      saved.status mustBe "SUBMITTED"
      saved.hmrcMarkGgis mustBe None
    }

    "set hmrcMarkGgis from irMarkReceived when poll returns SUBMITTED" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "3600"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = new SubmissionService(
        connector,
        appConfig,
        sessionRepository,
        chrisRequestBuilder,
        itemPayloadBuilder,
        utcClock
      )

      val submittedAt       = LocalDateTime.now().minusSeconds(60)
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
        .thenReturn(
          Future.successful(
            ChrisPollResponse("SUBMITTED", Some("newUrl"), Some(10), None, Some("IR-MARK-RECEIVED"), None, None)
          )
        )
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "SUBMITTED"

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())
      val saved                               = captor.getValue.get(SubmissionDetailsPage).value
      saved.status mustBe "SUBMITTED"
      saved.hmrcMarkGgis mustBe Some("IR-MARK-RECEIVED")
    }

    "mark as timed out when timeout exceeded and status is still PENDING" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = LocalDateTime.now().minusSeconds(120)
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
        .thenReturn(Future.successful(ChrisPollResponse("PENDING", Some("someurl"), None, None, None, None, None)))
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "TIMED_OUT"

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
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = LocalDateTime.now().minusSeconds(120)
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
        .thenReturn(Future.successful(ChrisPollResponse("ACCEPTED", Some("someurl"), None, None, None, None, None)))
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "TIMED_OUT"

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "ACCEPTED"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe true
    }

    "mark as timed out without polling when timeout exceeded and status remains PENDING" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      import models.submission.SubmissionDetails
      import pages.submission.{SubmissionDetailsPage, SubmissionStatusTimedOutPage}

      val submittedAt       = LocalDateTime.now().minusSeconds(120)
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

      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val result = service.checkAndUpdateSubmissionStatus(ua).futureValue

      result mustBe "TIMED_OUT"

      val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(captor.capture())

      val savedUa = captor.getValue
      savedUa.get(SubmissionDetailsPage).value.status mustBe "PENDING"
      savedUa.get(SubmissionStatusTimedOutPage("sub-123")).value mustBe true
    }
  }

  "sendSuccessEmail" - {

    "do nothing when success email already sent" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration("submission-poll-timeout-seconds" -> "60")
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val ua           = uaBase
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), true)
        .success
        .value

      val out = service.sendSuccessEmail(ua, "en").futureValue
      out mustBe ua

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "send email, set SuccessEmailSentPage to true and persist" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"

      val ua = uaBase
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value

      when(
        connector
          .sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(any[HeaderCarrier], any[ExecutionContext])
      )
        .thenReturn(Future.unit)

      when(sessionRepository.get(any[String])).thenReturn(Future.successful(Some(ua)))

      val savedCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(sessionRepository.set(savedCaptor.capture()))
        .thenReturn(Future.successful(true))

      val out = service.sendSuccessEmail(ua, "en").futureValue

      out.get(SuccessEmailSentPage(submissionId)).value mustBe true
      savedCaptor.getValue.get(SuccessEmailSentPage(submissionId)).value mustBe true

      verify(connector)
        .sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(any[HeaderCarrier], any[ExecutionContext])
      verify(sessionRepository).set(any[UserAnswers])
    }

    "throw IllegalStateException when SubmissionDetailsPage missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ex = intercept[IllegalStateException] {
        service.sendSuccessEmail(uaBase, "en").futureValue
      }
      ex.getMessage mustBe "Submission details missing"

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "throw IllegalStateException when Month/Year not selected (nil)" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   =
        new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val details      = SubmissionDetails(
        id = submissionId,
        status = "SUBMITTED",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )

      val ua = UserAnswers("id", Json.obj())
        .set(SubmissionDetailsPage, details)
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value

      val ex = intercept[IllegalStateException] {
        service.sendSuccessEmail(ua, "en").futureValue
      }

      ex.getMessage mustBe "Month/Year not selected"
      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "throw IllegalStateException when DateConfirmNilPaymentsPage missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val details      = SubmissionDetails(
        id = submissionId,
        status = "SUBMITTED",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )

      val ua = UserAnswers("id", Json.obj())
        .set(SubmissionDetailsPage, details)
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value
        .set(EnterYourEmailAddressPage, "test@example.com")
        .success
        .value
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value

      val ex = intercept[IllegalStateException] {
        service.sendSuccessEmail(ua, "en")
      }
      ex.getMessage mustBe "Month/Year not selected"

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "send email using DateConfirmPaymentsPage for monthly standard return" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   =
        new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val ua           = UserAnswers("id", Json.obj())
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 10, 5))
        .success
        .value
        .set(EnterYourEmailAddressPage, "standard@test.com")
        .success
        .value

      when(
        connector.sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.unit)

      when(sessionRepository.get(any[String])).thenReturn(Future.successful(Some(ua)))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      service.sendSuccessEmail(ua, "cy").futureValue

      val reqCaptor: ArgumentCaptor[SendSuccessEmailRequest] =
        ArgumentCaptor.forClass(classOf[SendSuccessEmailRequest])

      verify(connector)
        .sendSuccessfulEmail(eqTo(submissionId), reqCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])

      reqCaptor.getValue.month mustBe "Hydref"
      reqCaptor.getValue.year mustBe "2025"
      reqCaptor.getValue.language mustBe "cy"
    }

    "do not send email and just persist when monthly standard return email is missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   =
        new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val ua           = UserAnswers("id", Json.obj())
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 10, 5))
        .success
        .value
        .set(EnterYourEmailAddressPage, "   ")
        .success
        .value

      when(sessionRepository.get(any[String])).thenReturn(Future.successful(Some(ua)))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val out = service.sendSuccessEmail(ua, "en").futureValue

      out.get(SuccessEmailSentPage(submissionId)).value mustBe true
      verifyNoInteractions(connector)
      verify(sessionRepository).set(any[UserAnswers])
    }

    "send email using EnterYourEmailAddressPage for monthly standard return" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   =
        new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val ua           = UserAnswers("id", Json.obj())
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 10, 5))
        .success
        .value
        .set(EnterYourEmailAddressPage, "standard@test.com")
        .success
        .value

      when(
        connector.sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.unit)

      when(sessionRepository.get(any[String])).thenReturn(Future.successful(Some(ua)))
      when(sessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      service.sendSuccessEmail(ua, "en").futureValue

      val reqCaptor: ArgumentCaptor[SendSuccessEmailRequest] =
        ArgumentCaptor.forClass(classOf[SendSuccessEmailRequest])

      verify(connector)
        .sendSuccessfulEmail(eqTo(submissionId), reqCaptor.capture())(any[HeaderCarrier], any[ExecutionContext])

      reqCaptor.getValue.email mustBe "standard@test.com"
      reqCaptor.getValue.language mustBe "en"
    }

    "throw IllegalStateException when DateConfirmPaymentsPage missing" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   =
        new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val ua           = UserAnswers("id", Json.obj())
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value

      val ex = intercept[IllegalStateException] {
        service.sendSuccessEmail(ua, "en").futureValue
      }

      ex.getMessage mustBe "Month/Year not selected"
    }

    "throw IllegalStateException when DateConfirmPaymentsPage missing for monthly standard return" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   =
        new FrontendAppConfig(Configuration("submission-poll-timeout-seconds" -> "60"))
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val submissionId = "sub-123"
      val ua           = UserAnswers("id", Json.obj())
        .set(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = submissionId,
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
          )
        )
        .success
        .value
        .set(SuccessEmailSentPage(submissionId), false)
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(EnterYourEmailAddressPage, "standard@test.com")
        .success
        .value

      val ex = intercept[IllegalStateException] {
        service.sendSuccessEmail(ua, "en").futureValue
      }

      ex.getMessage mustBe "Month/Year not selected"
    }
  }

  "isAlreadySubmitted" - {
    "should return true when submission exists and is true" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val ym = YearMonth.of(2026, 3)

      val userAnswers = emptyUserAnswers
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 3, 5))
        .success
        .value
        .set(SubmissionCreatedPage(ym.toString), true)
        .success
        .value

      val result = service.isAlreadySubmitted(userAnswers)

      result mustBe true
    }

    "should return false when submission does not exist" in {
      val connector: ConstructionIndustrySchemeConnector = mock(classOf[ConstructionIndustrySchemeConnector])
      val sessionRepository: SessionRepository           = mock(classOf[SessionRepository])
      val appConfig: FrontendAppConfig                   = new FrontendAppConfig(
        Configuration(
          "submission-poll-timeout-seconds" -> "60"
        )
      )
      val chrisRequestBuilder                            = mock(classOf[ChrisSubmissionRequestBuilder])
      val service                                        = mkService(connector, sessionRepository, appConfig, chrisRequestBuilder)

      val userAnswers = emptyUserAnswers
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 3, 5))
        .success
        .value

      val result = service.isAlreadySubmitted(userAnswers)

      result mustBe false
    }
  }

  // UUID-scoped submission tests
  "getOrCreateSubmissionForChris with id" - {

    "reuse the ID-scoped resubmission id without calling the connector" in {
      val (service, connector, sessionRepository, chrisRequestBuilder) = newScopedService()
      val id                                                           = "test-id"

      val ua = UserAnswers("id")
        .setOrException(ResubmissionIdPage(id), 13180L)
        .setOrException(ResubmissionIdPage, 999L)

      service.getOrCreateSubmissionForChris(ua, id).futureValue mustBe ("13180", ua, true)

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
      verifyNoInteractions(chrisRequestBuilder)
    }

    "reuse only the created submission id for the correct journey" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      val id                                         = "test-id"

      val ua = scopedUa(id)
        .setOrException(SubmissionCreatedPage("2025-10"), true)
        .setOrException(CreatedSubmissionIdPage("2025-10"), "legacy-submission")
        .setOrException(SubmissionCreatedForFlowPage(id, "2025-10"), true)
        .setOrException(CreatedSubmissionIdForFlowPage(id, "2025-10"), "scoped-submission")

      service.getOrCreateSubmissionForChris(ua, id).futureValue mustBe ("scoped-submission", ua, false)

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "create a submission when no ID-scoped submission exists" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      val id                                         = "test-id"

      val ua = scopedUa(id)
        .setOrException(SubmissionCreatedPage("2025-10"), true)
        .setOrException(CreatedSubmissionIdPage("2025-10"), "legacy-submission")

      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(CreateSubmissionResponse("scoped-submission")))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val (submissionId, updated, isResubmission) = service.getOrCreateSubmissionForChris(ua, id).futureValue

      submissionId mustBe "scoped-submission"
      isResubmission mustBe false
      updated.get(SubmissionCreatedForFlowPage(id, "2025-10")) mustBe Some(true)
      updated.get(CreatedSubmissionIdForFlowPage(id, "2025-10")) mustBe Some("scoped-submission")
      updated.get(CreatedSubmissionIdPage("2025-10")) mustBe Some("legacy-submission")

      verify(connector).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
      verify(sessionRepository).set(updated)
    }
  }

  "refreshMonthlyReturnItems with id" - {

    "refresh only the ID-scoped subcontractor items" in {
      val (service, connector, _, _) = newScopedService()
      val id                         = "test-id"

      val selected = SelectedSubcontractor(
        id = 99L,
        name = "Scoped Subcontractor",
        totalPaymentsMade = Some(BigDecimal("100")),
        costOfMaterials = Some(BigDecimal("10")),
        totalTaxDeducted = Some(BigDecimal("5"))
      )

      val ua = scopedUa(id, MonthlyStandardReturn)
        .setOrException(SelectedSubcontractorPage.all(id), Map(1 -> selected))
        .setOrException(SelectedSubcontractorForFlowPage(id, 1), selected)
        .setOrException(SelectedSubcontractorPage(1), selected.copy(id = 123L))

      when(connector.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.refreshMonthlyReturnItems(ua, id).futureValue mustBe (())

      val captor = ArgumentCaptor.forClass(classOf[UpdateMonthlyReturnItemRequest])
      verify(connector).updateMonthlyReturnItem(captor.capture())(any[HeaderCarrier])
      captor.getValue.subcontractorId mustBe 99L
      captor.getValue.instanceId mustBe "CIS-123"
    }

    "fail without updating the backend when an ID-scoped item cannot be built" in {
      val (service, connector, _, _) = newScopedService()
      val id                         = "test-id"

      val incomplete = SelectedSubcontractor(99L, "Incomplete", None, None, None)
      val ua         = scopedUa(id, MonthlyStandardReturn)
        .setOrException(SelectedSubcontractorPage.all(id), Map(1 -> incomplete))

      val exception = service.refreshMonthlyReturnItems(ua, id).failed.futureValue

      exception mustBe an[IllegalStateException]
      exception.getMessage must include("index: 1")
      verifyNoInteractions(connector)
    }
  }

  "create with id" - {

    "build the create request from ID-scoped answers and persist the created submission" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      val id                                         = "test-id"

      val ua = scopedUa(id, MonthlyAmendedNilReturn)
        .setOrException(EnterYourEmailAddressPage(id), "scoped@test.com")
        .setOrException(EnterYourEmailAddressPage, "legacy@test.com")

      when(connector.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(CreateSubmissionResponse("sub-123")))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val (_, updated) = service.create(ua, id).futureValue

      val captor = ArgumentCaptor.forClass(classOf[CreateSubmissionRequest])
      verify(connector).createSubmission(captor.capture())(any[HeaderCarrier])

      captor.getValue mustBe CreateSubmissionRequest(
        instanceId = "CIS-123",
        taxYear = 2025,
        taxMonth = 10,
        amendment = "Y",
        emailRecipient = Some("scoped@test.com")
      )
      updated.get(SubmissionCreatedForFlowPage(id, "2025-10")) mustBe Some(true)
      updated.get(CreatedSubmissionIdForFlowPage(id, "2025-10")) mustBe Some("sub-123")
      updated.get(SubmissionCreatedPage("2025-10")) mustBe None
    }

    "fail rather than use legacy answers when required scoped details are missing" in {
      val (service, connector, _, _) = newScopedService()

      val exception = service.create(uaBase, "test-id").failed.futureValue

      exception.getMessage mustBe "Monthly return details missing"
      verifyNoInteractions(connector)
    }
  }

  "submitToChrisAndPersist with id" - {

    "submit the ID-scoped return and persist its submission details" in {
      val (service, connector, sessionRepository, chrisRequestBuilder) = newScopedService()
      val id                                                           = "test-id"
      val ua                                                           = scopedUa(id, MonthlyAmendedNilReturn)

      when(connector.getCisTaxpayer()(any[HeaderCarrier])).thenReturn(Future.successful(taxpayer))
      stubRetrieveMonthlyReturnForEditDetails(connector)

      val builtRequest = mock(classOf[ChrisSubmissionRequest])
      when(
        chrisRequestBuilder.build(
          any[UserAnswers],
          eqTo(id),
          any[CisTaxpayer],
          eqTo(false),
          any[GetAllMonthlyReturnDetailsResponse],
          eqTo(false)
        )
      ).thenReturn(builtRequest)

      when(connector.submitToChris(eqTo("sub-123"), any[ChrisSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkChrisResp()))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      service.submitToChrisAndPersist("sub-123", ua, id, false, false).futureValue mustBe mkChrisResp()

      val editCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])
      verify(connector).retrieveMonthlyReturnForEditDetails(editCaptor.capture())(any[HeaderCarrier])
      editCaptor.getValue mustBe GetMonthlyReturnForEditRequest("CIS-123", 10, 2025, true)
      verify(connector).submitToChris(eqTo("sub-123"), eqTo(builtRequest))(any[HeaderCarrier])

      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(uaCaptor.capture())
      uaCaptor.getValue.get(SubmissionDetailsPage(id)).value.status mustBe "SUBMITTED"
      uaCaptor.getValue.get(CorrelationIdPage(id)) mustBe Some("CID123")
      uaCaptor.getValue.get(SubmissionDetailsPage) mustBe None
    }

    "fail when ID-scoped return details are missing even if legacy answers exist" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      when(connector.getCisTaxpayer()(any[HeaderCarrier])).thenReturn(Future.successful(taxpayer))

      val exception = service.submitToChrisAndPersist("sub-123", uaBase, "test-id", false, false).failed.futureValue

      exception.getMessage mustBe "CIS ID, month or year of return missing"
      verify(connector, never()).retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(
        any[HeaderCarrier]
      )
      verifyNoInteractions(sessionRepository)
    }
  }

  "updateSubmissionFromChrisResponse with id" - {

    "send an update request using ID-scoped answers" in {
      val (service, connector, _, _) = newScopedService()
      val id                         = "test-id"

      val ua = scopedUa(id, MonthlyAmendedNilReturn)
        .setOrException(EnterYourEmailAddressPage(id), "scoped@test.com")
        .setOrException(CisIdPage, "legacy-CIS")

      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.updateSubmissionFromChrisResponse("sub-123", ua, id, mkChrisResp()).futureValue mustBe (())

      val captor = ArgumentCaptor.forClass(classOf[UpdateSubmissionRequest])
      verify(connector).updateSubmission(eqTo("sub-123"), captor.capture())(any[HeaderCarrier])

      captor.getValue.instanceId mustBe "CIS-123"
      captor.getValue.taxYear mustBe 2025
      captor.getValue.taxMonth mustBe 10
      captor.getValue.amendment mustBe "Y"
      captor.getValue.emailRecipient mustBe Some("scoped@test.com")
      captor.getValue.submittableStatus mustBe "SUBMITTED"
    }
  }

  "getPollInterval with id" - {

    "use the ID-scoped interval or the configured default, never the legacy interval" in {
      val (service, _, _, _) = newScopedService()
      val ua                 = UserAnswers("id")
        .setOrException(PollIntervalPage, 25)
        .setOrException(PollIntervalPage("first-id"), 5)
        .setOrException(PollIntervalPage("second-id"), 15)

      service.getPollInterval(ua, "first-id") mustBe 5
      service.getPollInterval(ua, "second-id") mustBe 15
      service.getPollInterval(ua, "missing-id") mustBe 10
    }
  }

  "checkAndUpdateSubmissionStatusIfAllowed with id" - {

    "skip polling when the ID-scoped last message is too recent" in {
      val (service, connector, sessionRepository, _) = newScopedService()

      val ua = scopedUa("test-id")
        .setOrException(LastMessageDatePage("test-id"), Instant.now())
        .setOrException(PollIntervalPage("test-id"), 10)
        .setOrException(LastMessageDatePage, Instant.now().minusSeconds(60))

      service.checkAndUpdateSubmissionStatusIfAllowed(ua, "test-id").futureValue mustBe PollDecision.Skip

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }
  }

  "checkAndUpdateSubmissionStatus with id" - {

    "fail when ID-scoped submission details are missing" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      val ua                                         = scopedUa("test-id")
        .setOrException(
          SubmissionDetailsPage,
          SubmissionDetails(
            id = "legacy-submission",
            status = "PENDING",
            irMark = "legacy-ir-mark",
            submittedAt = LocalDateTime.now()
          )
        )

      service.checkAndUpdateSubmissionStatus(ua, "test-id").failed.futureValue.getMessage mustBe
        "No submission details present"

      verifyNoInteractions(connector)
      verifyNoInteractions(sessionRepository)
    }

    "poll and persist only the ID-scoped response details" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      val id                                         = "test-id"

      val details = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.now().minusSeconds(5)
      )
      val ua      = scopedUa(id)
        .setOrException(SubmissionDetailsPage(id), details)
        .setOrException(PollUrlPage(id), "old-url")

      when(connector.getSubmissionStatus(any[String], any[String])(any[HeaderCarrier]))
        .thenReturn(
          Future.successful(
            ChrisPollResponse("ACCEPTED", Some("new-url"), Some(30), None, None, None, None)
          )
        )
      when(connector.updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      service.checkAndUpdateSubmissionStatus(ua, id).futureValue mustBe "ACCEPTED"

      verify(connector).getSubmissionStatus(eqTo("old-url"), eqTo("sub-123"))(any[HeaderCarrier])
      verify(connector).updateSubmission(any[String], any[UpdateSubmissionRequest])(any[HeaderCarrier])

      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(sessionRepository).set(uaCaptor.capture())

      uaCaptor.getValue.get(SubmissionDetailsPage(id)).value.status mustBe "ACCEPTED"
      uaCaptor.getValue.get(PollUrlPage(id)) mustBe Some("new-url")
      uaCaptor.getValue.get(PollIntervalPage(id)) mustBe Some(30)
      uaCaptor.getValue.get(SubmissionDetailsPage) mustBe None
    }
  }

  "sendSuccessEmail with id" - {

    "send to the ID-scoped email address and persist the sent marker" in {
      val (service, connector, sessionRepository, _) = newScopedService()
      val id                                         = "test-id"

      val ua = scopedUa(id)
        .setOrException(
          SubmissionDetailsPage(id),
          SubmissionDetails(
            id = "sub-123",
            status = "SUBMITTED",
            irMark = "IR-MARK-123",
            submittedAt = LocalDateTime.now()
          )
        )
        .setOrException(EnterYourEmailAddressPage(id), "scoped@test.com")
        .setOrException(EnterYourEmailAddressPage, "legacy@test.com")

      when(
        connector.sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(
          any[HeaderCarrier],
          any[ExecutionContext]
        )
      ).thenReturn(Future.unit)
      when(sessionRepository.get(any[String])).thenReturn(Future.successful(Some(ua)))
      when(sessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val updated = service.sendSuccessEmail(ua, id, "en").futureValue

      val captor = ArgumentCaptor.forClass(classOf[SendSuccessEmailRequest])
      verify(connector).sendSuccessfulEmail(eqTo("sub-123"), captor.capture())(
        any[HeaderCarrier],
        any[ExecutionContext]
      )

      captor.getValue.email mustBe "scoped@test.com"
      captor.getValue.year mustBe "2025"
      captor.getValue.language mustBe "en"
      updated.get(SuccessEmailSentPage("sub-123")) mustBe Some(true)
      updated.get(EnterYourEmailAddressPage) mustBe Some("legacy@test.com")
    }
  }

  "isAlreadySubmitted with id" - {

    "check the created flag for the requested ID-scoped journey" in {
      val (service, _, _, _) = newScopedService()

      val ua = scopedUa("first-id")
        .setOrException(DateConfirmPaymentsPage("second-id"), LocalDate.of(2025, 10, 5))
        .setOrException(SubmissionCreatedPage("2025-10"), true)
        .setOrException(SubmissionCreatedForFlowPage("first-id", "2025-10"), true)

      service.isAlreadySubmitted(ua, "first-id") mustBe true
      service.isAlreadySubmitted(ua, "second-id") mustBe false
    }
  }

  private def uaWithSelectedSubcontractors(count: Int): UserAnswers =
    (1 to count).foldLeft(
      uaBase
        .set(CisIdPage, "instance-1")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 3, 1))
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
    ) { (ua, i) =>
      ua.set(
        SelectedSubcontractorPage(i),
        SelectedSubcontractor(
          id = i.toLong,
          name = s"Subcontractor $i",
          totalPaymentsMade = Some(BigDecimal("100")),
          costOfMaterials = Some(BigDecimal("10")),
          totalTaxDeducted = Some(BigDecimal("5"))
        )
      ).success
        .value
    }

  private def newScopedService(): (
    SubmissionService,
    ConstructionIndustrySchemeConnector,
    SessionRepository,
    ChrisSubmissionRequestBuilder
  ) = {
    val connector           = mock(classOf[ConstructionIndustrySchemeConnector])
    val sessionRepository   = mock(classOf[SessionRepository])
    val chrisRequestBuilder = mock(classOf[ChrisSubmissionRequestBuilder])
    val appConfig           = new FrontendAppConfig(
      Configuration(
        "submission-poll-timeout-seconds"          -> "60",
        "submission-poll-default-interval-seconds" -> "10"
      )
    )

    (
      mkService(connector, sessionRepository, appConfig, chrisRequestBuilder),
      connector,
      sessionRepository,
      chrisRequestBuilder
    )
  }

  private def scopedUa(id: String, returnType: ReturnType = MonthlyNilReturn): UserAnswers =
    UserAnswers("id")
      .setOrException(ReturnTypePage(id), returnType)
      .setOrException(CisIdPage(id), "CIS-123")
      .setOrException(DateConfirmPaymentsPage(id), LocalDate.of(2025, 10, 5))

  private def uaBase: UserAnswers =
    emptyUserAnswers
      .set(CisIdPage, "123")
      .success
      .value
      .set(DateConfirmPaymentsPage, LocalDate.of(2025, 10, 5))
      .success
      .value
      .set(EnterYourEmailAddressPage, "test@test.com")
      .success
      .value
      .set(ReturnTypePage, MonthlyNilReturn)
      .success
      .value

  private def uaWithInactivityYes: UserAnswers =
    uaBase.set(InactivityRequestPage, InactivityRequest.Option1).success.value

  private lazy val agentDate: AgentClientData =
    AgentClientData("CLIENT-123", "taxOfficeNumber", "taxOfficeReference", Some("PAL 355 Scheme"))

  private val emptyMonthlyReturnDetailsResponse = GetAllMonthlyReturnDetailsResponse(
    scheme = Seq.empty,
    monthlyReturn = Seq(MonthlyReturn(monthlyReturnId = 1, taxYear = 2025, taxMonth = 10, amendment = Some("N"))),
    subcontractors = Seq.empty,
    monthlyReturnItems = Seq.empty,
    submission = Seq.empty
  )

  private def stubRetrieveMonthlyReturnForEditDetails(connector: ConstructionIndustrySchemeConnector): Unit =
    when(connector.retrieveMonthlyReturnForEditDetails(any[GetMonthlyReturnForEditRequest])(any[HeaderCarrier]))
      .thenReturn(Future.successful(emptyMonthlyReturnDetailsResponse))

  private def mkChrisResp(
    status: String = "SUBMITTED",
    irmark: String = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
    corr: String = "CID123",
    ts: String = "2025-01-01T00:00:00",
    err: Option[JsObject] = None,
    accepted: Option[String] = None,
    govTalkErrorStatus: Option[GovTalkErrorStatus] = None
  ): ChrisSubmissionResponse =
    ChrisSubmissionResponse(
      submissionId = "sub-123",
      status = status,
      hmrcMarkGenerated = irmark,
      correlationId = Some(corr),
      responseEndPoint = None,
      gatewayTimestamp = Some(ts),
      acceptedTime = accepted,
      error = err,
      govTalkErrorStatus = govTalkErrorStatus
    )

}
