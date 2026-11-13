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

class ConfirmationByEmailPageSpec extends SpecBase {

  "ConfirmationByEmailPage" - {

    "have the correct path" in {
      ConfirmationByEmailPage.path mustBe (JsPath \ "confirmationByEmail")
    }

    "have the correct toString" in {
      ConfirmationByEmailPage.toString mustBe "confirmationByEmail"
    }

    "cleanup" - {

      "must remove EnterYourEmailAddressPage when answer is No" in {
        val userAnswers = emptyUserAnswers
          .setOrException(EnterYourEmailAddressPage, "test@example.com")

        val result = ConfirmationByEmailPage.cleanup(Some(false), userAnswers).success.value

        result.get(EnterYourEmailAddressPage) mustBe None
      }

      "must remove EnterYourEmailAddressPage when answer is removed" in {
        val userAnswers = emptyUserAnswers
          .setOrException(EnterYourEmailAddressPage, "test@example.com")

        val result = ConfirmationByEmailPage.cleanup(None, userAnswers).success.value

        result.get(EnterYourEmailAddressPage) mustBe None
      }

      "must retain EnterYourEmailAddressPage when answer is Yes" in {
        val userAnswers = emptyUserAnswers
          .setOrException(EnterYourEmailAddressPage, "test@example.com")

        val result = ConfirmationByEmailPage.cleanup(Some(true), userAnswers).success.value

        result.get(EnterYourEmailAddressPage) mustBe Some("test@example.com")
      }
    }

    "UUID-scoped journey" - {

      "have the correct path under monthlyReturn" in {
        val id = "test-flow-id"

        ConfirmationByEmailPage(id).path mustBe
          (JsPath \ "monthlyReturn" \ id \ "confirmationByEmail")
      }

      "have the correct toString" in {
        val id = "test-flow-id"

        ConfirmationByEmailPage(id).toString mustBe "confirmationByEmail"
      }

      "must use different paths for different flowIds" in {
        val firstId  = "first-flow-id"
        val secondId = "second-flow-id"

        ConfirmationByEmailPage(firstId).path mustBe
          (JsPath \ "monthlyReturn" \ firstId \ "confirmationByEmail")

        ConfirmationByEmailPage(secondId).path mustBe
          (JsPath \ "monthlyReturn" \ secondId \ "confirmationByEmail")
      }

      "must store and retrieve the answer for the correct flowId" in {
        val firstId  = "first-flow-id"
        val secondId = "second-flow-id"

        val userAnswers = emptyUserAnswers
          .setOrException(ConfirmationByEmailPage(firstId), true)
          .setOrException(ConfirmationByEmailPage(secondId), false)

        userAnswers.get(ConfirmationByEmailPage(firstId)) mustBe Some(true)
        userAnswers.get(ConfirmationByEmailPage(secondId)) mustBe Some(false)
      }

      "must not use the legacy answer when the scoped answer is missing" in {
        val id = "test-flow-id"

        val userAnswers = emptyUserAnswers
          .setOrException(ConfirmationByEmailPage, true)

        userAnswers.get(ConfirmationByEmailPage(id)) mustBe None
        userAnswers.get(ConfirmationByEmailPage) mustBe Some(true)
      }

      "cleanup" - {

        "must remove the scoped EnterYourEmailAddressPage when answer is No" in {
          val id = "test-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(id), "test@example.com")

          val result = ConfirmationByEmailPage(id).cleanup(Some(false), userAnswers).success.value

          result.get(EnterYourEmailAddressPage(id)) mustBe None
        }

        "must remove the scoped EnterYourEmailAddressPage when answer is removed" in {
          val id = "test-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(id), "test@example.com")

          val result = ConfirmationByEmailPage(id).cleanup(None, userAnswers).success.value

          result.get(EnterYourEmailAddressPage(id)) mustBe None
        }

        "must retain the scoped EnterYourEmailAddressPage when answer is Yes" in {
          val id = "test-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(id), "test@example.com")

          val result = ConfirmationByEmailPage(id).cleanup(Some(true), userAnswers).success.value

          result.get(EnterYourEmailAddressPage(id)) mustBe Some("test@example.com")
        }

        "must not remove another journey email address when answer is No" in {
          val firstId  = "first-flow-id"
          val secondId = "second-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(firstId), "first@example.com")
            .setOrException(EnterYourEmailAddressPage(secondId), "second@example.com")

          val result = ConfirmationByEmailPage(firstId).cleanup(Some(false), userAnswers).success.value

          result.get(EnterYourEmailAddressPage(firstId)) mustBe None
          result.get(EnterYourEmailAddressPage(secondId)) mustBe Some("second@example.com")
        }

        "must not remove another journey email address when answer is removed" in {
          val firstId  = "first-flow-id"
          val secondId = "second-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(firstId), "first@example.com")
            .setOrException(EnterYourEmailAddressPage(secondId), "second@example.com")

          val result = ConfirmationByEmailPage(firstId).cleanup(None, userAnswers).success.value

          result.get(EnterYourEmailAddressPage(firstId)) mustBe None
          result.get(EnterYourEmailAddressPage(secondId)) mustBe Some("second@example.com")
        }

        "must not remove the legacy email address when the scoped answer is No" in {
          val id = "test-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage, "legacy@example.com")
            .setOrException(EnterYourEmailAddressPage(id), "scoped@example.com")

          val result = ConfirmationByEmailPage(id).cleanup(Some(false), userAnswers).success.value

          result.get(EnterYourEmailAddressPage(id)) mustBe None
          result.get(EnterYourEmailAddressPage) mustBe Some("legacy@example.com")
        }

        "must remove the scoped email address when ConfirmationByEmailPage is set to No" in {
          val id = "test-flow-id"

          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(id), "test@example.com")
            .setOrException(ConfirmationByEmailPage(id), false)

          userAnswers.get(ConfirmationByEmailPage(id)) mustBe Some(false)
          userAnswers.get(EnterYourEmailAddressPage(id)) mustBe None
        }
      }
    }
  }
}
