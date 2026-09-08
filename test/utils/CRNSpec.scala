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

class CRNSpec extends SpecBase {
  import CRN.isValid
  import CRN.isLengthInRange

  "The CRN utility object" - {

    "returns false for a known invalid CRN" in {
      val invalidCrn = "5860920998"
      isValid(invalidCrn) mustBe false
      isLengthInRange(invalidCrn) mustBe false
    }

    "returns true for a known valid CRN length & format" in {
      val validCrn = "5860"
      isValid(validCrn) mustBe true
      isLengthInRange(validCrn) mustBe true
    }

    "returns true for a known valid CRN format" in {
      val validCrn = "AB5860"
      isValid(validCrn) mustBe true
      isLengthInRange(validCrn) mustBe true
    }

    "returns false for a known invalid CRN format with first value=char" in {
      val validCrn = "A5860"
      isValid(validCrn) mustBe false
      isLengthInRange(validCrn) mustBe true
    }

    "returns false for a known invalid CRN format but valid length" in {
      val invalidCrn = "ABC5860"
      isValid(invalidCrn) mustBe false
      isLengthInRange(invalidCrn) mustBe true
    }

  }

}
