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

package views

import base.SpecBase
import play.api.Application
import play.api.test.FakeRequest
import play.api.test.Helpers.running
import views.html.AgentLostAccessView

class AgentLostAccessViewSpec extends SpecBase {

  "AgentLostAccessView" - {

    "must render the page with the expected content and links" in {

      val application: Application =
        applicationBuilder().build()

      running(application) {

        implicit val request   = FakeRequest()
        implicit val appConfig = applicationConfig
        implicit val msgs      = messages(application)

        val view =
          application.injector.instanceOf[AgentLostAccessView]

        val authoriseClientRequestUrl =
          "https://example.com/authorise-client"

        val html =
          view(authoriseClientRequestUrl).toString

        html must include(msgs("agent.agentLostAccess.heading"))
        html must include(msgs("agent.agentLostAccess.p1"))
        html must include(msgs("agent.agentLostAccess.h2"))

        html must include(authoriseClientRequestUrl)
        html must include(appConfig.taxAgentsAndAdvisorsAuthorisationFormsUrl)
        html must include(appConfig.clientListSearchUrl)
      }
    }
  }
}
