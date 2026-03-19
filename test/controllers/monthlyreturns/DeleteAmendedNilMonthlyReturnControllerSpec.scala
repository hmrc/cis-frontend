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
import forms.monthlyreturns.DeleteAmendedNilMonthlyReturnFormProvider
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{DateConfirmPaymentsPage, DeleteAmendedNilMonthlyReturnPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.DeleteAmendedNilMonthlyReturnView

import scala.concurrent.Future
import java.time.LocalDate

class DeleteAmendedNilMonthlyReturnControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider        = new DeleteAmendedNilMonthlyReturnFormProvider()
  val form: Form[Boolean] = formProvider()

  val periodEndDate: LocalDate = LocalDate.of(2026, 3, 31)
  val periodEnd                = "March 2026"

  val baseUa: UserAnswers =
    emptyUserAnswers
      .set(DateConfirmPaymentsPage, periodEndDate)
      .success
      .value

  lazy val deleteAmendedNilMonthlyReturnRoute: String =
    controllers.monthlyreturns.routes.DeleteAmendedNilMonthlyReturnController.onPageLoad(NormalMode).url

  "DeleteAmendedNilMonthlyReturn Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(baseUa)).build()

      running(application) {
        val request = FakeRequest(GET, deleteAmendedNilMonthlyReturnRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[DeleteAmendedNilMonthlyReturnView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, periodEnd, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        baseUa.set(DeleteAmendedNilMonthlyReturnPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, deleteAmendedNilMonthlyReturnRoute)

        val view = application.injector.instanceOf[DeleteAmendedNilMonthlyReturnView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), periodEnd, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(baseUa))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAmendedNilMonthlyReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(baseUa)).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAmendedNilMonthlyReturnRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[DeleteAmendedNilMonthlyReturnView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, periodEnd, NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, deleteAmendedNilMonthlyReturnRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, deleteAmendedNilMonthlyReturnRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
