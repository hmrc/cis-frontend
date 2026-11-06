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

package pages.agent

import base.SpecBase
import play.api.libs.json.JsPath

class AgentClientDataPageSpec extends SpecBase {

  "AgentClientDataPage" - {

    "have the correct path" in {
      AgentClientDataPage.path mustBe (JsPath \ "agent")
    }

    "have the correct toString" in {
      AgentClientDataPage.toString mustBe "agentClientData"
    }

    "have the correct scoped path" in {
      AgentClientDataPage("org_flow-1").path mustBe
        (JsPath \ "monthlyReturn" \ "org_flow-1" \ "agent")
    }

    "use different paths for different journeys" in {
      val firstPage  = AgentClientDataPage("org_flow-1")
      val secondPage = AgentClientDataPage("org_flow-2")

      firstPage.path must not be secondPage.path
    }

    "have the correct scoped toString" in {
      AgentClientDataPage("org_flow-1").toString mustBe "agentClientData"
    }
  }
}
