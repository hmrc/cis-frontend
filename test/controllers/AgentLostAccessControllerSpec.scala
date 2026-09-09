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
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.AgentLostAccessView

class AgentLostAccessControllerSpec extends SpecBase with MockitoSugar {

  "AgentLostAccessController" - {

    "onPageLoad" - {

      "must return OK and render the Agent Lost Access page when the agent code is present" in {

        val agentCode = "agentCode"

        val application: Application =
          applicationBuilder(
            userAnswers = Some(emptyUserAnswers),
            isAgent = true,
            agentCode = Some(agentCode)
          ).build()

        running(application) {

          val request = FakeRequest(GET, routes.AgentLostAccessController.onPageLoad().url)

          val result =
            application.injector
              .instanceOf[AgentLostAccessController]
              .onPageLoad(request)

          val appConfig =
            application.injector.instanceOf[config.FrontendAppConfig]

          val view =
            application.injector.instanceOf[AgentLostAccessView]

          val expectedView =
            view(appConfig.authoriseClientRequestUrl(agentCode))(
              request,
              appConfig,
              messages(application)
            )

          status(result) mustEqual OK
          contentAsString(result) mustEqual expectedView.toString
        }
      }

      "must redirect to the unauthorised agent affinity page when the agent code is missing" in {

        val application: Application =
          applicationBuilder(
            userAnswers = Some(emptyUserAnswers),
            isAgent = true,
            agentCode = None
          ).build()

        running(application) {

          val request = FakeRequest(GET, routes.AgentLostAccessController.onPageLoad().url)

          val result =
            application.injector
              .instanceOf[AgentLostAccessController]
              .onPageLoad(request)

          status(result) mustEqual SEE_OTHER

          redirectLocation(result) mustBe Some(
            routes.UnauthorisedAgentAffinityController.onPageLoad().url
          )
        }
      }
    }
  }
}
