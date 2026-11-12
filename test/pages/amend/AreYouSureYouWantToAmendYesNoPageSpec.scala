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

package pages.amend

import base.SpecBase
import play.api.libs.json.JsPath

class AreYouSureYouWantToAmendYesNoPageSpec extends SpecBase {
  "have the correct path" in {
    AreYouSureYouWantToAmendYesNoPage.path mustBe (JsPath \ "areYouSureYouWantToAmendYesNo")
  }

  "have the correct toString" in {
    AreYouSureYouWantToAmendYesNoPage.toString mustBe "areYouSureYouWantToAmendYesNo"
  }

  "have the correct scoped path" in {
    AreYouSureYouWantToAmendYesNoPage("org_flow-1").path mustBe
      (JsPath \ "amend" \ "org_flow-1" \ "areYouSureYouWantToAmendYesNo")
  }

  "have the correct scoped toString" in {
    AreYouSureYouWantToAmendYesNoPage("org_flow-1").toString mustBe "areYouSureYouWantToAmendYesNo"
  }
}
