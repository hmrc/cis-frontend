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

class SurnameSpec extends SpecBase {

  import Surname.isValid
  import Surname.isLengthInRange

  "The Surname utility object" - {
    "returns false for a known invalid Name & invalid length" in {
      val invalidName = "A2345678901234567890123456789012345678901234567890#@[]{}$%^&£~"
      isValid(invalidName) mustBe false
      isLengthInRange(invalidName) mustBe false
    }

    "returns false for a known invalid Name & valid length" in {
      val invalidName = "A#@[]{}$%^£~"
      isValid(invalidName) mustBe false
      isLengthInRange(invalidName) mustBe true
    }

    "returns true for a known valid Name length & format" in {
      val validName = "Surname"
      isValid(validName) mustBe true
      isLengthInRange(validName) mustBe true
    }

    "returns true for a known valid Name format & length" in {
      val validName = "Surname1234567890 ,.&/-'"
      isValid(validName) mustBe true
      isLengthInRange(validName) mustBe true
    }

    "returns false for a known invalid Name format but invalid length" in {
      val invalidName = "123456789012345678901234567890<>{}[]!"
      isValid(invalidName) mustBe false
      isLengthInRange(invalidName) mustBe false
    }

  }
}
