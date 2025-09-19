/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.SubmissionUnsuccessfulView

class SubmissionUnsuccessfulControllerSpec extends SpecBase {

  "SubmissionUnsuccessful Controller" - {

    "GET onPageLoad" - {

      "must return OK and the correct view when user answers exist" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.SubmissionUnsuccessfulController.onPageLoad.url)
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[SubmissionUnsuccessfulView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view()(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when no existing data is found" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.SubmissionUnsuccessfulController.onPageLoad.url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "POST onSubmit must redirect to IndexController (start of journey)" in {
      val app = applicationBuilder(userAnswers = None).build()
      running(app) {
        val result = route(
          app,
          FakeRequest(POST, routes.SubmissionUnsuccessfulController.onSubmit.url)
        ).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.IndexController.onPageLoad().url
      }
    }

  }
}
