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
import forms.amend.AreYouSureYouWantToAmendYesNoFormProvider
import models.ReturnType.MonthlyStandardReturn
import models.amend.AreYouSureYouWantToAmendYesNo.*
import models.amend.{AreYouSureYouWantToAmendYesNo, DeleteAllMonthlyReturnItemsRequest}
import models.monthlyreturns.UpdateMonthlyReturnRequest
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.AreYouSureYouWantToAmendYesNoPage
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import views.html.amend.AreYouSureYouWantToAmendYesNoView

import java.time.LocalDate
import scala.concurrent.Future

class AreYouSureYouWantToAmendYesNoControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val areYouSureYouWantToAmendYesNoRoute =
    routes.AreYouSureYouWantToAmendYesNoController.onPageLoad().url

  val formProvider = new AreYouSureYouWantToAmendYesNoFormProvider()
  val form         = formProvider()

  private val completeUserAnswers =
    emptyUserAnswers
      .set(CisIdPage, "cis-123")
      .success
      .value
      .set(DateConfirmPaymentsPage, LocalDate.of(2025, 1, 1))
      .success
      .value
      .set(ReturnTypePage, MonthlyStandardReturn)
      .success
      .value

  "AreYouSureYouWantToAmendYesNo Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, areYouSureYouWantToAmendYesNoRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        completeUserAnswers
          .set(AreYouSureYouWantToAmendYesNoPage, AreYouSureYouWantToAmendYesNo.values.head)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, areYouSureYouWantToAmendYesNoRoute)

        val view = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AreYouSureYouWantToAmendYesNo.values.head))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when No is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
            .withFormUrlEncodedBody(("value", No.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must delete all monthly return items, update monthly return and redirect when Yes is submitted" in {

      val mockSessionRepository     = mock[SessionRepository]
      val mockAmendMonthlyReturnSvc = mock[AmendMonthlyReturnService]
      val mockMonthlyReturnService  = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockAmendMonthlyReturnSvc.deleteAllMonthlyReturnItems(any[DeleteAllMonthlyReturnItemsRequest]())(any()))
        .thenReturn(Future.successful(()))

      when(mockMonthlyReturnService.updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnSvc),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
            .withFormUrlEncodedBody(("value", Yes.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, areYouSureYouWantToAmendYesNoRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
            .withFormUrlEncodedBody(("value", No.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
