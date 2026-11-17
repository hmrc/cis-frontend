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
import models.{CheckMode, NormalMode}
import pages.monthlyreturns.{CisIdPage, SubmitInactivityRequestPage}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.InactivityRequestWarningView

import java.util.UUID

class InactivityRequestWarningControllerSpec extends SpecBase {

  private def userAnswersWithInactivityRequest(value: Boolean) =
    userAnswersWithCisId.set(SubmitInactivityRequestPage, value).success.value

  private def scopedUserAnswersWithInactivityRequest(flowId: UUID, value: Boolean) = {
    val id = flowId.toString

    emptyUserAnswers
      .setOrException(CisIdPage(id), "test-cis-id")
      .setOrException(SubmitInactivityRequestPage(id), value)
  }

  "InactivityRequestWarning Controller" - {

    "must return OK and render the view with ConfirmationByEmail as next URL in NormalMode" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithInactivityRequest(true))).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(NormalMode).url
          )

        val result = route(application, request).value

        val view    = application.injector.instanceOf[InactivityRequestWarningView]
        val nextUrl = controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode).url

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(nextUrl)(request, messages(application)).toString
      }
    }

    "must return OK and render the view with CheckYourAnswers as next URL in CheckMode" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithInactivityRequest(true))).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(CheckMode).url
          )

        val result = route(application, request).value

        val view    = application.injector.instanceOf[InactivityRequestWarningView]
        val nextUrl = controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(nextUrl)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery when the inactivity request answer is No" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithInactivityRequest(false))).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(NormalMode).url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when the inactivity request answer is not present" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(NormalMode).url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "UUID-scoped journey" - {

      "must return OK and render the view with scoped ConfirmationByEmail as next URL in NormalMode" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswersWithInactivityRequest(flowId, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          val view    = application.injector.instanceOf[InactivityRequestWarningView]
          val nextUrl = controllers.monthlyreturns.routes.ConfirmationByEmailController
            .onPageLoadWithId(flowId, NormalMode)
            .url

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(nextUrl)(request, messages(application)).toString
        }
      }

      "must return OK and render the view with scoped CheckYourAnswers as next URL in CheckMode" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswersWithInactivityRequest(flowId, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, CheckMode)
                .url
            )

          val result = route(application, request).value

          val view    = application.injector.instanceOf[InactivityRequestWarningView]
          val nextUrl = controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(nextUrl)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when the scoped inactivity request answer is No" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswersWithInactivityRequest(flowId, false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped inactivity request answer is not present" in {

        val flowId = UUID.randomUUID()

        val userAnswers = emptyUserAnswers
          .setOrException(CisIdPage(flowId.toString), "test-cis-id")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must not use the legacy inactivity request answer when the scoped answer is missing" in {

        val flowId = UUID.randomUUID()

        val userAnswers = userAnswersWithInactivityRequest(true)
          .setOrException(CisIdPage(flowId.toString), "test-cis-id")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must display the warning only for the UUID-scoped journey with an inactivity request" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()

        val userAnswers = scopedUserAnswersWithInactivityRequest(firstFlowId, true)
          .setOrException(CisIdPage(secondFlowId.toString), "second-cis-id")
          .setOrException(SubmitInactivityRequestPage(secondFlowId.toString), false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val firstRequest =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(firstFlowId, NormalMode)
                .url
            )

          val secondRequest =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(secondFlowId, NormalMode)
                .url
            )

          val firstResult  = route(application, firstRequest).value
          val secondResult = route(application, secondRequest).value

          val view = application.injector.instanceOf[InactivityRequestWarningView]

          val nextUrl = controllers.monthlyreturns.routes.ConfirmationByEmailController
            .onPageLoadWithId(firstFlowId, NormalMode)
            .url

          status(firstResult) mustEqual OK
          contentAsString(firstResult) mustEqual view(nextUrl)(firstRequest, messages(application)).toString

          status(secondResult) mustEqual SEE_OTHER
          redirectLocation(secondResult).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = None)
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped CisIdPage is missing" in {

        val flowId = UUID.randomUUID()

        val userAnswers = emptyUserAnswers
          .setOrException(SubmitInactivityRequestPage(flowId.toString), true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped CisIdPage is blank" in {

        val flowId = UUID.randomUUID()

        val userAnswers = emptyUserAnswers
          .setOrException(CisIdPage(flowId.toString), " ")
          .setOrException(SubmitInactivityRequestPage(flowId.toString), true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the returns landing page when multi-tab is disabled" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswersWithInactivityRequest(flowId, true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> false)
          .build()

        running(application) {
          val request =
            FakeRequest(
              GET,
              controllers.monthlyreturns.routes.InactivityRequestWarningController
                .onPageLoadWithId(flowId, NormalMode)
                .url
            )

          val result = route(application, request).value

          val appConfig = application.injector.instanceOf[FrontendAppConfig]

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appConfig.returnsLandingPageUrl("test-cis-id", None)
        }
      }
    }
  }
}
