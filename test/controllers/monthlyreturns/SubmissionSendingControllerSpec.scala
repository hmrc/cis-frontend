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
import navigation.{FakeNavigator, Navigator}
import play.api.test.FakeRequest
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.Helpers.*
import play.api.libs.json._
import repositories.SessionRepository

import scala.concurrent.Future

class SubmissionSendingControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  "SubmissionSending Controller" - {

    "must return SEE_OTHER and redirect to the Submission Success page when user answer has email address as Submissionsuccessful@test.com" in {

      val data: JsObject = Json.obj(
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Submissionsuccessful@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad.url
      }
    }

    "must return SEE_OTHER and redirect to the Submission Unsuccess page when user answer has email address as Submissionunsuccessful@test.com" in {

      val data: JsObject = Json.obj(
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Submissionunsuccessful@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
      }
    }

    "must return SEE_OTHER and redirect to the Submission Awaiting page when user answer has email address as Awaitingconfirmation@test.com" in {

      val data: JsObject = Json.obj(
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Awaitingconfirmation@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad.url
      }
    }

    "must return SEE_OTHER and redirect to the Journey Recovery page when user answer has email address other than above 3 emails" in {

      val data: JsObject = Json.obj(
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
