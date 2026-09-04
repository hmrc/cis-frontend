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

class WorksReferenceNumberValidatorSpec extends AnyWordSpec with Matchers {

  "WorksReferenceNumberValidator - validate WRN " must {

    "return no failure when the WRN is missing" in {
      WorksReferenceNumberValidator
        .validate(None) mustBe None
    }

    "return no failure when the WRN is empty" in {
      WorksReferenceNumberValidator
        .validate(Some("")) mustBe None
    }

    "return no failure when the WRN contains only whitespace" in {
      WorksReferenceNumberValidator
        .validate(Some("   ")) mustBe None
    }

    "return no failure for a valid WRN" in {
      WorksReferenceNumberValidator
        .validate(
          Some("5860")
        ) mustBe None
    }

    "return no failure for a valid WRN - Work Ref No 1234@" in {
      WorksReferenceNumberValidator
        .validate(
          Some("Work Ref No 1234@")
        ) mustBe None
    }

    "return a failure when the WRN exceeds the maximum length" in {
      val wrn = "A12323452345#@[]{}$%^&£~"

      WorksReferenceNumberValidator
        .validate(
          Some(wrn)
        ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.WorksReferenceNumber,
            value = Some(wrn)
          )
        )
    }

    "return a failure when the WRN contains incorrect format" in {
      val wrn = "WRN No <>"

      WorksReferenceNumberValidator
        .validate(
          Some(wrn)
        ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.WorksReferenceNumber,
            value = Some(wrn)
          )
        )
    }

  }
}
