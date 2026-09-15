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
import models.validation.FieldValidationFailure
import models.validation.SubcontractorValidationField.{MobilePhoneNumber, PhoneNumber}

class PhoneNumberValidatorSpec extends SpecBase {

  "PhoneNumberValidator.validatePhoneNumber" - {

    "return no failure when the phone number is missing" in {
      PhoneNumberValidator.validatePhoneNumber(None) mustBe None
    }

    "return no failure when the phone number is empty" in {
      PhoneNumberValidator.validatePhoneNumber(Some("")) mustBe None
    }

    "return no failure when the phone number contains only whitespace" in {
      PhoneNumberValidator.validatePhoneNumber(Some("   ")) mustBe None
    }

    "return no failure for a valid phone number" in {
      PhoneNumberValidator
        .validatePhoneNumber(Some("0191 123-4567")) mustBe None
    }

    "return a failure when an international phone number contains a plus sign" in {
      val phoneNumber =
        "+44 7700 900 999"

      PhoneNumberValidator.validatePhoneNumber(Some(phoneNumber)) mustBe
        Some(
          FieldValidationFailure(
            field = PhoneNumber,
            value = Some(phoneNumber)
          )
        )
    }

    "return a failure when the phone number contains a slash" in {
      val phoneNumber =
        "0191/1234567"

      PhoneNumberValidator.validatePhoneNumber(Some(phoneNumber)) mustBe
        Some(
          FieldValidationFailure(
            field = PhoneNumber,
            value = Some(phoneNumber)
          )
        )
    }

    "return no failure when the phone number is exactly 35 characters" in {
      PhoneNumberValidator
        .validatePhoneNumber(Some("1" * 35)) mustBe None
    }

    "return a failure when the phone number exceeds 35 characters" in {
      val phoneNumber =
        "1" * 36

      PhoneNumberValidator.validatePhoneNumber(Some(phoneNumber)) mustBe
        Some(
          FieldValidationFailure(
            field = PhoneNumber,
            value = Some(phoneNumber)
          )
        )
    }

    "return a failure when the phone number contains unsupported characters" in {
      val phoneNumber =
        "0191 PHONE"

      PhoneNumberValidator.validatePhoneNumber(Some(phoneNumber)) mustBe
        Some(
          FieldValidationFailure(
            field = PhoneNumber,
            value = Some(phoneNumber)
          )
        )
    }

    "return no failure when fewer than six digits are allowed by the supplied regex" in {
      PhoneNumberValidator
        .validatePhoneNumber(Some("12345")) mustBe None
    }

    "return no failure for zero-digit values allowed by the supplied regex" in {
      Seq("()", "-").foreach { phoneNumber =>
        PhoneNumberValidator
          .validatePhoneNumber(Some(phoneNumber)) mustBe None
      }
    }

    "return no failure when a tab is allowed by the supplied regex" in {
      PhoneNumberValidator
        .validatePhoneNumber(Some("0191\t1234567")) mustBe None
    }
  }

  "PhoneNumberValidator.validateMobilePhoneNumber" - {

    "return no failure when the mobile number is missing" in {
      PhoneNumberValidator.validateMobilePhoneNumber(None) mustBe None
    }

    "return no failure for a valid mobile number" in {
      PhoneNumberValidator
        .validateMobilePhoneNumber(Some("07700 900123")) mustBe None
    }

    "return a failure with the mobile-phone field identifier when invalid" in {
      val mobilePhoneNumber =
        "07700 MOBILE"

      PhoneNumberValidator.validateMobilePhoneNumber(Some(mobilePhoneNumber)) mustBe
        Some(
          FieldValidationFailure(
            field = MobilePhoneNumber,
            value = Some(mobilePhoneNumber)
          )
        )
    }

    "return a failure when the mobile number exceeds 35 characters" in {
      val mobilePhoneNumber =
        "1" * 36

      PhoneNumberValidator.validateMobilePhoneNumber(Some(mobilePhoneNumber)) mustBe
        Some(
          FieldValidationFailure(
            field = MobilePhoneNumber,
            value = Some(mobilePhoneNumber)
          )
        )
    }

    "return no failure for zero-digit values allowed by the supplied regex" in {
      PhoneNumberValidator
        .validateMobilePhoneNumber(Some("()")) mustBe None
    }
  }
}
