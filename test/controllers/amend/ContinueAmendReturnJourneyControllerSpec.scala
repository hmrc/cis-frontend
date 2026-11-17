/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.amend

import base.SpecBase
import models.{NormalMode, UserAnswers}
import models.agent.AgentClientData
import models.finalvalidation.MonthlyFinalValidationSource
import models.monthlyreturns.*
import models.requests.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.agent.AgentClientDataPage
import pages.finalvalidations.MonthlyFinalValidationSourcePage
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.util.UUID
import scala.concurrent.Future

class ContinueAmendReturnJourneyControllerSpec extends SpecBase with MockitoSugar {

  private val continueAmendReturnJourneyUrl =
    "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=false"

  private val populatedAnswers = UserAnswers("user-id")

  private val populatedAnswersWithSource = populatedAnswers.setOrException(
    MonthlyFinalValidationSourcePage,
    MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
  )

  "ContinueAmendReturnJourneyController" - {

    "must save user answers and redirect to SubcontractorDetailsAdded when in-progress return is standard" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = true,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode).url

        verify(mockSessionRepo).set(populatedAnswersWithSource)
      }
    }

    "must save user answers and redirect to WhatDoYouWantToAmendNil when original is nil and in-progress is nil" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = true
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=true"
        ).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url
      }
    }

    "must save user answers and redirect to WhatDoYouWantToAmendStandard when in-progress return is standard with no subcontractors" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=true"
        ).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad().url
      }
    }

    "must save user answers and redirect to SubcontractorDetailsAdded when in-progress return is standard with subcontractors" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = true,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=true"
        ).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode).url

        verify(mockSessionRepo).set(populatedAnswersWithSource)
      }
    }

    "must redirect to JourneyRecovery when service returns Left" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Left("error")))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockSessionRepo)
      }
    }

    "must redirect to WhatDoYouWantToAmendStandard when original return is standard and in-progress return is nil" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = true
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          "/monthly-return/continue-amend-return-journey?instanceId=CIS-123&taxYear=2025&taxMonth=1&isOriginalNilReturn=false"
        ).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad().url
      }
    }

    "must pass isAmendment = true to the service" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val application = applicationBuilder()
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        route(application, request).value.futureValue

        val captor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

        verify(mockService).populateUserAnswersForContinueAmendJourney(any[UserAnswers], captor.capture())(
          any[HeaderCarrier]
        )

        captor.getValue mustBe GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2025,
          taxMonth = 1,
          isAmendment = true
        )
      }
    }

    "must redirect to UnauthorisedOrganisationAffinity when reconcile returns PRECONDITION_FAILED" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]
      val mockReconcile   = mock[FormpRdsReconcileService]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("missing", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      val application = applicationBuilder(formpRdsReconcileService = mockReconcile)
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo("CIS-123"), any(), any())(any[HeaderCarrier])
      }
    }

    "must redirect to JourneyRecovery when reconcile returns a general error" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]
      val mockReconcile   = mock[FormpRdsReconcileService]

      val result = ContinueAmendJourneyResult(
        userAnswers = populatedAnswers,
        hasSubcontractors = false,
        isNilReturn = false
      )

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(Right(result)))

      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))

      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(formpRdsReconcileService = mockReconcile)
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)

        val res = route(application, request).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo("CIS-123"), any(), any())(any[HeaderCarrier])
      }
    }

    "must populate agent client data and reconcile with the client's reference when user is an agent" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]
      val mockReconcile   = mock[FormpRdsReconcileService]

      val agentData = AgentClientData("uniqueId", "120", "NF05844", None)
      val agentUa   = populatedAnswers.setOrException(AgentClientDataPage, agentData)

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(
        Future.successful(
          Right(ContinueAmendJourneyResult(populatedAnswers, hasSubcontractors = false, isNilReturn = false))
        )
      )
      when(mockService.populateAgentClientDataIfRequired(any[UserAnswers], any[String], eqTo(true))(any[HeaderCarrier]))
        .thenReturn(Future.successful(agentUa))
      when(mockSessionRepo.set(any[UserAnswers])).thenReturn(Future.successful(true))
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(()))

      val application = applicationBuilder(isAgent = true, formpRdsReconcileService = mockReconcile)
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val res = route(application, FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe
          controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo("CIS-123"), eqTo("120"), eqTo("NF05844"))(any[HeaderCarrier])
        verify(mockSessionRepo).set(agentUa)
      }
    }

    "must redirect to JourneyRecovery when agent client data population fails" in {
      val mockService     = mock[MonthlyReturnService]
      val mockSessionRepo = mock[SessionRepository]

      when(
        mockService.populateUserAnswersForContinueAmendJourney(any[UserAnswers], any[GetMonthlyReturnForEditRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(
        Future.successful(
          Right(ContinueAmendJourneyResult(populatedAnswers, hasSubcontractors = false, isNilReturn = false))
        )
      )
      when(mockService.populateAgentClientDataIfRequired(any[UserAnswers], any[String], eqTo(true))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("Agent no longer authorised for client")))

      val application = applicationBuilder(isAgent = true)
        .overrides(
          bind[MonthlyReturnService].toInstance(mockService),
          bind[SessionRepository].toInstance(mockSessionRepo)
        )
        .build()

      running(application) {
        val res = route(application, FakeRequest(GET, continueAmendReturnJourneyUrl).withBody(AnyContentAsEmpty)).value

        status(res) mustBe SEE_OTHER
        redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        verify(mockSessionRepo, never()).set(any[UserAnswers])
      }
    }

    // Multi-tab
    "continueAmendReturnJourneyWithCisId" - {

      "must preserve existing answers and redirect to scoped WhatDoYouWantToAmendStandard" in {
        val mockService     = mock[MonthlyReturnService]
        val mockSessionRepo = mock[SessionRepository]
        val mockTaxpayerSvc = mock[CisTaxpayerService]
        val mockReconcile   = mock[FormpRdsReconcileService]
        val taxpayer        = mock[CisTaxpayer]
        val otherId         = UUID.randomUUID().toString

        val existingAnswers = UserAnswers("id").setOrException(CisIdPage(otherId), "OTHER-CIS")

        val amendResult = ContinueAmendJourneyResult(
          userAnswers = existingAnswers,
          hasSubcontractors = false,
          isNilReturn = false
        )

        when(taxpayer.uniqueId).thenReturn("CIS-123")
        when(taxpayer.taxOfficeNumber).thenReturn("123")
        when(taxpayer.taxOfficeRef).thenReturn("AB456")

        when(mockTaxpayerSvc.findForOrganisation(using any[IdentifierRequest[?]]))
          .thenReturn(Future.successful(taxpayer))

        when(mockSessionRepo.get(any[String]))
          .thenReturn(Future.successful(Some(existingAnswers)))

        when(mockSessionRepo.set(any[UserAnswers]))
          .thenReturn(Future.successful(true))

        when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        when(
          mockService.populateUserAnswersForContinueAmendJourney(
            any[UserAnswers],
            any[String],
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(Right(amendResult)))

        val application = applicationBuilder(isAgent = false, formpRdsReconcileService = mockReconcile)
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockService),
            bind[SessionRepository].toInstance(mockSessionRepo),
            bind[CisTaxpayerService].toInstance(mockTaxpayerSvc)
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.ContinueAmendReturnJourneyController
              .continueAmendReturnJourneyWithCisId(
                ContinueReturnJourneyQueryParams("CIS-123", 2025, 1, Some(false))
              )
              .url
          ).withBody(AnyContentAsEmpty)

          val res = route(application, request).value

          status(res) mustBe SEE_OTHER

          val idCaptor      = ArgumentCaptor.forClass(classOf[String])
          val requestCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])
          val answersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])

          verify(mockService).populateUserAnswersForContinueAmendJourney(
            eqTo(existingAnswers),
            idCaptor.capture(),
            requestCaptor.capture()
          )(any[HeaderCarrier])

          verify(mockSessionRepo).set(answersCaptor.capture())

          val id     = idCaptor.getValue
          val flowId = UUID.fromString(id)
          val saved  = answersCaptor.getValue

          redirectLocation(res).value mustBe
            routes.WhatDoYouWantToAmendStandardController.onPageLoadWithId(flowId).url

          saved.get(CisIdPage(otherId)) mustBe Some("OTHER-CIS")

          requestCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
            instanceId = "CIS-123",
            taxYear = 2025,
            taxMonth = 1,
            isAmendment = true
          )
        }
      }
    }
  }
}
