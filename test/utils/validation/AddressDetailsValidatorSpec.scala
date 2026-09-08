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
import models.validation.SubcontractorValidationField.*
import models.validation.{AddressDetails, FieldValidationFailure}

class AddressDetailsValidatorSpec extends SpecBase {

  "AddressDetailsValidator.validate" - {

    "return no failures when the address is missing" in {
      AddressDetailsValidator.validate(None) mustBe Nil
    }

    "return no failures when every address field is missing" in {
      AddressDetailsValidator.validate(Some(address())) mustBe Nil
    }

    "return no failures for a valid address" in {
      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            addressLine2 = Some("Newcastle"),
            postcode = Some("NE1 1AA"),
            country = Some("United Kingdom")
          )
        )
      ) mustBe Nil
    }

    "require address line 1 when another address line exists" in {
      AddressDetailsValidator.validate(
        Some(address(addressLine2 = Some("Newcastle")))
      ) mustBe
        List(
          FieldValidationFailure(
            field = AddressLine1,
            value = None
          )
        )
    }

    "require address line 1 when only the postcode exists" in {
      AddressDetailsValidator.validate(
        Some(address(postcode = Some("NE1 1AA")))
      ) mustBe
        List(
          FieldValidationFailure(
            field = AddressLine1,
            value = None
          )
        )
    }

    "require address line 1 when only the country exists" in {
      AddressDetailsValidator.validate(
        Some(address(country = Some("United Kingdom")))
      ) mustBe
        List(
          FieldValidationFailure(
            field = AddressLine1,
            value = None
          )
        )
    }

    "treat whitespace-only address line 1 as missing when required" in {
      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("   "),
            addressLine2 = Some("Newcastle")
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(
            field = AddressLine1,
            value = None
          )
        )
    }

    "return no failure when an address line is exactly 35 characters" in {
      AddressDetailsValidator.validate(
        Some(address(addressLine1 = Some("A" * 35)))
      ) mustBe Nil
    }

    "return a failure when an address line exceeds 35 characters" in {
      val value =
        "A" * 36

      AddressDetailsValidator.validate(
        Some(address(addressLine1 = Some(value)))
      ) mustBe
        List(
          FieldValidationFailure(
            field = AddressLine1,
            value = Some(value)
          )
        )
    }

    "return a failure when an address value does not start with a letter or digit" in {
      val value =
        "-High Street"

      AddressDetailsValidator.validate(
        Some(address(addressLine1 = Some(value)))
      ) mustBe
        List(
          FieldValidationFailure(
            field = AddressLine1,
            value = Some(value)
          )
        )
    }

    "return failures for every invalid optional address line" in {
      val line2 =
        "-Newcastle"

      val line3 =
        "A" * 36

      val line4 =
        "Tyne | Wear"

      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            addressLine2 = Some(line2),
            addressLine3 = Some(line3),
            addressLine4 = Some(line4)
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(AddressLine2, Some(line2)),
          FieldValidationFailure(AddressLine3, Some(line3)),
          FieldValidationFailure(AddressLine4, Some(line4))
        )
    }

    "return no failure when the postcode is exactly 8 characters" in {
      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            postcode = Some("NE12 3AA")
          )
        )
      ) mustBe Nil
    }

    "return a failure when the postcode exceeds 8 characters" in {
      val postcode =
        "ABCDEFGHI"

      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            postcode = Some(postcode)
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(
            field = Postcode,
            value = Some(postcode)
          )
        )
    }

    "return a failure when the postcode contains unsupported characters" in {
      val postcode =
        "NE1`1AA"

      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            postcode = Some(postcode)
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(
            field = Postcode,
            value = Some(postcode)
          )
        )
    }

    "return no failure for permitted postcode characters" in {
      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            postcode = Some("A~!@#")
          )
        )
      ) mustBe Nil
    }

    "return a failure when the country exceeds 35 characters" in {
      val country =
        "A" * 36

      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine1 = Some("1 High Street"),
            country = Some(country)
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(
            field = Country,
            value = Some(country)
          )
        )
    }

    "return every failure in address-field order" in {
      val line2 =
        "-Newcastle"

      val line3 =
        "A" * 36

      val postcode =
        "ABCDEFGHI"

      val country =
        "-United Kingdom"

      AddressDetailsValidator.validate(
        Some(
          address(
            addressLine2 = Some(line2),
            addressLine3 = Some(line3),
            postcode = Some(postcode),
            country = Some(country)
          )
        )
      ) mustBe
        List(
          FieldValidationFailure(AddressLine1, None),
          FieldValidationFailure(AddressLine2, Some(line2)),
          FieldValidationFailure(AddressLine3, Some(line3)),
          FieldValidationFailure(Postcode, Some(postcode)),
          FieldValidationFailure(Country, Some(country))
        )
    }
  }

  private def address(
    addressLine1: Option[String] = None,
    addressLine2: Option[String] = None,
    addressLine3: Option[String] = None,
    addressLine4: Option[String] = None,
    postcode: Option[String] = None,
    country: Option[String] = None
  ): AddressDetails =
    AddressDetails(
      addressLine1 = addressLine1,
      addressLine2 = addressLine2,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postcode = postcode,
      country = country
    )
}
