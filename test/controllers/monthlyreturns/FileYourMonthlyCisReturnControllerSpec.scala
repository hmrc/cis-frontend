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

  "FileYourMonthlyCisReturnController.onPageLoad" - {

    "Org: with instanceId => stores CisIdPage and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = false)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockRepo).set(any())
        verifyNoInteractions(mockService)
      }
    }

    "Org: without instanceId => returns OK and does not store" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = false)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(GET, controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url)

        val result = route(app, request).value
        status(result) mustBe OK

        verifyNoInteractions(mockRepo)
        verifyNoInteractions(mockService)
      }
    }

    "Agent: with instanceId + ton/tor and hasClient=true => stores and returns OK" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

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
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url +
            "?instanceId=CIS-123&taxOfficeNumber=163&taxOfficeReference=AB0063"
        )

        val res = route(app, req).value
        status(res) mustBe OK

        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verify(mockRepo).set(any())
      }
    }

    "Agent: with instanceId + ton/tor and hasClient=false => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.successful(false))

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
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url +
            "?instanceId=CIS-123&taxOfficeNumber=163&taxOfficeReference=AB0063"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verifyNoInteractions(mockRepo)
      }
    }

    "Agent: with instanceId but missing ton/tor => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

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
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url +
            "?instanceId=CIS-123"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockService)
        verifyNoInteractions(mockRepo)
      }
    }

    "Agent: missing instanceId => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(
            bind[SessionRepository].toInstance(mockRepo),
            bind[MonthlyReturnService].toInstance(mockService)
          )
          .build()

      running(app) {
        val request =
          FakeRequest(GET, controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url)

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verifyNoInteractions(mockService)
        verifyNoInteractions(mockRepo)
      }
    }

    "Agent: hasClient throws => redirect JourneyRecovery" in {
      val mockRepo    = mock[SessionRepository]
      val mockService = mock[MonthlyReturnService]

      when(mockService.hasClient(eqTo("163"), eqTo("AB0063"))(any()))
        .thenReturn(Future.failed(new RuntimeException("boom")))

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
          controllers.monthlyreturns.routes.FileYourMonthlyCisReturnController.onPageLoad().url +
            "?instanceId=CIS-123&taxOfficeNumber=163&taxOfficeReference=AB0063"
        )

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService).hasClient(eqTo("163"), eqTo("AB0063"))(any())
        verifyNoInteractions(mockRepo)
      }
    }
  }
}
