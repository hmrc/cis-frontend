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

class CisIdPageSpec extends SpecBase {

  "CisIdPage" - {

    "have the correct path" in {
      CisIdPage.path mustBe (JsPath \ "cisId")
    }

    "have the correct toString" in {
      CisIdPage.toString mustBe "cisId"
    }

    "have the correct scoped path" in {
      CisIdPage("org_flow-1").path mustBe
        (JsPath \ "org_flow-1" \ "cisId")
    }

    "have the correct scoped toString" in {
      CisIdPage("org_flow-1").toString mustBe "cisId"
    }
  }
}
