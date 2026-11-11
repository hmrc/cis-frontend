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

class PaymentDetailsConfirmationPageSpec extends SpecBase {

  "PaymentDetailsConfirmationPage" - {
    "have use the correct PaymentDetailsConfirmationPage path" in {
      PaymentDetailsConfirmationPage.path mustBe (JsPath \ "paymentDetailsConfirmation")
      PaymentDetailsConfirmationPage.toString mustBe "paymentDetailsConfirmation"
    }

    "have use the correct PaymentDetailsConfirmationPage path with id" in {
      val testId = "testId"
      PaymentDetailsConfirmationPage(
        testId
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "paymentDetailsConfirmation")
      PaymentDetailsConfirmationPage(testId).toString mustBe "paymentDetailsConfirmation"
    }
  }
}
