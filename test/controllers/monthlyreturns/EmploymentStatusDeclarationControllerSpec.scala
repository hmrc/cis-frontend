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
import forms.monthlyreturns.EmploymentStatusDeclarationFormProvider
import models.{CheckMode, NormalMode, UserAnswers}
import models.ReturnType.MonthlyStandardReturn
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.EmploymentStatusDeclarationView

import java.util.UUID
import scala.concurrent.Future

class EmploymentStatusDeclarationControllerSpec extends SpecBase with MockitoSugar {

  private val flowId       = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  private val subJourneyId = flowId.toString

  private val userAnswersWithId: UserAnswers = emptyUserAnswers
    .setOrException(CisIdPage(subJourneyId), "i-123")
    .setOrException(ReturnTypePage(subJourneyId), MonthlyStandardReturn)

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new EmploymentStatusDeclarationFormProvider()
  val form         = formProvider()

  lazy val employmentStatusDeclarationRoute =
    controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onPageLoad(NormalMode).url

  lazy val employmentStatusDeclarationRouteWithId =
    controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onPageLoadWithId(flowId, NormalMode).url

  "EmploymentStatusDeclaration Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, employmentStatusDeclarationRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmploymentStatusDeclarationView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCisId.set(EmploymentStatusDeclarationPage, true).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, employmentStatusDeclarationRoute)

        val view = application.injector.instanceOf[EmploymentStatusDeclarationView]

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
          FakeRequest(POST, employmentStatusDeclarationRoute)
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
          FakeRequest(POST, employmentStatusDeclarationRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EmploymentStatusDeclarationView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, employmentStatusDeclarationRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, employmentStatusDeclarationRoute)
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return OK and the correct view for a GET with ID" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithId))
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request = FakeRequest(GET, employmentStatusDeclarationRouteWithId)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmploymentStatusDeclarationView]

        val formAction = Some(
          controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onSubmitWithId(flowId, NormalMode)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, formAction)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET with ID when the question has previously been answered" in {

      val userAnswers = userAnswersWithId
        .setOrException(EmploymentStatusDeclarationPage, false)
        .setOrException(EmploymentStatusDeclarationPage(subJourneyId), true)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request = FakeRequest(GET, employmentStatusDeclarationRouteWithId)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EmploymentStatusDeclarationView]

        val formAction = Some(
          controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onSubmitWithId(flowId, NormalMode)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(true), NormalMode, formAction)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next scoped page when valid data is submitted with ID" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val userAnswers = userAnswersWithId
        .setOrException(EmploymentStatusDeclarationPage, false)

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
            controllers.monthlyreturns.routes.EmploymentStatusDeclarationController
              .onSubmitWithId(flowId, NormalMode)
              .url
          )
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.VerifiedStatusDeclarationController.onPageLoadWithId(flowId, NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(EmploymentStatusDeclarationPage(subJourneyId)) mustBe Some(true)
        captor.getValue.get(EmploymentStatusDeclarationPage) mustBe Some(false)
      }
    }

    "must redirect to scoped Check Your Answers when valid data is submitted in CheckMode with ID" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithId))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.monthlyreturns.routes.EmploymentStatusDeclarationController
              .onSubmitWithId(flowId, CheckMode)
              .url
          )
            .withFormUrlEncodedBody(("value", "true"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(EmploymentStatusDeclarationPage(subJourneyId)) mustBe Some(true)
      }
    }

    "must return a Bad Request and errors when invalid data is submitted with ID" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithId))
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.monthlyreturns.routes.EmploymentStatusDeclarationController
              .onSubmitWithId(flowId, NormalMode)
              .url
          )
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EmploymentStatusDeclarationView]

        val formAction = Some(
          controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onSubmitWithId(flowId, NormalMode)
        )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, formAction)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
