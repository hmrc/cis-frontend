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
import forms.amend.WhichSubcontractorsToAddFormProvider
import models.{NormalMode, UserAnswers}
import models.amend.WhichSubcontractorsToAdd
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhichSubcontractorsToAddPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.amend.WhichSubcontractorsToAddView

import scala.concurrent.Future

class WhichSubcontractorsToAddControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val whichSubcontractorsToAddRoute = routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode).url

  private val subcontractors = WhichSubcontractorsToAdd.mockSubcontractors
  private val checkboxItems  = WhichSubcontractorsToAdd.checkboxItems(subcontractors)
  val formProvider           = new WhichSubcontractorsToAddFormProvider()
  val form                   = formProvider(subcontractors)

  "WhichSubcontractorsToAdd Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhichSubcontractorsToAddView]

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(form, NormalMode, checkboxItems)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val selectedIds = Set(subcontractors.head.id)

      val userAnswers = UserAnswers(userAnswersId)
        .set(WhichSubcontractorsToAddPage, selectedIds)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

        val view = application.injector.instanceOf[WhichSubcontractorsToAddView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(selectedIds), NormalMode, checkboxItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhichSubcontractorsToAddView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, checkboxItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
