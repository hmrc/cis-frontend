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

package models.finalvalidation

import base.SpecBase
import play.api.libs.json.*

class FinalValidationChangeTargetSpec extends SpecBase {

  import FinalValidationChangeTarget.*

  "FinalValidationChangeTarget" - {

    "return a target from its key" in {
      fromKey("utr") mustBe Some(Utr)
    }

    "return None for an unknown key" in {
      fromKey("unknown") mustBe None
    }

    "serialise and deserialise" in {
      Json.toJson[FinalValidationChangeTarget](TradingName) mustBe JsString("tradingName")

      Json.fromJson[FinalValidationChangeTarget](JsString("tradingName")) mustBe
        JsSuccess(TradingName)
    }

    "fail to deserialise an unknown key" in {
      Json.fromJson[FinalValidationChangeTarget](JsString("unknown")) mustBe a[JsError]
    }
  }
}
