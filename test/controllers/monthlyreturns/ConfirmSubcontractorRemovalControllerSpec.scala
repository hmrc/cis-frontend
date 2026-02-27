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
import controllers.routes
import forms.monthlyreturns.ConfirmSubcontractorRemovalFormProvider
import models.NormalMode
import models.monthlyreturns.SelectedSubcontractor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.ConfirmSubcontractorRemovalView

import scala.concurrent.Future

class ConfirmSubcontractorRemovalControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider      = new ConfirmSubcontractorRemovalFormProvider()
  val form              = formProvider()
  val subcontractorName = "TyneWear Ltd"
  val subcontractor     = SelectedSubcontractor(123L, subcontractorName, None, None, None)

  val userAnswersWithSubcontractor =
    emptyUserAnswers.set(SelectedSubcontractorPage(1), subcontractor).success.value

  lazy val confirmSubcontractorRemovalRoute =
    controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController.onPageLoad(1).url

  "ConfirmSubcontractorRemoval Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSubcontractor)).build()

      running(application) {
        val request = FakeRequest(GET, confirmSubcontractorRemovalRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmSubcontractorRemovalView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, 1, subcontractorName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET when no subcontractor exists at index" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, confirmSubcontractorRemovalRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted with true" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithSubcontractor))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmSubcontractorRemovalRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to the next page when valid data is submitted with false" in {

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithSubcontractor))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, confirmSubcontractorRemovalRoute)
            .withFormUrlEncodedBody(("value", "false"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSubcontractor)).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmSubcontractorRemovalRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[ConfirmSubcontractorRemovalView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, 1, subcontractorName)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a POST when no subcontractor exists at index" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmSubcontractorRemovalRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, confirmSubcontractorRemovalRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, confirmSubcontractorRemovalRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
