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

class WorkReferenceNumberSpec extends SpecBase {

  import WorkReferenceNumber.isValid
  import WorkReferenceNumber.isLengthInRange

  "The WorkReferenceNumber utility object" - {

    "returns false for a known invalid WorkReferenceNumber & invalid length" in {
      val invalidWRN = "A12323452345#@[]{}$%^&£~"
      isValid(invalidWRN) mustBe false
      isLengthInRange(invalidWRN) mustBe false
    }

    "returns true for a known valid WorkReferenceNumber length & format" in {
      val validWRN = "Work Reference No."
      isValid(validWRN) mustBe true
      isLengthInRange(validWRN) mustBe true
    }

    "returns true for a known valid WorkReferenceNumber format & length" in {
      val validWRN = "Work Ref No 1234@"
      isValid(validWRN) mustBe true
      isLengthInRange(validWRN) mustBe true
    }

    "returns false for a known invalid WorkReferenceNumber format but valid length" in {
      val invalidWRN = "WRN No <>"
      isValid(invalidWRN) mustBe false
      isLengthInRange(invalidWRN) mustBe true
    }

  }

}
