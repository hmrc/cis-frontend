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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.SubmissionUnsuccessfulResubmitView

class SubmissionUnsuccessfulResubmitControllerSpec extends SpecBase {

  "ResubmissionUnsuccessful Controller" - {

    "must return OK and the correct view for a GET" in {

      val fakeCisId = "1"

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionUnsuccessfulResubmitController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionUnsuccessfulResubmitView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(fakeCisId)(request, messages(application)).toString
      }
    }

    "throw IllegalStateException when cisId is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.SubmissionUnsuccessfulResubmitController.onPageLoad().url)

        val controller = application.injector.instanceOf[SubmissionUnsuccessfulResubmitController]

        val exception = controller.onPageLoad()(request).failed.futureValue

        exception mustBe a[IllegalStateException]
        exception.getMessage mustBe "[SubmissionUnsuccessfulResubmit] cisId missing from userAnswers"
      }
    }
  }
}
