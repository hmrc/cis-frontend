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

package controllers.monthlyreturns

import base.SpecBase
import config.FrontendAppConfig
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.MonthlyReturnService

import scala.concurrent.Future

class ManageCisReturnControllerSpec extends SpecBase with MockitoSugar {

  "ManageCisReturnController.onExit" - {

    "clear the monthly return journey and redirect to Manage CIS Return" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockMonthlyReturnService.clearSubmissionJourney(any[UserAnswers]))
        .thenReturn(Future.unit)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ManageCisReturnController.onExit().url)

        val result = route(application, request).value

        val appConfig =
          application.injector.instanceOf[FrontendAppConfig]

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          appConfig.returnsLandingPageUrl("1", None)

        verify(mockMonthlyReturnService)
          .clearSubmissionJourney(any[UserAnswers])
      }
    }

    "redirect to the system error page when the journey cannot be cleared" in {
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockMonthlyReturnService.clearSubmissionJourney(any[UserAnswers]))
        .thenReturn(Future.failed(new RuntimeException("clear failed")))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, routes.ManageCisReturnController.onExit().url)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.SystemErrorController.onPageLoad().url

        verify(mockMonthlyReturnService)
          .clearSubmissionJourney(any[UserAnswers])
      }
    }
  }
}
