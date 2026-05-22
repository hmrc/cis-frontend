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
import forms.monthlyreturns.PaymentDetailsConfirmationFormProvider
import models.NormalMode
import models.ReturnType.{MonthlyAmendedStandardReturn, MonthlyStandardReturn}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{PaymentDetailsConfirmationPage, ReturnTypePage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.PaymentDetailsConfirmationView

import scala.concurrent.Future

class PaymentDetailsConfirmationControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new PaymentDetailsConfirmationFormProvider()
  val form         = formProvider()

  lazy val paymentDetailsConfirmationRoute = routes.PaymentDetailsConfirmationController.onPageLoad(NormalMode).url

  "PaymentDetailsConfirmation Controller" - {

    "must return OK and the correct view for a GET" in {
      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsConfirmationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentDetailsConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, true)(request, messages(application)).toString
      }
    }

    "must return OK and the amendment view for a GET when return type is amended" in {
      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsConfirmationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentDetailsConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, NormalMode, true)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCisId
        .set(PaymentDetailsConfirmationPage, true)
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsConfirmationRoute)

        val view = application.injector.instanceOf[PaymentDetailsConfirmationView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, true)(
          request,
          messages(application)
        ).toString
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
          FakeRequest(POST, paymentDetailsConfirmationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {
      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentDetailsConfirmationRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[PaymentDetailsConfirmationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, true)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsConfirmationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentDetailsConfirmationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to confirm cancel amendment page when cancelling amendment" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            routes.PaymentDetailsConfirmationController.onCancelAmendment().url
          )

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoad().url
      }
    }

    "must return OK and the non-amendment view for a GET when return type is not amended" in {
      val userAnswers = userAnswersWithCisId
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsConfirmationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentDetailsConfirmationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual
          view(form, NormalMode, false)(request, messages(application)).toString
      }
    }

    "must throw when ReturnTypePage is missing on GET" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsConfirmationRoute)

        val result = route(application, request).value

        whenReady(result.failed) { ex =>
          ex mustBe a[RuntimeException]
          ex.getMessage mustBe "ReturnTypePage not found in user answers"
        }
      }
    }
  }
}
