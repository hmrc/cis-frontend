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

package utils.validation

import models.validation.{FieldValidationFailure, SubcontractorValidationField}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CrnValidatorSpec extends AnyWordSpec with Matchers {
  "CrnValidator - validate CRN " must {

    "return no failure when the CRN is missing" in {
      CrnValidator
        .validate(None) mustBe None
    }

    "return no failure when the CRN is empty" in {
      CrnValidator
        .validate(Some("")) mustBe None
    }

    "return no failure when the CRN contains only whitespace" in {
      CrnValidator
        .validate(Some("   ")) mustBe None
    }

    "return no failure for a valid CRN" in {
      CrnValidator
        .validate(
          Some("5860")
        ) mustBe None
    }

    "return no failure for a valid CRN - AB5860" in {
      CrnValidator
        .validate(
          Some("AB5860")
        ) mustBe None
    }

    "return a failure when the CRN exceeds the maximum length" in {
      val crn = "5860920998"

      CrnValidator
        .validate(
          Some(crn)
        ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Crn,
            value = Some(crn)
          )
        )
    }

    "return a failure when the CRN contains incorrect format" in {
      val crn = "ABC5860"

      CrnValidator
        .validate(
          Some(crn)
        ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Crn,
            value = Some(crn)
          )
        )
    }

    "retain the original invalid CRN in the failure" in {
      val crn =
        "invalid-number"

      CrnValidator
        .validate(
          Some(crn)
        ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Crn,
            value = Some(crn)
          )
        )
    }
  }
}
