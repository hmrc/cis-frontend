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

package controllers.monthlyreturns

import base.SpecBase
import config.FrontendAppConfig
import models.agent.AgentClientData
import models.monthlyreturns.CisTaxpayer
import models.requests.CisPath.{CisId, CisOrg}
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.requests.IdentifierRequest
import models.{JourneyScope, NormalMode, ReturnType, UserAnswers}
import org.mockito.Mockito.*
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}

import scala.concurrent.Future
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.ReturnTypePage
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import repositories.SessionRepository
import services.{CisTaxpayerService, FormpRdsReconcileService, MonthlyReturnService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.util.UUID

class FileYourMonthlyCisReturnControllerSpec extends SpecBase with MockitoSugar {

  private val cisId = "123456789"

  private def scopedApplication(
    mockRepo: SessionRepository,
    mockReconcile: FormpRdsReconcileService,
    enabled: Boolean = true,
    isAgent: Boolean = false
  ): Application = {
    val taxpayer = mock[CisTaxpayer]

    when(taxpayer.uniqueId).thenReturn(cisId)
    when(taxpayer.taxOfficeNumber).thenReturn("123")
    when(taxpayer.taxOfficeRef).thenReturn("AB456")

    val taxpayerService = mock[CisTaxpayerService]

    if (isAgent) {
      when(taxpayerService.findForAgent(eqTo(cisId))(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(Some(taxpayer)))
    } else {
      when(taxpayerService.findForOrganisation(using any[IdentifierRequest[?]]))
        .thenReturn(Future.successful(taxpayer))
    }

    applicationBuilder(
      isAgent = isAgent,
      formpRdsReconcileService = mockReconcile
    )
      .configure("features.multi-tab-enabled" -> enabled)
      .overrides(
        bind[CisTaxpayerService].toInstance(taxpayerService),
        bind[SessionRepository].toInstance(mockRepo)
      )
      .build()
  }

  "FileYourMonthlyCisReturnController.startMonthlyReturn" - {

    "Org: with instanceId => stores ReturnType, CisId and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockRepo, times(2)).set(any())
        verifyNoInteractions(mockService)
      }
    }

    "Org: without instanceId => fetch and store CisId, stores ReturnType and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", userAnswersWithCisId)))

      val app =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url
          )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockRepo).set(any())
        verify(mockService).resolveAndStoreCisId(any(), any())(any())
      }
    }

    "Org: without instanceId => resolveAndStoreCisId returns NOT_FOUND then redirects to Journey Recovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND)))

      val app =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url
          )

        val result = route(app, request).value
        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo).set(any())
        verify(mockService).resolveAndStoreCisId(any(), any())(any())
      }
    }

    "Agent: with instanceId + ton/tor and hasClient=true => stores ReturnType, CisId and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(true))
      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val req = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val res = route(app, req).value
        status(res) mustBe OK

        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verify(mockRepo, times(3)).set(any())
      }
    }

    "Agent: with instanceId + ton/tor and hasClient=false => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(false))

      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo).set(any())
        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
      }
    }

    "Agent: with instanceId but missing ton/tor => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(None)
        )

      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockRepo).set(any())
      }
    }

    "Agent: missing instanceId => ton/tor and hasClient=true => stores ReturnType, CisId and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(true))
      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url
          )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockRepo, times(3)).set(any())
      }
    }

    "Agent: missing instanceId => ton/tor and hasClient=true => missing agent data then redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(None)
        )
      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url
          )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockRepo, times(1)).set(any())
      }
    }

    "Agent: hasClient throws => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verify(mockRepo).set(any())
      }
    }
  }

  "FileYourMonthlyCisReturnController.onSubmit" - {

    "redirects to DateConfirmPayments for MonthlyStandardReturn and persists cleaned answers" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmit(ReturnType.MonthlyStandardReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.DateConfirmPaymentsController
            .onPageLoad(NormalMode, Some(ReturnType.MonthlyStandardReturn))
            .url

        verify(mockRepo).set(any())
        verifyNoInteractions(mockService)
      }
    }

    "redirects to DateConfirmPayments for MonthlyNilReturn and persists cleaned answers" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmit(ReturnType.MonthlyNilReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.DateConfirmPaymentsController
            .onPageLoad(NormalMode, Some(ReturnType.MonthlyNilReturn))
            .url

        verify(mockRepo).set(any())
        verifyNoInteractions(mockService)
      }
    }

    "redirects to JourneyRecovery when no user answers exist" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      val app =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmit(ReturnType.MonthlyStandardReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockRepo)
        verifyNoInteractions(mockService)
      }
    }

    "redirects to JourneyRecovery when session repository fails" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.failed(new RuntimeException("boom")))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmit(ReturnType.MonthlyStandardReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo).set(any())
        verifyNoInteractions(mockService)
      }
    }
  }

  "FileYourMonthlyCisReturnController.startNilReturn" - {

    "Org: with instanceId => stores ReturnType, CisId and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockRepo, times(2)).set(any())
        verifyNoInteractions(mockService)
      }
    }

    "Org: without instanceId => fetch and store CisId, stores ReturnType and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.successful(("CIS-123", userAnswersWithCisId)))

      val app =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url
          )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockRepo).set(any())
        verify(mockService).resolveAndStoreCisId(any(), any())(any())
      }
    }

    "Org: without instanceId => resolveAndStoreCisId returns NOT_FOUND then redirects to Journey Recovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockService.resolveAndStoreCisId(any[UserAnswers], any[Boolean])(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND)))

      val app =
        applicationBuilder(userAnswers = None)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url
          )

        val result = route(app, request).value
        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo).set(any())
        verify(mockService).resolveAndStoreCisId(any(), any())(any())
      }
    }

    "Agent: with instanceId + ton/tor and hasClient=true => stores ReturnType, CisId and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(true))
      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val req = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url +
            "?instanceId=CIS-123"
        )

        val res = route(app, req).value
        status(res) mustBe OK

        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verify(mockRepo, times(3)).set(any())
      }
    }

    "Agent: with instanceId + ton/tor and hasClient=false => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(false))

      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo).set(any())
        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
      }
    }

    "Agent: with instanceId but missing ton/tor => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(None)
        )

      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockRepo).set(any())
      }
    }

    "Agent: missing instanceId => ton/tor and hasClient=true => stores ReturnType, CisId and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(true))
      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url
          )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockRepo, times(3)).set(any())
      }
    }

    "Agent: missing instanceId => ton/tor and hasClient=true => missing agent data then redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(None)
        )
      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url
          )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockRepo, times(1)).set(any())
      }
    }

    "Agent: hasClient throws => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      when(mockRepo.set(any()))
        .thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startNilReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).getAgentClient(eqTo(emptyUserAnswers.id))(any(), any())
        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verify(mockRepo).set(any())
      }
    }
  }

  "FileYourMonthlyCisReturnController FormP/RDS reconciliation" - {

    "Org: reconcile fails with PRECONDITION_FAILED => redirect to register (UnauthorisedOrganisationAffinity)" in {
      val mockRepo      = mock[SessionRepository]
      val mockService   = mock[MonthlyReturnService]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("missing", PRECONDITION_FAILED, PRECONDITION_FAILED)))

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          formpRdsReconcileService = mockReconcile
        )
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo("CIS-123"), any(), any())(any[HeaderCarrier])
      }
    }

    "Org: reconcile fails with a general error => redirect JourneyRecovery" in {
      val mockRepo      = mock[SessionRepository]
      val mockService   = mock[MonthlyReturnService]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          formpRdsReconcileService = mockReconcile
        )
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo("CIS-123"), any(), any())(any[HeaderCarrier])
      }
    }

    "Org: reconcile succeeds => uses employer tax office reference and returns OK" in {
      val mockRepo      = mock[SessionRepository]
      val mockService   = mock[MonthlyReturnService]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          formpRdsReconcileService = mockReconcile
        )
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockReconcile)
          .reconcile(eqTo("CIS-123"), eqTo("taxOfficeNumber"), eqTo("taxOfficeReference"))(any[HeaderCarrier])
      }
    }

    "Agent: reconcile uses agent client tax office reference and returns OK" in {
      val mockRepo      = mock[SessionRepository]
      val mockService   = mock[MonthlyReturnService]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockService.getAgentClient(any())(any(), any()))
        .thenReturn(
          Future.successful(Some(AgentClientData("CLIENT-123", "163", "AB0063", Some("ABC Construction Ltd"))))
        )
      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(true))
      when(mockRepo.set(any())).thenReturn(Future.successful(true))
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          formpRdsReconcileService = mockReconcile
        )
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.startMonthlyReturn().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockReconcile)
          .reconcile(eqTo("CIS-123"), eqTo("163"), eqTo("AB0063"))(any[HeaderCarrier])
      }
    }
  }

  "FileYourMonthlyCisReturnController UUID-scoped entry points" - {

    "return OK for the new standard monthly return entry point" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val app = scopedApplication(mockRepo, mockReconcile)

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .startMonthlyReturnWithCisPath(CisOrg)
            .url
        )

        val result = route(app, request).value

        val expectedPostRoute =
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisOrg, MonthlyStandardReturn)
            .url

        status(result) mustBe OK
        contentAsString(result) must include(expectedPostRoute)

        verify(mockReconcile)
          .reconcile(eqTo(cisId), eqTo("123"), eqTo("AB456"))(any[HeaderCarrier])

        verifyNoInteractions(mockRepo)
      }
    }

    "return OK for the new nil return entry point" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val app = scopedApplication(mockRepo, mockReconcile)

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .startNilReturnWithCisPath(CisOrg)
            .url
        )

        val result = route(app, request).value

        val expectedPostRoute =
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisOrg, MonthlyNilReturn)
            .url

        status(result) mustBe OK
        contentAsString(result) must include(expectedPostRoute)

        verify(mockReconcile)
          .reconcile(eqTo(cisId), eqTo("123"), eqTo("AB456"))(any[HeaderCarrier])

        verifyNoInteractions(mockRepo)
      }
    }

    "redirect to CIS Manage Returns when multi-tab is disabled" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      val app = scopedApplication(mockRepo, mockReconcile, enabled = false)

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .startMonthlyReturnWithCisPath(CisOrg)
            .url
        )

        val result = route(app, request).value

        val expectedUrl = app.injector
          .instanceOf[FrontendAppConfig]
          .returnsLandingPageUrl(cisId, None)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedUrl

        verifyNoInteractions(mockRepo, mockReconcile)
      }
    }

    "redirect the nil return entry point when multi-tab is disabled" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      val app = scopedApplication(mockRepo, mockReconcile, enabled = false)

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .startNilReturnWithCisPath(CisOrg)
            .url
        )

        val result = route(app, request).value

        val expectedUrl = app.injector
          .instanceOf[FrontendAppConfig]
          .returnsLandingPageUrl(cisId, None)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedUrl

        verifyNoInteractions(mockRepo, mockReconcile)
      }
    }

    "create a UUID-scoped standard return journey on submission" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockRepo.get(any[String]))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))

      when(mockRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val app = scopedApplication(mockRepo, mockReconcile)

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisOrg, MonthlyStandardReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).value
        val flowId      = UUID.fromString(redirectUrl.split("/").last)
        val id          = JourneyScope.subJourneyId(CisOrg, flowId)

        redirectUrl mustBe
          controllers.monthlyreturns.routes.DateConfirmPaymentsController
            .onPageLoadWithId(CisOrg, flowId, NormalMode)
            .url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockRepo).set(captor.capture())

        val savedAnswers = captor.getValue

        savedAnswers.hasSubJourney(id) mustBe true
        savedAnswers.get(ReturnTypePage(id)) mustBe Some(MonthlyStandardReturn)
        savedAnswers.get(ReturnTypePage) mustBe None
      }
    }

    "create a UUID-scoped nil return journey on submission" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockRepo.get(any[String]))
        .thenReturn(Future.successful(Some(emptyUserAnswers)))

      when(mockRepo.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val app = scopedApplication(mockRepo, mockReconcile)

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisOrg, MonthlyNilReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER

        val redirectUrl = redirectLocation(result).value
        val flowId      = UUID.fromString(redirectUrl.split("/").last)
        val id          = JourneyScope.subJourneyId(CisOrg, flowId)

        redirectUrl mustBe
          controllers.monthlyreturns.routes.DateConfirmPaymentsController
            .onPageLoadWithId(CisOrg, flowId, NormalMode)
            .url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])

        verify(mockRepo).set(captor.capture())

        val savedAnswers = captor.getValue

        savedAnswers.hasSubJourney(id) mustBe true
        savedAnswers.get(ReturnTypePage(id)) mustBe Some(MonthlyNilReturn)
        savedAnswers.get(ReturnTypePage) mustBe None
      }
    }

    "not create a new journey when multi-tab is disabled" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      val app = scopedApplication(mockRepo, mockReconcile, enabled = false)

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisOrg, MonthlyStandardReturn)
            .url
        )

        val result = route(app, request).value

        val expectedUrl = app.injector
          .instanceOf[FrontendAppConfig]
          .returnsLandingPageUrl(cisId, None)

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe expectedUrl

        verifyNoInteractions(mockRepo, mockReconcile)
      }
    }

    "redirect to Journey Recovery when journey creation fails" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockRepo.get(any[String]))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val app = scopedApplication(mockRepo, mockReconcile)

      running(app) {
        val request = FakeRequest(
          POST,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisOrg, MonthlyStandardReturn)
            .url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo, never()).set(any[UserAnswers])
      }
    }

    "return OK for an authorised agent client" in {
      val mockRepo      = mock[SessionRepository]
      val mockReconcile = mock[FormpRdsReconcileService]

      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val app = scopedApplication(
        mockRepo,
        mockReconcile,
        isAgent = true
      )

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .startMonthlyReturnWithCisPath(CisId(cisId))
            .url
        )

        val result = route(app, request).value

        val expectedPostRoute =
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController
            .onSubmitWithCisPath(CisId(cisId), MonthlyStandardReturn)
            .url

        status(result) mustBe OK
        contentAsString(result) must include(expectedPostRoute)

        verify(mockReconcile)
          .reconcile(eqTo(cisId), eqTo("123"), eqTo("AB456"))(any[HeaderCarrier])
      }
    }
  }
}
