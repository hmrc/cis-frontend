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
import forms.monthlyreturns.EnterYourEmailAddressFormProvider
import models.NormalMode
import models.monthlyreturns.{ContractorScheme, GetAllMonthlyReturnDetailsResponse}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{DateConfirmPaymentsPage, EnterYourEmailAddressPage}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import views.html.monthlyreturns.EnterYourEmailAddressView

import java.time.LocalDate
import scala.concurrent.Future

class EnterYourEmailAddressControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  val formProvider = new EnterYourEmailAddressFormProvider()
  val form         = formProvider()

  lazy val enterYourEmailAddressRoute =
    controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(NormalMode).url

  "EnterYourEmailAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode)(request, messages(application)).toString
      }
    }

    "must call retrieveMonthlyReturnForEditDetails and prepopulate the email address for a GET in NormalMode when not already answered" in {

      val userAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 5, 5))
        .success
        .value

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      val scheme                   = ContractorScheme(
        schemeId = 1,
        instanceId = "1",
        accountsOfficeReference = "ref",
        taxOfficeNumber = "num",
        taxOfficeReference = "ref",
        emailAddress = Some("prepopulated@test.com")
      )
      when(mockMonthlyReturnService.retrieveMonthlyReturnForEditDetails(any())(any()))
        .thenReturn(Future.successful(GetAllMonthlyReturnDetailsResponse(Seq(scheme), Nil, Nil, Nil, Nil)))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("prepopulated@test.com"), NormalMode)(
          request,
          messages(application)
        ).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers = userAnswersWithCisId.set(EnterYourEmailAddressPage, "answer").success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill("answer"), NormalMode)(request, messages(application)).toString
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
          FakeRequest(POST, enterYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", "test@example.com"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[EnterYourEmailAddressView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, enterYourEmailAddressRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourEmailAddressRoute)
            .withFormUrlEncodedBody(("value", "answer"))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
