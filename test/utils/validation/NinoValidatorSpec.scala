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

class NinoValidatorSpec extends SpecBase {

  "NinoValidator.validate" - {

    "return None when NINO is missing" in {
      NinoValidator.validate(None) mustBe None
    }

    "return None when NINO is blank" in {
      NinoValidator.validate(
        Some("   ")
      ) mustBe None
    }

    "return None when NINO is valid" in {
      NinoValidator.validate(
        Some("AA123456A")
      ) mustBe None
    }

    "return a failure when NINO is too long" in {
      val invalidNino =
        "AA123456AA"

      NinoValidator.validate(
        Some(invalidNino)
      ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Nino,
            value = Some(invalidNino)
          )
        )
    }

    "return a failure when NINO format is invalid" in {
      val invalidNino =
        "123456789"

      NinoValidator.validate(
        Some(invalidNino)
      ) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Nino,
            value = Some(invalidNino)
          )
        )
    }
  }
}
