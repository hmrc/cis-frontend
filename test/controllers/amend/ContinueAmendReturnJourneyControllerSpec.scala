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
import services.{CisTaxpayerService, ContinueAmendJourneyResult, FormpRdsReconcileService, MonthlyReturnService}
import models.finalvalidation.MonthlyFinalValidationSource
import models.requests.{GetMonthlyReturnForEditRequest, IdentifierRequest}
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
import org.mockito.invocation.InvocationOnMock
import pages.monthlyreturns.CisIdPage
import play.api.Application

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

  // --- Multi-tab helpers for continueAmendReturnJourneyWithCisId ---

  private val scopedInstanceId = "CIS-123"

  private def scopedUrl(isOriginalNilReturn: Boolean = false, instanceId: String = scopedInstanceId): String =
    controllers.amend.routes.ContinueAmendReturnJourneyController
      .continueAmendReturnJourneyWithCisId(
        ContinueReturnJourneyQueryParams(instanceId, 2025, 1, Some(isOriginalNilReturn))
      )
      .url

  private def flowIdFrom(res: Future[play.api.mvc.Result]): UUID =
    UUID.fromString(redirectLocation(res).value.split("/").last)

  private def tempRedirect(flowId: UUID): String =
    controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(flowId).url

  private def reconcileOk: FormpRdsReconcileService = {
    val reconcile = mock[FormpRdsReconcileService]
    when(reconcile.reconcile(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.unit)
    reconcile
  }

  private def scopedSessionRepo(initial: Option[UserAnswers] = Some(emptyUserAnswers)): SessionRepository = {
    val repo = mock[SessionRepository]
    when(repo.get(any[String])).thenReturn(Future.successful(initial))
    when(repo.set(any[UserAnswers])).thenReturn(Future.successful(true))
    repo
  }

  private def lastSaved(repo: SessionRepository): UserAnswers = {
    val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
    verify(repo).set(captor.capture())
    captor.getValue
  }

  private def stubPopulate(service: MonthlyReturnService, hasSubcontractors: Boolean, isNilReturn: Boolean): Unit =
    when(
      service.populateUserAnswersForContinueAmendJourney(
        any[UserAnswers],
        any[String],
        any[GetMonthlyReturnForEditRequest]
      )(any[HeaderCarrier])
    ).thenAnswer { (inv: InvocationOnMock) =>
      Future.successful[Either[String, ContinueAmendJourneyResult]](
        Right(ContinueAmendJourneyResult(inv.getArgument[UserAnswers](0), hasSubcontractors, isNilReturn))
      )
    }

  private def scopedApplication(
    service: MonthlyReturnService,
    repo: SessionRepository,
    reconcile: FormpRdsReconcileService = reconcileOk,
    enabled: Boolean = true,
    isAgent: Boolean = false,
    taxpayerCisId: String = scopedInstanceId,
    clientFound: Boolean = true
  ): Application = {
    val taxpayer = mock[CisTaxpayer]
    when(taxpayer.uniqueId).thenReturn(taxpayerCisId)
    when(taxpayer.taxOfficeNumber).thenReturn("123")
    when(taxpayer.taxOfficeRef).thenReturn("AB456")
    when(taxpayer.schemeName).thenReturn(Some("ABC Construction Ltd"))

    val taxpayerService = mock[CisTaxpayerService]
    when(taxpayerService.findForOrganisation(using any[IdentifierRequest[?]]))
      .thenReturn(Future.successful(taxpayer))
    when(taxpayerService.findForAgent(any[String])(using any[IdentifierRequest[?]]))
      .thenReturn(Future.successful(Option.when(clientFound)(taxpayer)))

    applicationBuilder(isAgent = isAgent, formpRdsReconcileService = reconcile)
      .configure("features.multi-tab-enabled" -> enabled)
      .overrides(
        bind[MonthlyReturnService].toInstance(service),
        bind[SessionRepository].toInstance(repo),
        bind[CisTaxpayerService].toInstance(taxpayerService)
      )
      .build()
  }

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

    "continueAmendReturnJourneyWithCisId" - {

      "must redirect to the scoped WhatDoYouWantToAmendNil page when original is nil and in-progress is nil" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()
        stubPopulate(service, hasSubcontractors = false, isNilReturn = true)

        val application = scopedApplication(service, repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = true)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)
          redirectLocation(res).value mustBe
            controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(flowId).url

          // an organisation user gets no agent client data
          lastSaved(repo).get(AgentClientDataPage(flowId.toString)) mustBe None
        }
      }

      Seq(true, false).foreach { originalIsNil =>
        s"must set the scoped final validation source when in-progress is standard with subcontractors (original nil = $originalIsNil)" in {
          val service = mock[MonthlyReturnService]
          val repo    = scopedSessionRepo()
          stubPopulate(service, hasSubcontractors = true, isNilReturn = false)

          val application = scopedApplication(service, repo)

          running(application) {
            val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = originalIsNil)).withBody(AnyContentAsEmpty)
            val res     = route(application, request).value

            status(res) mustBe SEE_OTHER

            val flowId = flowIdFrom(res)
            val id     = flowId.toString

            // TODO: update to SubcontractorDetailsAddedController.onPageLoadWithId once it exists
            redirectLocation(res).value mustBe tempRedirect(flowId)

            val saved = lastSaved(repo)
            saved.get(MonthlyFinalValidationSourcePage(id)) mustBe Some(
              MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
            )
            saved.get(MonthlyFinalValidationSourcePage) mustBe None
          }
        }
      }

      "must not set the final validation source when in-progress is standard with no subcontractors" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        val application = scopedApplication(service, repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = true)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)

          // TODO: update to WhatDoYouWantToAmendStandardController.onPageLoadWithId once it exists
          redirectLocation(res).value mustBe tempRedirect(flowId)

          val saved = lastSaved(repo)
          saved.get(MonthlyFinalValidationSourcePage(flowId.toString)) mustBe None
          saved.get(MonthlyFinalValidationSourcePage) mustBe None
        }
      }

      "must redirect to the standard amend route when original return is standard and in-progress return is nil" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()
        stubPopulate(service, hasSubcontractors = false, isNilReturn = true)

        val application = scopedApplication(service, repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = false)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          // TODO: update to WhatDoYouWantToAmendStandardController.onPageLoadWithId once it exists
          redirectLocation(res).value mustBe tempRedirect(flowIdFrom(res))
        }
      }

      "must pass the flow id and isAmendment = true to the service and keep existing answers" in {
        val service  = mock[MonthlyReturnService]
        val otherId  = UUID.randomUUID().toString
        val existing = emptyUserAnswers
          .set(CisIdPage, "LEGACY-CIS")
          .success
          .value
          .set(CisIdPage(otherId), "OTHER-CIS")
          .success
          .value
        val repo     = scopedSessionRepo(Some(existing))
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        val application = scopedApplication(service, repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)

          val idCaptor  = ArgumentCaptor.forClass(classOf[String])
          val reqCaptor = ArgumentCaptor.forClass(classOf[GetMonthlyReturnForEditRequest])

          verify(service).populateUserAnswersForContinueAmendJourney(
            any[UserAnswers],
            idCaptor.capture(),
            reqCaptor.capture()
          )(any[HeaderCarrier])

          idCaptor.getValue mustBe flowId.toString
          reqCaptor.getValue mustBe GetMonthlyReturnForEditRequest(
            instanceId = "CIS-123",
            taxYear = 2025,
            taxMonth = 1,
            isAmendment = true
          )

          val saved = lastSaved(repo)
          saved.get(CisIdPage) mustBe Some("LEGACY-CIS")
          saved.get(CisIdPage(otherId)) mustBe Some("OTHER-CIS")
        }
      }

      "must start from empty answers when there is no session yet" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo(None)
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        val application = scopedApplication(service, repo)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(service).populateUserAnswersForContinueAmendJourney(
            captor.capture(),
            any[String],
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])

          captor.getValue.data.value mustBe empty
        }
      }

      "must redirect to JourneyRecovery and not save when service returns Left" in {
        val service   = mock[MonthlyReturnService]
        val repo      = scopedSessionRepo()
        val reconcile = mock[FormpRdsReconcileService]

        when(
          service.populateUserAnswersForContinueAmendJourney(
            any[UserAnswers],
            any[String],
            any[GetMonthlyReturnForEditRequest]
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(Left("error")))

        val application = scopedApplication(service, repo, reconcile)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(repo, never()).set(any[UserAnswers])
          verifyNoInteractions(reconcile)
        }
      }

      "must redirect to JourneyRecovery when saving the populated answers fails" in {
        val service   = mock[MonthlyReturnService]
        val repo      = scopedSessionRepo()
        val reconcile = mock[FormpRdsReconcileService]
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        when(repo.set(any[UserAnswers])).thenReturn(Future.failed(new RuntimeException("Session error")))

        val application = scopedApplication(service, repo, reconcile)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verifyNoInteractions(reconcile)
        }
      }

      "must redirect to JourneyRecovery when the session cannot be read" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()

        when(repo.get(any[String])).thenReturn(Future.failed(new RuntimeException("Database error")))

        val application = scopedApplication(service, repo)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verifyNoInteractions(service)
          verify(repo, never()).set(any[UserAnswers])
        }
      }

      "must redirect to the landing page and not touch the service or session when multi-tab is disabled" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()

        val application = scopedApplication(service, repo, enabled = false)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe
            application.injector.instanceOf[FrontendAppConfig].returnsLandingPageUrl("CIS-123", None)

          verifyNoInteractions(service)
          verifyNoInteractions(repo)
        }
      }

      "must redirect to JourneyRecovery when the instanceId does not belong to the organisation" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()

        val application = scopedApplication(service, repo)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(instanceId = "SOMEONE-ELSE")).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verifyNoInteractions(service)
          verifyNoInteractions(repo)
        }
      }

      "must redirect to UnauthorisedOrganisationAffinity after saving when reconcile returns PRECONDITION_FAILED" in {
        val service   = mock[MonthlyReturnService]
        val repo      = scopedSessionRepo()
        val reconcile = mock[FormpRdsReconcileService]
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        when(reconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("missing", PRECONDITION_FAILED, PRECONDITION_FAILED)))

        val application = scopedApplication(service, repo, reconcile)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe
            controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url

          verify(reconcile).reconcile(eqTo("CIS-123"), eqTo("123"), eqTo("AB456"))(any[HeaderCarrier])
          verify(repo).set(any[UserAnswers])
        }
      }

      "must redirect to JourneyRecovery when reconcile returns a general error" in {
        val service   = mock[MonthlyReturnService]
        val repo      = scopedSessionRepo()
        val reconcile = mock[FormpRdsReconcileService]
        stubPopulate(service, hasSubcontractors = false, isNilReturn = false)

        when(reconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val application = scopedApplication(service, repo, reconcile)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(reconcile).reconcile(eqTo("CIS-123"), eqTo("123"), eqTo("AB456"))(any[HeaderCarrier])
          verify(repo).set(any[UserAnswers])
        }
      }

      "must save the scoped agent client data for an authorised agent" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()
        stubPopulate(service, hasSubcontractors = false, isNilReturn = true)

        val application = scopedApplication(service, repo, isAgent = true)

        running(application) {
          val request = FakeRequest(GET, scopedUrl(isOriginalNilReturn = true)).withBody(AnyContentAsEmpty)
          val res     = route(application, request).value

          status(res) mustBe SEE_OTHER

          val flowId = flowIdFrom(res)
          val id     = flowId.toString

          redirectLocation(res).value mustBe
            controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(flowId).url

          val saved = lastSaved(repo)
          saved.get(AgentClientDataPage(id)) mustBe
            Some(AgentClientData("CIS-123", "123", "AB456", Some("ABC Construction Ltd")))
          saved.get(AgentClientDataPage) mustBe None
        }
      }

      "must redirect an agent to JourneyRecovery when the client is not authorised" in {
        val service = mock[MonthlyReturnService]
        val repo    = scopedSessionRepo()

        val application = scopedApplication(service, repo, isAgent = true, clientFound = false)

        running(application) {
          val res = route(application, FakeRequest(GET, scopedUrl()).withBody(AnyContentAsEmpty)).value

          status(res) mustBe SEE_OTHER
          redirectLocation(res).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

          verifyNoInteractions(service)
          verifyNoInteractions(repo)
        }
      }
    }
  }
}
