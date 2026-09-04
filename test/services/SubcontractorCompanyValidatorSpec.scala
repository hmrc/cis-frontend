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
import models.validation.SubcontractorValidationField.{Crn, TradingName, Utr, WorksReferenceNumber}
import models.validation.{FieldValidationFailure, SubcontractorValidationFailure}

class SubcontractorCompanyValidatorSpec extends SpecBase {

  private val validator =
    new SubcontractorCompanyValidator()

  "SubcontractorCompanyValidator.validate" - {

    "return no failures for an empty subcontractor list" in {
      validator.validate(Seq.empty) mustBe Nil
    }

    "exclude a subcontractor when all common details are valid" in {
      validator.validate(
        Seq(subcontractor(1L))
      ) mustBe Nil
    }

    "return a subcontractor containing an invalid works reference number" in {
      val invalidWrn =
        "A12323452345#@[]{}$%^&£~"

      val result =
        validator.validate(
          Seq(
            subcontractor(1L).copy(
              worksReferenceNumber = Some(invalidWrn)
            )
          )
        )

      result mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = WorksReferenceNumber,
                value = Some(invalidWrn)
              )
            )
          )
        )
    }

    "return a subcontractor containing an invalid crn" in {
      val invalidCrn =
        "invalid-number"

      val result =
        validator.validate(
          Seq(
            subcontractor(1L).copy(
              crn = Some(invalidCrn)
            )
          )
        )

      result mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = Crn,
                value = Some(invalidCrn)
              )
            )
          )
        )
    }

    "return a subcontractor containing an invalid TradingName" in {
      val invalidTradingName =
        "2345678901234567890123456789012345678901234567890<>"

      val result =
        validator.validate(
          Seq(
            subcontractor(1L).copy(
              tradingName = Some(invalidTradingName)
            )
          )
        )

      result mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = TradingName,
                value = Some(invalidTradingName)
              )
            )
          )
        )
    }

    "return a subcontractor containing an invalid Utr" in {
      val invalidUtr =
        "1234567890"

      val result =
        validator.validate(
          Seq(
            subcontractor(1L).copy(
              utr = Some(invalidUtr)
            )
          )
        )

      result mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = Utr,
                value = Some(invalidUtr)
              )
            )
          )
        )
    }

    "return subcontractors containing a duplicated Utr" in {
      val result =
        validator.validate(
          Seq(
            subcontractor(1L),
            subcontractor(2L)
          )
        )

      result mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = Utr,
                value = Some("5860920998")
              )
            )
          ),
          SubcontractorValidationFailure(
            subcontractorId = 2L,
            failedFields = List(
              FieldValidationFailure(
                field = Utr,
                value = Some("5860920998")
              )
            )
          )
        )
    }
  }

  private def subcontractor(
    subcontractorId: Long
  ): Subcontractor =
    Subcontractor(
      subcontractorId = subcontractorId,
      utr = Some("5860920998"),
      pageVisited = Some(1),
      partnerUtr = None,
      crn = Some("AB5860"),
      firstName = None,
      nino = Some("AA123456A"),
      secondName = None,
      surname = None,
      partnershipTradingName = None,
      tradingName = Some("Trading Name ABC Ltd"),
      subcontractorType = Some("company"),
      addressLine1 = Some("1 High Street"),
      addressLine2 = Some("Newcastle"),
      addressLine3 = None,
      addressLine4 = None,
      country = Some("GB"),
      postCode = Some("NE1 1AA"),
      emailAddress = Some("subcontractor@example.com"),
      phoneNumber = Some("0191 123 4567"),
      mobilePhoneNumber = Some("07700 900123"),
      worksReferenceNumber = Some("Work Ref No 1234@"),
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
      displayName = Some("Trading Name ABC Ltd")
    )
}
