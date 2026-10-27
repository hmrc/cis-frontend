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

package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class NilReturnStatusPageSpec extends SpecBase {

  "NilReturnStatusPage" - {

    "use the scoped JSON path for a journey" in {
      val page = NilReturnStatusPage("org_flow-1")

      page.path mustBe (JsPath \ "org_flow-1" \ "nilReturnStatus")
    }

    "use different paths for different journeys" in {
      val firstPage  = NilReturnStatusPage("org_flow-1")
      val secondPage = NilReturnStatusPage("org_flow-2")

      firstPage.path must not be secondPage.path
    }

    "retain the legacy unscoped JSON path" in {
      NilReturnStatusPage.path mustBe (JsPath \ "nilReturnStatus")
    }

    "retain the correct page name" in {
      NilReturnStatusPage("org_flow-1").toString mustBe "nil-return-status"
      NilReturnStatusPage.toString mustBe "nil-return-status"
    }
  }
}
