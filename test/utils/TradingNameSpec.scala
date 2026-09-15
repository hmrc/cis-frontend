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

package utils

import base.SpecBase

class TradingNameSpec extends SpecBase {
  import TradingName.isValid
  import TradingName.isLengthInRange

  "The TradingName utility object" - {

    "returns false for a known valid TradingName & invalid length" in {
      val invalidName = "A2345678901234567890123456789012345678901234567890#@[]{}$%^&£~"
      isValid(invalidName) mustBe true
      isLengthInRange(invalidName) mustBe false
    }

    "returns true for a known valid TradingName length & format" in {
      val validName = "Test Trading Name"
      isValid(validName) mustBe true
      isLengthInRange(validName) mustBe true
    }

    "returns true for a known valid TradingName format & length" in {
      val validName = "Test Trading Name 1234@"
      isValid(validName) mustBe true
      isLengthInRange(validName) mustBe true
    }

    "returns false for a known invalid TradingName format but valid length" in {
      val invalidName = "12345678901234567890123456789012345678901234567890<>"
      isValid(invalidName) mustBe false
      isLengthInRange(invalidName) mustBe true
    }

  }

}
