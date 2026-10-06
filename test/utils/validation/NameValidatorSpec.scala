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

class NameValidatorSpec extends SpecBase {

  "NameValidator.validate" - {

    "return no failures when first name, surname and trading name are present" in {
      NameValidator.validate(
        firstName = Some("John"),
        secondName = None,
        surname = Some("Smith"),
        tradingName = Some("Smith Builders")
      ) mustBe Seq.empty
    }

    "return FirstName failure when trading name and first name are blank" in {
      NameValidator.validate(
        firstName = None,
        secondName = None,
        surname = Some("Smith"),
        tradingName = None
      ) mustBe
        Seq(
          FieldValidationFailure(
            field = SubcontractorValidationField.FirstName,
            value = None
          )
        )
    }

    "return TradingName and Surname failures when trading name and surname are blank" in {
      NameValidator.validate(
        firstName = None,
        secondName = None,
        surname = None,
        tradingName = None
      ) mustBe
        Seq(
          FieldValidationFailure(
            field = SubcontractorValidationField.FirstName,
            value = None
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.TradingName,
            value = None
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.Surname,
            value = None
          )
        )
    }

    "return Surname failure when first name is present but surname is blank" in {
      NameValidator.validate(
        firstName = Some("John"),
        secondName = None,
        surname = None,
        tradingName = Some("Smith Builders")
      ) mustBe
        Seq(
          FieldValidationFailure(
            field = SubcontractorValidationField.Surname,
            value = None
          )
        )
    }

    "return FirstName failure when second name is present but first name is blank" in {
      NameValidator.validate(
        firstName = None,
        secondName = Some("Michael"),
        surname = Some("Smith"),
        tradingName = Some("Smith Builders")
      ) mustBe
        Seq(
          FieldValidationFailure(
            field = SubcontractorValidationField.FirstName,
            value = None
          )
        )
    }
  }
}
