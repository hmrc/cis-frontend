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

    "use correct ConfirmationByEmailPage path" in {
      ConfirmationByEmailPage.path mustBe (JsPath \ "confirmationByEmail")
      ConfirmationByEmailPage.toString mustBe "confirmationByEmail"
    }

    "use correct ConfirmationByEmailPage path with id" in {
      val testId = "testId"
      ConfirmationByEmailPage(testId).path mustBe (JsPath \ "monthlyReturn" \ testId \ "confirmationByEmail")
      ConfirmationByEmailPage(testId).toString mustBe "confirmationByEmail"
    }

    "cleanup" - {
      "without id" - {
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

      "with id" - {
        val testId = "testId"

        "must remove EnterYourEmailAddressPage when answer is No" in {
          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(testId), "test@example.com")

          val result = ConfirmationByEmailPage(testId).cleanup(Some(false), userAnswers).success.value

          result.get(EnterYourEmailAddressPage(testId)) mustBe None
        }

        "must remove EnterYourEmailAddressPage when answer is removed" in {
          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(testId), "test@example.com")

          val result = ConfirmationByEmailPage(testId).cleanup(None, userAnswers).success.value

          result.get(EnterYourEmailAddressPage(testId)) mustBe None
        }

        "must retain EnterYourEmailAddressPage when answer is Yes" in {
          val userAnswers = emptyUserAnswers
            .setOrException(EnterYourEmailAddressPage(testId), "test@example.com")

          val result = ConfirmationByEmailPage(testId).cleanup(Some(true), userAnswers).success.value

          result.get(EnterYourEmailAddressPage(testId)) mustBe Some("test@example.com")
        }
      }
    }
  }
}
