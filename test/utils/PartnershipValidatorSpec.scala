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
import models.validation.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PartnershipValidatorSpec extends AnyWordSpec with Matchers {

  "PartnershipValidator.validate" must {

    "return no failures when all fields are valid" in {
      PartnershipValidator.validate(
        subcontractorToValidate = subcontractorValid,
        allSubcontractors = Seq(subcontractorValid)
      ) mustBe Nil
    }

    "return a partnership trading name failure when partnership trading name is missing" in {
      val subcontractor =
        subcontractorValid.copy(
          partnershipTradingName = None
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractor,
        allSubcontractors = Seq(subcontractor)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.PartnershipTradingName,
            value = None
          )
        )
    }

    "return a trading name failure when trading name is missing" in {
      val subcontractor =
        subcontractorValid.copy(
          tradingName = None
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractor,
        allSubcontractors = Seq(subcontractor)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.TradingName,
            value = None
          )
        )
    }

    "return a UTR failure when the UTR is duplicated" in {
      val utr = "5860920998"

      val subcontractorToValidate =
        subcontractorValid.copy(
          subcontractorId = 1L,
          utr = Some(utr)
        )

      val otherSubcontractor =
        subcontractorValid.copy(
          subcontractorId = 2L,
          utr = Some(utr)
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractorToValidate,
        allSubcontractors = Seq(
          subcontractorToValidate,
          otherSubcontractor
        )
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = Some(utr)
          )
        )
    }

    "return a PartnerUtr failure when the partner UTR is invalid" in {
      val partnerUtr = "invalid-number"

      val subcontractor =
        subcontractorValid.copy(
          partnerUtr = Some(partnerUtr)
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractor,
        allSubcontractors = Seq(subcontractor)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.PartnerUtr,
            value = Some(partnerUtr)
          )
        )
    }

    "not check the partner UTR for duplicates" in {
      val partnerUtr = "5860920998"

      val subcontractorToValidate =
        subcontractorValid.copy(
          subcontractorId = 1L,
          utr = Some("1111111111"),
          partnerUtr = Some(partnerUtr)
        )

      val firstSubcontractor =
        subcontractorValid.copy(
          subcontractorId = 2L,
          utr = Some(partnerUtr)
        )

      val secondSubcontractor =
        subcontractorValid.copy(
          subcontractorId = 3L,
          utr = Some(partnerUtr)
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractorToValidate,
        allSubcontractors = Seq(
          subcontractorToValidate,
          firstSubcontractor,
          secondSubcontractor
        )
      ) mustBe Nil
    }

    "return a NINO failure when the NINO is invalid" in {
      val nino = "123456789"

      val subcontractor =
        subcontractorValid.copy(
          nino = Some(nino)
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractor,
        allSubcontractors = Seq(subcontractor)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.Nino,
            value = Some(nino)
          )
        )
    }

    "return a works reference number failure when the works reference number is invalid" in {
      val worksReferenceNumber =
        "A12323452345#@[]{}$%^&£~"

      val subcontractor =
        subcontractorValid.copy(
          worksReferenceNumber = Some(worksReferenceNumber)
        )

      PartnershipValidator.validate(
        subcontractorToValidate = subcontractor,
        allSubcontractors = Seq(subcontractor)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.WorksReferenceNumber,
            value = Some(worksReferenceNumber)
          )
        )
    }
  }

  private def subcontractorValid: Subcontractor =
    subcontractor(
      utr = Some("5860920998"),
      partnerUtr = Some("5860920998"),
      crn = Some("AB5860"),
      partnershipTradingName = Some("Smith & Partners"),
      tradingName = Some("Smith Builders"),
      nino = Some("AA123456A"),
      worksReferenceNumber = None
    )

  private def subcontractor(
    utr: Option[String] = None,
    partnerUtr: Option[String] = None,
    crn: Option[String] = None,
    partnershipTradingName: Option[String] = None,
    tradingName: Option[String] = None,
    nino: Option[String] = None,
    worksReferenceNumber: Option[String] = None
  ): Subcontractor =
    Subcontractor(
      subcontractorId = 1L,
      utr = utr,
      pageVisited = Some(1),
      partnerUtr = partnerUtr,
      crn = crn,
      firstName = None,
      nino = nino,
      secondName = None,
      surname = None,
      partnershipTradingName = partnershipTradingName,
      tradingName = tradingName,
      subcontractorType = None,
      addressLine1 = None,
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      country = None,
      postCode = None,
      emailAddress = None,
      phoneNumber = None,
      mobilePhoneNumber = None,
      worksReferenceNumber = worksReferenceNumber,
      createDate = None,
      lastUpdate = None,
      subbieResourceRef = None,
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
      displayName = None
    )
}
