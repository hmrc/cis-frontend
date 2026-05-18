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
import controllers.routes
import forms.amend.WhatDoYouWantToAmendNilFormProvider
import models.ReturnType.MonthlyStandardReturn
import models.amend.WhatDoYouWantToAmendNil
import models.monthlyreturns.UpdateMonthlyReturnRequest
import models.NormalMode
import models.amend.WhatDoYouWantToAmendNil.AmendNilReturn
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhatDoYouWantToAmendNilPage
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import views.html.amend.WhatDoYouWantToAmendNilView

import java.time.LocalDate
import scala.concurrent.Future

class WhatDoYouWantToAmendNilControllerSpec extends SpecBase with MockitoSugar {

  private lazy val whatDoYouWantToAmendNilRoute =
    controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url

  private val formProvider = new WhatDoYouWantToAmendNilFormProvider()
  private val form         = formProvider()

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

  "WhatDoYouWantToAmendNil Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        completeUserAnswers
          .set(WhatDoYouWantToAmendNilPage, AmendNilReturn)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AmendNilReturn))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the SubmitInactivityRequestController page when valid data AmendNilReturn is submitted" in {

      val mockSessionRepository    = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockMonthlyReturnService.updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AmendNilReturn.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubmitInactivityRequestController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to the WhichSubcontractorsToAddController page when valid data AddPaymentOrSubcontractorDetails is submitted" in {

      val mockSessionRepository    = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.amend.routes.WhichSubcontractorsToAddController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", AmendNilReturn.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
