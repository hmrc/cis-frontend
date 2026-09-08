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
import models.validation.SubcontractorValidationField.*
import models.validation.{AddressDetails, FieldValidationFailure}

class CommonDetailsValidatorSpec extends SpecBase {

  "CommonDetailsValidator.validate" - {

    "return no failures when all common details are missing" in {
      CommonDetailsValidator.validate(
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = None,
        address = None
      ) mustBe Nil
    }

    "return no failures when all common details are valid" in {
      CommonDetailsValidator.validate(
        emailAddress = Some("subcontractor@example.com"),
        phoneNumber = Some("0191 123 4567"),
        mobilePhoneNumber = Some("07700 900123"),
        address = Some(
          AddressDetails(
            addressLine1 = Some("1 High Street"),
            addressLine2 = Some("Newcastle"),
            addressLine3 = None,
            addressLine4 = None,
            postcode = Some("NE1 1AA"),
            country = Some("United Kingdom")
          )
        )
      ) mustBe Nil
    }

    "return an email failure" in {
      val emailAddress =
        "invalid-email"

      CommonDetailsValidator.validate(
        emailAddress = Some(emailAddress),
        phoneNumber = None,
        mobilePhoneNumber = None,
        address = None
      ) mustBe
        List(
          FieldValidationFailure(
            field = EmailAddress,
            value = Some(emailAddress)
          )
        )
    }

    "return every common-details failure in field order" in {
      val emailAddress =
        "invalid-email"

      val phoneNumber =
        "0191 PHONE"

      val mobilePhoneNumber =
        "07700 MOBILE"

      val addressLine2 =
        "-Newcastle"

      val postcode =
        "ABCDEFGHI"

      CommonDetailsValidator.validate(
        emailAddress = Some(emailAddress),
        phoneNumber = Some(phoneNumber),
        mobilePhoneNumber = Some(mobilePhoneNumber),
        address = Some(
          AddressDetails(
            addressLine1 = None,
            addressLine2 = Some(addressLine2),
            addressLine3 = None,
            addressLine4 = None,
            postcode = Some(postcode),
            country = None
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(EmailAddress, Some(emailAddress)),
          FieldValidationFailure(PhoneNumber, Some(phoneNumber)),
          FieldValidationFailure(MobilePhoneNumber, Some(mobilePhoneNumber)),
          FieldValidationFailure(AddressLine1, None),
          FieldValidationFailure(AddressLine2, Some(addressLine2)),
          FieldValidationFailure(Postcode, Some(postcode))
        )
    }

    "retain valid fields while returning only invalid fields" in {
      val phoneNumber =
        "0191 PHONE"

      CommonDetailsValidator.validate(
        emailAddress = Some("subcontractor@example.com"),
        phoneNumber = Some(phoneNumber),
        mobilePhoneNumber = Some("07700 900123"),
        address = Some(
          AddressDetails(
            addressLine1 = Some("1 High Street"),
            addressLine2 = Some("Newcastle"),
            addressLine3 = None,
            addressLine4 = None,
            postcode = Some("NE1 1AA"),
            country = Some("United Kingdom")
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(
            field = PhoneNumber,
            value = Some(phoneNumber)
          )
        )
    }
  }
}
