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

package services

import base.SpecBase
import models.monthlyreturns.Subcontractor
import models.validation.SubcontractorValidationField.{AddressLine1, Country, EmailAddress, PhoneNumber, Postcode}
import models.validation.{FieldValidationFailure, SubcontractorValidationFailure}

class SubcontractorDetailsValidatorSpec extends SpecBase {

  private val validator =
    new SubcontractorDetailsValidator()

  "SubcontractorDetailsValidator.validate" - {

    "return no failures for an empty subcontractor list" in {
      validator.validate(Seq.empty) mustBe Nil
    }

    "exclude a subcontractor when all common details are valid" in {
      validator.validate(Seq(subcontractor(1L))) mustBe Nil
    }

    "return a subcontractor containing an invalid email address" in {
      val invalidEmail =
        "invalid-email"

      validator.validate(
        Seq(
          subcontractor(1L).copy(
            emailAddress = Some(invalidEmail)
          )
        )
      ) mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = EmailAddress,
                value = Some(invalidEmail)
              )
            )
          )
        )
    }

    "return an address-line-1 failure when another address field exists" in {
      validator.validate(
        Seq(
          subcontractor(2L).copy(
            addressLine1 = None,
            addressLine2 = Some("Newcastle"),
            addressLine3 = None,
            addressLine4 = None,
            postCode = None,
            country = None
          )
        )
      ) mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 2L,
            failedFields = List(
              FieldValidationFailure(
                field = AddressLine1,
                value = None
              )
            )
          )
        )
    }

    "return no address failure when every address field is missing" in {
      validator.validate(
        Seq(
          subcontractor(3L).copy(
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            postCode = None,
            country = None
          )
        )
      ) mustBe Nil
    }

    "map and validate the flat postcode field" in {
      val invalidPostcode =
        "ABCDEFGHI"

      validator.validate(
        Seq(
          subcontractor(4L).copy(
            postCode = Some(invalidPostcode)
          )
        )
      ) mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 4L,
            failedFields = List(
              FieldValidationFailure(
                field = Postcode,
                value = Some(invalidPostcode)
              )
            )
          )
        )
    }

    "map and validate the flat country field" in {
      val invalidCountry =
        "-United Kingdom"

      validator.validate(
        Seq(
          subcontractor(5L).copy(
            country = Some(invalidCountry)
          )
        )
      ) mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 5L,
            failedFields = List(
              FieldValidationFailure(
                field = Country,
                value = Some(invalidCountry)
              )
            )
          )
        )
    }

    "return only subcontractors containing failures and preserve their order" in {
      val invalidPhone =
        "0191 PHONE"

      val invalidEmail =
        "invalid-email"

      validator.validate(
        Seq(
          subcontractor(1L),
          subcontractor(2L).copy(
            phoneNumber = Some(invalidPhone)
          ),
          subcontractor(3L),
          subcontractor(4L).copy(
            emailAddress = Some(invalidEmail)
          )
        )
      ) mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 2L,
            failedFields = List(
              FieldValidationFailure(
                field = PhoneNumber,
                value = Some(invalidPhone)
              )
            )
          ),
          SubcontractorValidationFailure(
            subcontractorId = 4L,
            failedFields = List(
              FieldValidationFailure(
                field = EmailAddress,
                value = Some(invalidEmail)
              )
            )
          )
        )
    }
  }

  private def subcontractor(subcontractorId: Long): Subcontractor =
    Subcontractor(
      subcontractorId = subcontractorId,
      utr = Some("1234567890"),
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = Some("John"),
      nino = Some("AA123456A"),
      secondName = None,
      surname = Some("Smith"),
      partnershipTradingName = None,
      tradingName = None,
      subcontractorType = Some("soletrader"),
      addressLine1 = Some("1 High Street"),
      addressLine2 = Some("Newcastle"),
      addressLine3 = None,
      addressLine4 = None,
      country = Some("United Kingdom"),
      postCode = Some("NE1 1AA"),
      emailAddress = Some("subcontractor@example.com"),
      phoneNumber = Some("0191 123 4567"),
      mobilePhoneNumber = Some("07700 900123"),
      worksReferenceNumber = None,
      createDate = None,
      lastUpdate = None,
      subbieResourceRef = Some(subcontractorId * 10),
      matched = None,
      autoVerified = None,
      verified = None,
      verificationNumber = None,
      taxTreatment = None,
      verificationDate = None,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = None,
      pendingVerifications = None,
      displayName = Some("John Smith")
    )
}
