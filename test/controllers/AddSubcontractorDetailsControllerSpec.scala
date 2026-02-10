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

package controllers

import base.SpecBase
import forms.monthlyreturns.AddSubcontractorDetailsFormProvider
import models.monthlyreturns.AddSubcontractorDetails
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.AddSubcontractorDetailsPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.monthlyreturns.AddSubcontractorDetailsView

import scala.concurrent.Future

class AddSubcontractorDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val addSubcontractorDetailsRoute =
    controllers.monthlyreturns.routes.AddSubcontractorDetailsController.onPageLoad(NormalMode).url

  val formProvider = new AddSubcontractorDetailsFormProvider()
  val form         = formProvider()

  private val subcontractorsWithDetails: Seq[String] =
    Seq("BuildRight Construction")

  private val subcontractorsWithoutDetails: Seq[String] =
    Seq("Northern Trades Ltd", "TyneWear Ltd")

  "AddSubcontractorDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addSubcontractorDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddSubcontractorDetailsView]

        status(result) mustEqual OK
        val content = contentAsString(result)

        content mustEqual view(form, NormalMode, subcontractorsWithDetails, subcontractorsWithoutDetails)(
          request,
          messages(application)
        ).toString

        content must include("BuildRight Construction")
        content must include("Northern Trades Ltd")
        content must include("TyneWear Ltd")
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        UserAnswers(userAnswersId).set(AddSubcontractorDetailsPage, AddSubcontractorDetails.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, addSubcontractorDetailsRoute)

        val view = application.injector.instanceOf[AddSubcontractorDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(AddSubcontractorDetails.values.head),
          NormalMode,
          subcontractorsWithDetails,
          subcontractorsWithoutDetails
        )(
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
          FakeRequest(POST, addSubcontractorDetailsRoute)
            .withFormUrlEncodedBody(("value", AddSubcontractorDetails.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, addSubcontractorDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[AddSubcontractorDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          subcontractorsWithDetails,
          subcontractorsWithoutDetails
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addSubcontractorDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addSubcontractorDetailsRoute)
            .withFormUrlEncodedBody(("value", AddSubcontractorDetails.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
