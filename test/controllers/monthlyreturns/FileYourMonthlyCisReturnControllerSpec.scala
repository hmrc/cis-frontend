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
import models.agent.AgentClientData
import models.{NormalMode, ReturnType, UserAnswers}
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}

import scala.concurrent.Future
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import repositories.SessionRepository
import services.MonthlyReturnService

class FileYourMonthlyCisReturnControllerSpec extends SpecBase with MockitoSugar {

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
        verify(mockRepo, times(2)).set(any())
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
        verify(mockRepo, times(2)).set(any())
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
        verify(mockRepo, times(2)).set(any())
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

    "Agent: missing instanceId => => ton/tor and hasClient=true => stores ReturnType, CisId and returns OK" in {
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
        verify(mockRepo, times(2)).set(any())
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
}
