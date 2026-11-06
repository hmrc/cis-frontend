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

class EnterYourEmailAddressPageSpec extends SpecBase {

  "EnterYourEmailAddressPage" - {

    "have the correct path" in {
      EnterYourEmailAddressPage.path mustBe (JsPath \ "enterYourEmailAddress")
    }

    "have the correct toString" in {
      EnterYourEmailAddressPage.toString mustBe "enterYourEmailAddress"
    }

    "UUID-scoped journey" - {

      "have the correct path under monthlyReturn" in {
        val id = "test-flow-id"

        EnterYourEmailAddressPage(id).path mustBe
          (JsPath \ "monthlyReturn" \ id \ "enterYourEmailAddress")
      }

      "have the correct toString" in {
        val id = "test-flow-id"

        EnterYourEmailAddressPage(id).toString mustBe "enterYourEmailAddress"
      }

      "must use different paths for different flowIds" in {
        val firstId  = "first-flow-id"
        val secondId = "second-flow-id"

        EnterYourEmailAddressPage(firstId).path mustBe
          (JsPath \ "monthlyReturn" \ firstId \ "enterYourEmailAddress")

        EnterYourEmailAddressPage(secondId).path mustBe
          (JsPath \ "monthlyReturn" \ secondId \ "enterYourEmailAddress")
      }

      "must store and retrieve the email address for the correct flowId" in {
        val firstId  = "first-flow-id"
        val secondId = "second-flow-id"

        val userAnswers = emptyUserAnswers
          .setOrException(EnterYourEmailAddressPage(firstId), "first@example.com")
          .setOrException(EnterYourEmailAddressPage(secondId), "second@example.com")

        userAnswers.get(EnterYourEmailAddressPage(firstId)) mustBe Some("first@example.com")
        userAnswers.get(EnterYourEmailAddressPage(secondId)) mustBe Some("second@example.com")
      }

      "must not use the legacy email address when the scoped answer is missing" in {
        val id = "test-flow-id"

        val userAnswers = emptyUserAnswers
          .setOrException(EnterYourEmailAddressPage, "legacy@example.com")

        userAnswers.get(EnterYourEmailAddressPage(id)) mustBe None
        userAnswers.get(EnterYourEmailAddressPage) mustBe Some("legacy@example.com")
      }

      "must not overwrite the legacy email address when setting a scoped answer" in {
        val id = "test-flow-id"

        val userAnswers = emptyUserAnswers
          .setOrException(EnterYourEmailAddressPage, "legacy@example.com")
          .setOrException(EnterYourEmailAddressPage(id), "scoped@example.com")

        userAnswers.get(EnterYourEmailAddressPage) mustBe Some("legacy@example.com")
        userAnswers.get(EnterYourEmailAddressPage(id)) mustBe Some("scoped@example.com")
      }
    }
  }
}
