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

package controllers

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*

class ServiceHomeControllerSpec extends SpecBase {

  "ServiceHomeController" - {

    "must redirect an organisation user to the organisation CIS account" in {

      val application =
        applicationBuilder(
          userAnswers = None,
          isAgent = false
        ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ServiceHomeController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          applicationConfig.constructionIndustryOrgAccountUrl
      }
    }

    "must redirect an agent to the agent CIS account with the CIS id" in {

      val application =
        applicationBuilder(
          userAnswers = Some(userAnswersWithCisId),
          isAgent = true
        ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ServiceHomeController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          s"${applicationConfig.constructionIndustryAgentAccountUrl}1"
      }
    }

    "must redirect to Journey Recovery for an agent when the CIS id is unavailable" in {

      val application =
        applicationBuilder(
          userAnswers = None,
          isAgent = true
        ).build()

      running(application) {

        val request =
          FakeRequest(GET, routes.ServiceHomeController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
