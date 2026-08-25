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

import forms.Validation.{addressRegex, firstCharLetterOrDigitRegex, ukPostcodeRegex}
import forms.mappings.Constants.{MaxLength35, MaxLength8}
import models.validation.{AddressDetails, FieldValidationFailure, SubcontractorValidationField}

object AddressDetailsValidator {

  def validate(address: Option[AddressDetails]): List[FieldValidationFailure] =
    address.fold(List.empty[FieldValidationFailure])(validateAddress)

  private def validateAddress(address: AddressDetails): List[FieldValidationFailure] =
    List(
      validateAddressLine1(address),
      validateAddressField(
        SubcontractorValidationField.AddressLine2,
        address.addressLine2
      ),
      validateAddressField(
        SubcontractorValidationField.AddressLine3,
        address.addressLine3
      ),
      validateAddressField(
        SubcontractorValidationField.AddressLine4,
        address.addressLine4
      ),
      validatePostcode(address.postcode),
      validateAddressField(
        SubcontractorValidationField.Country,
        address.country
      )
    ).flatten

  private def validateAddressLine1(address: AddressDetails): Option[FieldValidationFailure] = {
    val otherAddressInformationCompleted =
      List(
        address.addressLine2,
        address.addressLine3,
        address.addressLine4,
        address.postcode,
        address.country
      ).exists(isCompleted)

    val missingWhenRequired =
      otherAddressInformationCompleted &&
        !isCompleted(address.addressLine1)

    val suppliedButInvalid =
      completedValue(address.addressLine1).exists(isInvalidAddressValue)

    Option.when(missingWhenRequired || suppliedButInvalid) {
      FieldValidationFailure(
        field = SubcontractorValidationField.AddressLine1,
        value = completedValue(address.addressLine1)
      )
    }
  }

  private def validateAddressField(
    field: SubcontractorValidationField,
    value: Option[String]
  ): Option[FieldValidationFailure] =
    completedValue(value)
      .filter(isInvalidAddressValue)
      .map { invalidValue =>
        FieldValidationFailure(
          field = field,
          value = Some(invalidValue)
        )
      }

  private def validatePostcode(value: Option[String]): Option[FieldValidationFailure] =
    completedValue(value)
      .filter { postcode =>
        postcode.length > MaxLength8 ||
        !postcode.matches(ukPostcodeRegex)
      }
      .map { invalidValue =>
        FieldValidationFailure(
          field = SubcontractorValidationField.Postcode,
          value = Some(invalidValue)
        )
      }

  private def isInvalidAddressValue(value: String): Boolean =
    value.length > MaxLength35 ||
      !value.matches(addressRegex) ||
      !value.matches(firstCharLetterOrDigitRegex)

  private def completedValue(value: Option[String]): Option[String] =
    value.filter(_.trim.nonEmpty)

  private def isCompleted(value: Option[String]): Boolean =
    completedValue(value).isDefined
}
