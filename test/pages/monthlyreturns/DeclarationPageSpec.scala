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

class DeclarationPageSpec extends SpecBase {

  "DeclarationPage" - {

    "use the scoped JSON path for a journey" in {
      val page = DeclarationPage("org_flow-1")

      page.path mustBe (JsPath \ "org_flow-1" \ "declaration")
    }

    "use different paths for different journeys" in {
      val firstPage  = DeclarationPage("org_flow-1")
      val secondPage = DeclarationPage("org_flow-2")

      firstPage.path must not be secondPage.path
    }

    "retain the legacy unscoped JSON path" in {
      DeclarationPage.path mustBe (JsPath \ "declaration")
    }

    "retain the correct page name" in {
      DeclarationPage("org_flow-1").toString mustBe "declaration"
      DeclarationPage.toString mustBe "declaration"
    }
  }
}
