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
import forms.monthlyreturns.CostOfMaterialsFormProvider
import models.monthlyreturns.SelectedSubcontractor
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{SelectedSubcontractorMaterialCostsPage, SelectedSubcontractorPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.CostOfMaterialsView

import scala.concurrent.Future

class CostOfMaterialsControllerSpec extends SpecBase with MockitoSugar {

  val formProvider                   = new CostOfMaterialsFormProvider()
  val form: Form[Option[BigDecimal]] = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  val validAnswer: BigDecimal = BigDecimal(0)
  val companyName             = "TyneWear Ltd"

  val userAnswers: UserAnswers = emptyUserAnswers
    .set(SelectedSubcontractorPage(1), SelectedSubcontractor(123, companyName, None, None, None))
    .success
    .value

  lazy val costOfMaterialsRoute: String =
    controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoad(NormalMode, 1, None).url

  "CostOfMaterials Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, costOfMaterialsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CostOfMaterialsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, companyName, 1, None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val updatedAnswers = userAnswers.set(SelectedSubcontractorMaterialCostsPage(1), validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, costOfMaterialsRoute)

        val view = application.injector.instanceOf[CostOfMaterialsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(Some(validAnswer)), NormalMode, companyName, 1, None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, costOfMaterialsRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when empty data is submitted (optional) and save a default value of 0" in {

      val existingAnswerUa =
        userAnswers.set(SelectedSubcontractorMaterialCostsPage(1), BigDecimal("123.00")).success.value

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(existingAnswerUa))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, costOfMaterialsRoute)
            .withFormUrlEncodedBody(("value", "")) // blank allowed

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[models.UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(SelectedSubcontractorMaterialCostsPage(1)) mustBe Some(BigDecimal(0))
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, costOfMaterialsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[CostOfMaterialsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, companyName, 1, None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, costOfMaterialsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no subcontractor is found for the index" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, costOfMaterialsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, costOfMaterialsRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no subcontractor is found for the index" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, costOfMaterialsRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Change Answers total payments when returnTo is changeAnswers" in {

      val changeAnswersPostRoute = s"/monthly-return/materials-cost/1?returnTo=changeAnswers"

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeAnswersPostRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController.onPageLoad(1).url
      }
    }

    "must redirect to Change Answers total payments when returnTo is changeAnswers and empty data is submitted (optional) and save a default value of 0" in {

      val existingAnswerUa =
        userAnswers.set(SelectedSubcontractorMaterialCostsPage(1), BigDecimal("123")).success.value

      val changeAnswersPostRoute = s"/monthly-return/materials-cost/1?returnTo=changeAnswers"

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(existingAnswerUa))
          .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, changeAnswersPostRoute)
            .withFormUrlEncodedBody(("value", "")) // blank allowed

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController.onPageLoad(1).url

        val captor = ArgumentCaptor.forClass(classOf[models.UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(SelectedSubcontractorMaterialCostsPage(1)) mustBe Some(BigDecimal(0))
      }
    }
  }
}
