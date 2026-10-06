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

import base.SpecBase
import models.validation.*

class FirstNameValidatorSpec extends SpecBase {

  "FirstNameValidator.validate" - {

    "return None when first name is missing" in {
      FirstNameValidator.validate(None) mustBe None
    }

    "return None when first name is valid" in {
      FirstNameValidator.validate(
        Some("John")
      ) mustBe None
    }

    "return a failure when first name is invalid" in {
      val invalidFirstName =
        "<invalid>"

      FirstNameValidator.validate(
        Some(invalidFirstName)
      ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.FirstName,
            value = Some(invalidFirstName)
          )
        )
    }
  }
}
