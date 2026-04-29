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
import models.amend.WhatDoYouWantToAmendNil
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhatDoYouWantToAmendNilPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.amend.WhatDoYouWantToAmendNilView

import scala.concurrent.Future

class WhatDoYouWantToAmendNilControllerSpec extends SpecBase with MockitoSugar {

  private lazy val whatDoYouWantToAmendNilRoute =
    controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url

  private val formProvider = new WhatDoYouWantToAmendNilFormProvider()
  private val form         = formProvider()

  "WhatDoYouWantToAmendNil Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

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
        UserAnswers(userAnswersId).set(WhatDoYouWantToAmendNilPage, WhatDoYouWantToAmendNil.values.head).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(WhatDoYouWantToAmendNil.values.head))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the WhatDoYouWantToAmendNil page when valid data AmendNilReturn is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AmendNilReturn.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.amend.routes.WhatDoYouWantToAmendNilController
          .onPageLoad()
          .url
      }
    }

    "must redirect to the WhatDoYouWantToAmendNil page when valid data AddPaymentOrSubcontractorDetails is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.amend.routes.WhatDoYouWantToAmendNilController
          .onPageLoad()
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

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
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.values.head.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
