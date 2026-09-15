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

import forms.Validation.{mobileRegex, phoneRegex}
import forms.mappings.Constants.MaxLength35
import models.validation.{FieldValidationFailure, SubcontractorValidationField}

object PhoneNumberValidator {

  def validatePhoneNumber(value: Option[String]): Option[FieldValidationFailure] =
    validate(
      field = SubcontractorValidationField.PhoneNumber,
      value = value,
      regex = phoneRegex
    )

  def validateMobilePhoneNumber(value: Option[String]): Option[FieldValidationFailure] =
    validate(
      field = SubcontractorValidationField.MobilePhoneNumber,
      value = value,
      regex = mobileRegex
    )

  private def validate(
    field: SubcontractorValidationField,
    value: Option[String],
    regex: String
  ): Option[FieldValidationFailure] =
    value
      .filter(_.trim.nonEmpty)
      .flatMap { phoneNumber =>
        Option.when(
          phoneNumber.length > MaxLength35 ||
            !phoneNumber.matches(regex)
        ) {
          FieldValidationFailure(
            field = field,
            value = Some(phoneNumber)
          )
        }
      }
}
