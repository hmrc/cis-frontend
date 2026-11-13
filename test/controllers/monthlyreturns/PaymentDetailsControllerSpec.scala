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
import forms.PaymentDetailsFormProvider
import models.monthlyreturns.SelectedSubcontractor
import models.{NormalMode, UserAnswers}
import models.ReturnType.MonthlyStandardReturn
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.monthlyreturns.PaymentDetailsView

import java.util.UUID
import scala.concurrent.Future

class PaymentDetailsControllerSpec extends SpecBase with MockitoSugar {

  private val flowId       = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
  private val subJourneyId = flowId.toString

  val formProvider           = new PaymentDetailsFormProvider()
  val form: Form[BigDecimal] = formProvider()

  def onwardRoute: Call = Call("GET", "/foo")

  val companyName             = "TyneWear Ltd"
  val validAnswer: BigDecimal = BigDecimal(0)

  val userAnswers: UserAnswers = userAnswersWithCisId
    .set(SelectedSubcontractorPage(1), SelectedSubcontractor(123, companyName, None, None, None))
    .success
    .value

  val userAnswersWithId: UserAnswers = userAnswers
    .setOrException(CisIdPage(subJourneyId), "i-123")
    .setOrException(ReturnTypePage(subJourneyId), MonthlyStandardReturn)
    .setOrException(
      SelectedSubcontractorPage(1, Some(subJourneyId)),
      SelectedSubcontractor(123, companyName, None, None, None)
    )

  lazy val paymentDetailsRoute: String =
    controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(NormalMode, 1, None).url

  lazy val paymentDetailsRouteWithId: String =
    controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoadWithId(flowId, NormalMode, 1, None).url

  "PaymentDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentDetailsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, companyName, 1, None)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val updatedAnswers =
        userAnswers.set(SelectedSubcontractorPaymentsMadePage(1), validAnswer).success.value

      val application = applicationBuilder(userAnswers = Some(updatedAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsRoute)

        val view = application.injector.instanceOf[PaymentDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, companyName, 1, None)(
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
          FakeRequest(POST, paymentDetailsRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PaymentDetailsView]

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
        val request = FakeRequest(GET, paymentDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET if no subcontractor is found for the index" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentDetailsRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no subcontractor is found for the index" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request =
          FakeRequest(POST, paymentDetailsRoute)
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Change Answers total payments when returnTo is changeAnswers" in {

      val changeAnswersPostRoute = s"/monthly-return/payment-details/1?returnTo=changeAnswers"

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

    "must populate the view correctly on a GET with ID when the question has previously been answered" in {

      val updatedAnswers = userAnswersWithId
        .setOrException(SelectedSubcontractorPaymentsMadePage(1), BigDecimal("999"))
        .setOrException(SelectedSubcontractorPaymentsMadePage(1, Some(subJourneyId)), validAnswer)

      val application = applicationBuilder(userAnswers = Some(updatedAnswers))
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsRouteWithId)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PaymentDetailsView]

        val formAction = Some(
          controllers.monthlyreturns.routes.PaymentDetailsController.onSubmitWithId(flowId, NormalMode, 1, None)
        )

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(validAnswer), NormalMode, companyName, 1, None, formAction)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET with ID if no scoped subcontractor is found" in {

      val ua = userAnswers
        .setOrException(CisIdPage(subJourneyId), "i-123")
        .setOrException(ReturnTypePage(subJourneyId), MonthlyStandardReturn)

      val application = applicationBuilder(userAnswers = Some(ua))
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request = FakeRequest(GET, paymentDetailsRouteWithId)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next scoped page when valid data is submitted with ID" in {

      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val updatedAnswers = userAnswersWithId
        .setOrException(SelectedSubcontractorPaymentsMadePage(1), BigDecimal("999"))

      val application =
        applicationBuilder(userAnswers = Some(updatedAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.monthlyreturns.routes.PaymentDetailsController
              .onSubmitWithId(flowId, NormalMode, 1, None)
              .url
          )
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.CostOfMaterialsController
            .onPageLoadWithId(flowId, NormalMode, 1, None)
            .url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(SelectedSubcontractorPaymentsMadePage(1, Some(subJourneyId))) mustBe Some(validAnswer)
        captor.getValue.get(SelectedSubcontractorPaymentsMadePage(1)) mustBe Some(BigDecimal("999"))
      }
    }

    "must redirect to scoped Change Answers total payments when returnTo is changeAnswers" in {

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
            controllers.monthlyreturns.routes.PaymentDetailsController
              .onSubmitWithId(flowId, NormalMode, 1, Some("changeAnswers"))
              .url
          )
            .withFormUrlEncodedBody(("value", validAnswer.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController.onPageLoadWithId(flowId, 1).url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(SelectedSubcontractorPaymentsMadePage(1, Some(subJourneyId))) mustBe Some(validAnswer)
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
            controllers.monthlyreturns.routes.PaymentDetailsController
              .onSubmitWithId(flowId, NormalMode, 1, None)
              .url
          )
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[PaymentDetailsView]

        val formAction = Some(
          controllers.monthlyreturns.routes.PaymentDetailsController.onSubmitWithId(flowId, NormalMode, 1, None)
        )

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, companyName, 1, None, formAction)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
