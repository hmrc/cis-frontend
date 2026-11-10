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
import models.{JourneyScope, NormalMode, UserAnswers}
import services.{CisTaxpayerService, ContinueAmendJourneyResult, FormpRdsReconcileService, MonthlyReturnService}
import models.finalvalidation.MonthlyFinalValidationSource
import models.requests.GetMonthlyReturnForEditRequest
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.agent.AgentClientDataPage
import pages.finalvalidations.MonthlyFinalValidationSourcePage
import play.api.inject.bind
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import config.FrontendAppConfig
import models.monthlyreturns.{CisTaxpayer, ContinueReturnJourneyQueryParams}
import models.requests.{CisPath, IdentifierRequest}
import models.requests.CisPath.{CisId, CisOrg}
import org.mockito.invocation.InvocationOnMock
import pages.monthlyreturns.CisIdPage
import play.api.Application

import java.util.UUID
import scala.collection.mutable.ListBuffer
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

    "continueAmendReturnJourneyWithCisPath" - {

      "must redirect to the scoped WhatDoYouWantToAmendNil page when original is nil and in-progress is nil" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers)
        stubPopulate(service, hasSubcontractors = false, isNilReturn = true)

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = true)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe
            controllers.amend.routes.WhatDoYouWantToAmendNilController
              .onPageLoadWithId(CisOrg, flowIdFrom(res))
              .url
        }
      }

      Seq(true, false).foreach { originalIsNil =>
        s"must set the scoped final validation source when in-progress is standard with subcontractors (original nil = $originalIsNil)" in {
          val service = mock[MonthlyReturnService]
          val session = new InMemorySession(emptyUserAnswers)
          stubPopulate(service, hasSubcontractors = true, isNilReturn = false)

          val application = scopedApplication(service, session.repo)

          running(application) {
            val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = originalIsNil)).withBody(AnyContentAsEmpty)
            val res     = route(application, request).value

            status(res) mustBe SEE_OTHER

            val flowId = flowIdFrom(res)
            val id     = JourneyScope.subJourneyId(CisOrg, flowId)

            // TODO: update to SubcontractorDetailsAddedController.onPageLoadWithId once it exists
            redirectLocation(res).value mustBe tempRedirect(CisOrg, flowId)

            session.latest.get(MonthlyFinalValidationSourcePage(id)) mustBe Some(
              MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
            )
            session.latest.get(MonthlyFinalValidationSourcePage) mustBe None
          }
        }
      }

      "must not set the final validation source when in-progress is standard with no subcontractors" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers)
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = true)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)
          val id     = JourneyScope.subJourneyId(CisOrg, flowId)

          // TODO: update to WhatDoYouWantToAmendStandardController.onPageLoadWithId once it exists
          redirectLocation(res).value mustBe tempRedirect(CisOrg, flowId)

          session.latest.get(MonthlyFinalValidationSourcePage(id)) mustBe None
          session.latest.get(MonthlyFinalValidationSourcePage) mustBe None
        }
      }

      "must redirect to the standard amend route when original return is standard and in-progress return is nil" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers)
        stubPopulate(service, hasSubcontractors = false, isNilReturn = true)

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = false)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          // TODO: update to WhatDoYouWantToAmendStandardController.onPageLoadWithId once it exists
          redirectLocation(res).value mustBe tempRedirect(CisOrg, flowIdFrom(res))
        }
      }

      "must create a sub-journey, keep existing answers and pass the scoped id and isAmendment = true to the service" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers.set(CisIdPage, "LEGACY-CIS").success.value)
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)
          val id     = JourneyScope.subJourneyId(CisOrg, flowId)

          val idCaptor  = ArgumentCaptor.forClass(classOf[String])
          val reqCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

          verify(service).populateUserAnswersForContinueAmendJourney(
            any[UserAnswers],
            idCaptor.capture(),
            reqCaptor.capture()
          )(any[HeaderCarrier])

          idCaptor.getValue mustBe id
          reqCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
            instanceId = "CIS-123",
            taxYear = 2025,
            taxMonth = 1,
            isAmendment = true
          )

          session.latest.hasSubJourney(id) mustBe true
          session.latest.get(CisIdPage) mustBe Some("LEGACY-CIS") // other answers survive
        }
      }

      "must redirect to JourneyRecovery when service returns Left" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers)

        when(
          service.populateUserAnswersForContinueAmendJourney(
            any[UserAnswers],
            any[String],
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(Left("error")))

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          // only the empty sub-journey created by JourneyService is saved
          session.saves.size mustBe 1
        }
      }

      "must redirect to JourneyRecovery when saving the populated answers fails" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers, failSaveNumber = Some(2))
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecovery when the journey cannot be created" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers, failSaveNumber = Some(1))

        val application = scopedApplication(service, session.repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verifyNoInteractions(service)
        }
      }

      "must redirect to the landing page and not touch the session when multi-tab is disabled" in {
        val service = mock[MonthlyReturnService]
        val session = new InMemorySession(emptyUserAnswers)

        val application = scopedApplication(service, session.repo, enabled = false)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe
            application.injector.instanceOf[FrontendAppConfig].returnsLandingPageUrl("CIS-123", None)

          verifyNoInteractions(service)
          verifyNoInteractions(session.repo)
        }
      }

      "must redirect to UnauthorisedOrganisationAffinity before saving anything when reconcile returns PRECONDITION_FAILED" in {
        val service   = mock[MonthlyReturnService]
        val session   = new InMemorySession(emptyUserAnswers)
        val reconcile = mock[FormpRdsReconcileService]

        when(reconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("missing", PRECONDITION_FAILED, PRECONDITION_FAILED)))

        val application = scopedApplication(service, session.repo, reconcile)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe
            controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url

          verify(reconcile).reconcile(eqTo("CIS-123"), eqTo("123"), eqTo("AB456"))(any[HeaderCarrier])
          verifyNoInteractions(service)
          verifyNoInteractions(session.repo)
        }
      }

      "must redirect to JourneyRecovery before saving anything when reconcile returns a general error" in {
        val service   = mock[MonthlyReturnService]
        val session   = new InMemorySession(emptyUserAnswers)
        val reconcile = mock[FormpRdsReconcileService]

        when(reconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val application = scopedApplication(service, session.repo, reconcile)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verifyNoInteractions(service)
          verifyNoInteractions(session.repo)
        }
      }

      "must scope the journey to the client for an authorised agent" in {
        val agentCisId = "CIS123" // CisPath only accepts letters and digits
        val service    = mock[MonthlyReturnService]
        val session    = new InMemorySession(emptyUserAnswers)
        stubPopulate(service, hasSubcontractors = false, isNilReturn = true)

        val application = scopedApplication(service, session.repo, isAgent = true, cisId = agentCisId)

        running(application) {
          val request = FakeRequest(
            GET,
            scopedUrl(CisId(agentCisId), isOriginalNilReturn = true, instanceId = agentCisId)
          ).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)
          val id     = JourneyScope.subJourneyId(CisId(agentCisId), flowId)

          redirectLocation(res).value mustBe
            controllers.amend.routes.WhatDoYouWantToAmendNilController
              .onPageLoadWithId(CisId(agentCisId), flowId)
              .url

          val idCaptor = ArgumentCaptor.forClass(classOf[String])
          verify(service).populateUserAnswersForContinueAmendJourney(
            any[UserAnswers],
            idCaptor.capture(),
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])

          idCaptor.getValue mustBe id
          session.latest.hasSubJourney(id) mustBe true
        }
      }
    }
  }
}
