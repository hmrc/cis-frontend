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

import models.monthlyreturns.Subcontractor
import models.validation.{FieldValidationFailure, SubcontractorValidationField}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDateTime

class CompanyValidatorSpec extends AnyWordSpec with Matchers {

  "CompanyValidator.validate" must {

    "return a trading name failure when all common details are missing" in {
      CompanyValidator.validate(
        subcontractor = subcontractorEmpty,
        subcontractors = Seq(subcontractorInvalid, subcontractorEmpty, subcontractorValid)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.TradingName,
            value = None
          )
        )
    }

    "return no failures when all fields are valid" in {
      CompanyValidator.validate(
        subcontractor = subcontractorValid,
        subcontractors = Seq(subcontractorInvalid, subcontractorEmpty, subcontractorValid)
      ) mustBe Nil
    }

    "return every failure" in {
      CompanyValidator.validate(
        subcontractor = subcontractorInvalid,
        subcontractors = Seq(subcontractorInvalid, subcontractorEmpty)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.WorksReferenceNumber,
            value = subcontractorInvalid.worksReferenceNumber
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = subcontractorInvalid.utr
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.TradingName,
            value = subcontractorInvalid.tradingName
          )
        )
    }

    "retain valid fields while returning only invalid fields" in {
      CompanyValidator.validate(
        subcontractor = subcontractorSomeValid,
        subcontractors = Seq(subcontractorSomeValid, subcontractorInvalid, subcontractorEmpty)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.WorksReferenceNumber,
            value = subcontractorSomeValid.worksReferenceNumber
          )
        )
    }

    "return a UTR failure when the UTR is duplicated" in {
      val utr = "5860920998"

      val subcontractorToValidate =
        subcontractorValid.copy(utr = Some(utr))

      val subcontractors =
        Seq(
          subcontractorValid.copy(utr = Some(utr)),
          subcontractorValid.copy(utr = Some(utr))
        )

      CompanyValidator.validate(
        subcontractor = subcontractorToValidate,
        subcontractors = subcontractors
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = Some(utr)
          )
        )
    }
  }

  private def subcontractorEmpty: Subcontractor =
    subcontractor(
      utr = None,
      tradingName = None,
      worksReferenceNumber = None
    )

  private def subcontractorValid: Subcontractor =
    subcontractor(
      utr = Some("5860920998"),
      tradingName = Some("Trading Name"),
      worksReferenceNumber = None
    )

  private def subcontractorSomeValid: Subcontractor =
    subcontractor(
      utr = Some("5860920998"),
      tradingName = Some("Test Trading Name 1234@"),
      worksReferenceNumber = Some("A12323452345#@[]{}$%^&£~")
    )

  private def subcontractorInvalid: Subcontractor =
    subcontractor(
      utr = Some("12345A7890"),
      tradingName = Some("12345678901234567890123456789012345678901234567890<>"),
      worksReferenceNumber = Some("A12323452345#@[]{}$%^&£~")
    )

  private def subcontractor(
    worksReferenceNumber: Option[String] = None,
    tradingName: Option[String] = None,
    utr: Option[String] = None
  ): Subcontractor =
    Subcontractor(
      subcontractorId = 1L,
      utr = utr,
      pageVisited = Some(1),
      partnerUtr = Some("1234567890"),
      crn = Some("AB5860"),
      firstName = Some("John"),
      nino = Some("AB123456C"),
      secondName = Some("Michael"),
      surname = Some("Smith"),
      partnershipTradingName = Some("Smith & Partners"),
      tradingName = tradingName,
      subcontractorType = Some("Sole Trader"),
      addressLine1 = Some("1 High Street"),
      addressLine2 = Some("Central"),
      addressLine3 = Some("London"),
      addressLine4 = Some("Greater London"),
      country = Some("United Kingdom"),
      postCode = Some("SW1A 1AA"),
      emailAddress = Some("john.smith@example.com"),
      phoneNumber = Some("02071234567"),
      mobilePhoneNumber = Some("07123456789"),
      worksReferenceNumber = worksReferenceNumber,
      createDate = Some(LocalDateTime.of(2025, 1, 10, 9, 30)),
      lastUpdate = Some(LocalDateTime.of(2025, 2, 15, 14, 45)),
      subbieResourceRef = Some(1001L),
      matched = Some("Y"),
      autoVerified = Some("Y"),
      verified = Some("Y"),
      verificationNumber = Some("VN123456"),
      taxTreatment = Some("20"),
      verificationDate = Some(LocalDateTime.of(2025, 2, 15, 14, 30)),
      version = Some(1),
      updatedTaxTreatment = Some("20"),
      lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 2, 1, 0, 0)),
      pendingVerifications = Some(0),
      displayName = Some("John Smith")
    )
}
