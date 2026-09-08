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

import forms.Validation.emailRegex
import forms.mappings.Constants.MaxLength254
import models.validation.SubcontractorValidationField.EmailAddress
import models.validation.FieldValidationFailure

object EmailAddressValidator {

  def validate(value: Option[String]): Option[FieldValidationFailure] =
    value
      .filter(_.trim.nonEmpty)
      .flatMap { emailAddress =>
        Option.when(
          emailAddress.length > MaxLength254 ||
            !emailAddress.matches(emailRegex)
        ) {
          FieldValidationFailure(
            field = EmailAddress,
            value = Some(emailAddress)
          )
        }
      }
}
