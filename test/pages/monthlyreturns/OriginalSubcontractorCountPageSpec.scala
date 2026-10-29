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

class OriginalSubcontractorCountPageSpec extends SpecBase {

  "OriginalSubcontractorCountPage" - {
    "have the correct path" in {
      OriginalSubcontractorCountPage.path mustBe (JsPath \ "originalSubcontractorCount")
    }

    "have the correct toString" in {
      OriginalSubcontractorCountPage.toString mustBe "originalSubcontractorCount"
    }

    "have the correct scoped path" in {
      OriginalSubcontractorCountPage("org_flow-1").path mustBe
        (JsPath \ "org_flow-1" \ "originalSubcontractorCount")
    }

    "have the correct scoped toString" in {
      OriginalSubcontractorCountPage("org_flow-1").toString mustBe "originalSubcontractorCount"
    }
  }
}
