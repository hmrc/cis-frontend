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

import models.monthlyreturns.Subcontractor
import models.validation.{FieldValidationFailure, SubcontractorValidationField}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDateTime

class UtrValidatorSpec extends AnyWordSpec with Matchers {
  "UtrValidator - validate UTR" must {

    "return no failure when the UTR is missing" in {
      UtrValidator.validate(None, Seq.empty) mustBe None
    }

    "return no failure when the UTR is empty" in {
      UtrValidator
        .validate(Some(""), Seq.empty) mustBe None
    }

    "return no failure when the UTR contains only whitespace" in {
      UtrValidator
        .validate(Some("   "), Seq.empty) mustBe None
    }

    "return no failure for a valid UTR - 5860920998" in {
      UtrValidator
        .validate(Some("5860920998"), Seq.empty) mustBe None
    }

    "return a failure when the UTR exceeds the maximum length" in {
      val utr = "1234567890"

      UtrValidator
        .validate(Some(utr), Seq.empty) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = Some(utr)
          )
        )
    }

    "return a failure when the UTR contains incorrect format" in {
      val utr = "12345A7890"

      UtrValidator
        .validate(Some(utr), Seq.empty) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = Some(utr)
          )
        )
    }

    "retain the original invalid UTR in the failure" in {
      val utr = "invalid-number"

      UtrValidator
        .validate(Some(utr), Seq.empty) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = Some(utr)
          )
        )
    }

    "return a failure when the UTR is duplicated" in {
      val utr = "5860920998"

      val subcontractors = Seq(
        Subcontractor(
          subcontractorId = 1L,
          utr = Some(utr),
          pageVisited = Some(1),
          partnerUtr = Some("1234567890"),
          crn = Some("CRN123456"),
          firstName = Some("John"),
          nino = Some("AB123456C"),
          secondName = Some("Michael"),
          surname = Some("Smith"),
          partnershipTradingName = Some("Smith & Partners"),
          tradingName = Some("John Smith Builders"),
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
          worksReferenceNumber = Some("WR123456"),
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
        ),
        Subcontractor(
          subcontractorId = 2L,
          utr = Some(utr),
          pageVisited = Some(2),
          partnerUtr = Some("9876543210"),
          crn = Some("CRN654321"),
          firstName = Some("Jane"),
          nino = Some("CD654321E"),
          secondName = Some("Elizabeth"),
          surname = Some("Jones"),
          partnershipTradingName = Some("Jones & Co"),
          tradingName = Some("Jane Jones Construction"),
          subcontractorType = Some("Company"),
          addressLine1 = Some("25 Market Street"),
          addressLine2 = Some("West End"),
          addressLine3 = Some("Manchester"),
          addressLine4 = Some("Greater Manchester"),
          country = Some("United Kingdom"),
          postCode = Some("M1 1AA"),
          emailAddress = Some("jane.jones@example.com"),
          phoneNumber = Some("01611234567"),
          mobilePhoneNumber = Some("07234567890"),
          worksReferenceNumber = Some("WR654321"),
          createDate = Some(LocalDateTime.of(2025, 3, 5, 10, 15)),
          lastUpdate = Some(LocalDateTime.of(2025, 4, 20, 16, 20)),
          subbieResourceRef = Some(1002L),
          matched = Some("N"),
          autoVerified = Some("N"),
          verified = Some("Y"),
          verificationNumber = Some("VN654321"),
          taxTreatment = Some("0T"),
          verificationDate = Some(LocalDateTime.of(2025, 4, 20, 16, 0)),
          version = Some(2),
          updatedTaxTreatment = Some("0T"),
          lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 4, 1, 0, 0)),
          pendingVerifications = Some(1),
          displayName = Some("Jane Jones")
        )
      )

      UtrValidator
        .validate(Some(utr), subcontractors) mustBe
        Some(
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = Some(utr)
          )
        )
    }

    "return no failure when the UTR is not duplicated" in {
      val utr = "5860920998"

      val subcontractors = Seq(
        Subcontractor(
          subcontractorId = 1L,
          utr = Some("1234567890"),
          pageVisited = Some(1),
          partnerUtr = Some("9876543210"),
          crn = Some("CRN123456"),
          firstName = Some("John"),
          nino = Some("AB123456C"),
          secondName = Some("Michael"),
          surname = Some("Smith"),
          partnershipTradingName = Some("Smith & Partners"),
          tradingName = Some("John Smith Builders"),
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
          worksReferenceNumber = Some("WR123456"),
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
      )

      UtrValidator
        .validate(Some(utr), subcontractors) mustBe None
    }
  }
}
