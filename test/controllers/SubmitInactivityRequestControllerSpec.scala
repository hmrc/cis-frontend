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
import forms.monthlyreturns.SubmitInactivityRequestFormProvider
import models.{CheckMode, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, SubmitInactivityRequestPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.SubmitInactivityRequestView

import java.util.UUID
import scala.concurrent.Future

class SubmitInactivityRequestControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new SubmitInactivityRequestFormProvider()
  val form         = formProvider()

  lazy val submitInactivityRequestRoute =
    controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode).url

  private def scopedUserAnswers(flowId: UUID): UserAnswers =
    emptyUserAnswers
      .setOrException(CisIdPage(flowId.toString), "test-cis-id")

  "SubmitInactivityRequest Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, submitInactivityRequestRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmitInactivityRequestView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCisId.set(SubmitInactivityRequestPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, submitInactivityRequestRoute)

        val view = application.injector.instanceOf[SubmitInactivityRequestView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, submitInactivityRequestRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request =
          FakeRequest(POST, submitInactivityRequestRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[SubmitInactivityRequestView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, submitInactivityRequestRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, submitInactivityRequestRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "UUID-scoped journey" - {

      "must return OK and the correct view for a GET" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmitInactivityRequestView]

          val formAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view correctly on a GET when the scoped question has previously been answered" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = scopedUserAnswers(flowId)
          .setOrException(SubmitInactivityRequestPage(id), true)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
          )

          val view = application.injector.instanceOf[SubmitInactivityRequestView]

          val result = route(application, request).value

          val formAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(true), NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK and the correct view for a GET in CheckMode" in {

        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        val userAnswers = scopedUserAnswers(flowId)
          .setOrException(SubmitInactivityRequestPage(id), false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, CheckMode).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmitInactivityRequestView]

          val formAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, CheckMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(false), CheckMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must not use the legacy answer when the scoped question has not been answered" in {

        val flowId = UUID.randomUUID()

        val userAnswers = userAnswersWithCisId
          .setOrException(SubmitInactivityRequestPage, true)
          .setOrException(CisIdPage(flowId.toString), "test-cis-id")

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmitInactivityRequestView]

          val formAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must populate the view using the answer from the correct UUID-scoped journey" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()

        val userAnswers = scopedUserAnswers(firstFlowId)
          .setOrException(SubmitInactivityRequestPage(firstFlowId.toString), true)
          .setOrException(CisIdPage(secondFlowId.toString), "second-cis-id")
          .setOrException(SubmitInactivityRequestPage(secondFlowId.toString), false)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val firstRequest = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController
              .onPageLoadWithId(firstFlowId, NormalMode)
              .url
          )

          val secondRequest = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController
              .onPageLoadWithId(secondFlowId, NormalMode)
              .url
          )

          val firstResult  = route(application, firstRequest).value
          val secondResult = route(application, secondRequest).value

          val view = application.injector.instanceOf[SubmitInactivityRequestView]

          val firstFormAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(firstFlowId, NormalMode)
          )

          val secondFormAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(secondFlowId, NormalMode)
          )

          status(firstResult) mustEqual OK
          contentAsString(firstResult) mustEqual view(form.fill(true), NormalMode, firstFormAction)(
            firstRequest,
            messages(application)
          ).toString

          status(secondResult) mustEqual OK
          contentAsString(secondResult) mustEqual view(form.fill(false), NormalMode, secondFormAction)(
            secondRequest,
            messages(application)
          ).toString
        }
      }

      "must save the scoped answer and redirect to Inactivity Request Warning when Yes is submitted" in {

        val flowId      = UUID.randomUUID()
        val id          = flowId.toString
        val userAnswers = scopedUserAnswers(flowId)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.InactivityRequestWarningController
              .onPageLoadWithId(flowId, NormalMode)
              .url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          captor.getValue.get(SubmitInactivityRequestPage(id)) mustEqual Some(true)
          captor.getValue.get(SubmitInactivityRequestPage) mustEqual None
        }
      }

      "must save the scoped answer and redirect to Confirmation By Email when No is submitted" in {

        val flowId      = UUID.randomUUID()
        val id          = flowId.toString
        val userAnswers = scopedUserAnswers(flowId)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(flowId, NormalMode).url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          captor.getValue.get(SubmitInactivityRequestPage(id)) mustEqual Some(false)
          captor.getValue.get(SubmitInactivityRequestPage) mustEqual None
        }
      }

      "must redirect to Inactivity Request Warning when Yes is submitted in CheckMode" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, CheckMode).url
            )
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoadWithId(flowId, CheckMode).url
        }
      }

      "must redirect to Check Your Answers when No is submitted in CheckMode" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, CheckMode).url
            )
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url
        }
      }

      "must save the answer for the correct UUID without changing another journey" in {

        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()
        val firstId      = firstFlowId.toString
        val secondId     = secondFlowId.toString

        val userAnswers = scopedUserAnswers(firstFlowId)
          .setOrException(CisIdPage(secondId), "second-cis-id")
          .setOrException(SubmitInactivityRequestPage(secondId), true)

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(userAnswers))
            .configure("features.multi-tab-enabled" -> true)
            .overrides(
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController
                .onSubmitWithId(firstFlowId, NormalMode)
                .url
            )
              .withFormUrlEncodedBody(("value", "false"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            controllers.monthlyreturns.routes.ConfirmationByEmailController
              .onPageLoadWithId(firstFlowId, NormalMode)
              .url

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(mockSessionRepository).set(captor.capture())

          val savedAnswers = captor.getValue

          savedAnswers.get(SubmitInactivityRequestPage(firstId)) mustEqual Some(false)
          savedAnswers.get(SubmitInactivityRequestPage(secondId)) mustEqual Some(true)
          savedAnswers.get(CisIdPage(secondId)) mustEqual Some("second-cis-id")
        }
      }

      "must return a Bad Request and errors when invalid scoped data is submitted" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", ""))

          val boundForm = form.bind(Map("value" -> ""))

          val view = application.injector.instanceOf[SubmitInactivityRequestView]

          val result = route(application, request).value

          val formAction = Some(
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode)
          )

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, NormalMode, formAction)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = None)
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery for a POST if no existing data is found" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = None)
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request =
            FakeRequest(
              POST,
              controllers.monthlyreturns.routes.SubmitInactivityRequestController.onSubmitWithId(flowId, NormalMode).url
            )
              .withFormUrlEncodedBody(("value", "true"))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when the scoped CisIdPage is missing" in {

        val flowId = UUID.randomUUID()

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
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

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the returns landing page when multi-tab is disabled" in {

        val flowId      = UUID.randomUUID()
        val userAnswers = scopedUserAnswers(flowId)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> false)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode).url
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
