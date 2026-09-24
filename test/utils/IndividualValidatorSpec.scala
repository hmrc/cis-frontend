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

class IndividualValidatorSpec extends AnyWordSpec with Matchers {

  "IndividualValidator.validate" must {

    "return no failures when all fields are valid" in {
      IndividualValidator.validate(
        subcontractorToValidate = subcontractorValid,
        allSubcontractors = Seq(subcontractorValid)
      ) mustBe Nil
    }

    "return every failure" in {
      IndividualValidator.validate(
        subcontractorToValidate = subcontractorInvalid,
        allSubcontractors = Seq(subcontractorInvalid)
      ) mustBe
        List(
          FieldValidationFailure(
            field = SubcontractorValidationField.Surname,
            value = subcontractorInvalid.surname
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.FirstName,
            value = subcontractorInvalid.firstName
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.SecondName,
            value = subcontractorInvalid.secondName
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.TradingName,
            value = subcontractorInvalid.tradingName
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.Utr,
            value = subcontractorInvalid.utr
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.Nino,
            value = subcontractorInvalid.nino
          ),
          FieldValidationFailure(
            field = SubcontractorValidationField.WorksReferenceNumber,
            value = subcontractorInvalid.worksReferenceNumber
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

      IndividualValidator.validate(
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
  }

  private def subcontractorValid: Subcontractor =
    subcontractor(
      firstName = Some("John"),
      secondName = Some("Michael"),
      surname = Some("Smith"),
      tradingName = Some("John Smith Builders"),
      utr = Some("5860920998"),
      nino = Some("AA123456A"),
      worksReferenceNumber = None
    )

  private def subcontractorInvalid: Subcontractor =
    subcontractor(
      firstName = Some("<invalid>"),
      secondName = Some("<invalid>"),
      surname = Some("<invalid>"),
      tradingName = Some("12345678901234567890123456789012345678901234567890<>"),
      utr = Some("12345A7890"),
      nino = Some("123456789"),
      worksReferenceNumber = Some("A12323452345#@[]{}$%^&£~")
    )

  private def subcontractor(
    firstName: Option[String] = None,
    secondName: Option[String] = None,
    surname: Option[String] = None,
    tradingName: Option[String] = None,
    utr: Option[String] = None,
    nino: Option[String] = None,
    worksReferenceNumber: Option[String] = None
  ): Subcontractor =
    Subcontractor(
      subcontractorId = 1L,
      utr = utr,
      pageVisited = Some(1),
      partnerUtr = None,
      crn = None,
      firstName = firstName,
      nino = nino,
      secondName = secondName,
      surname = surname,
      partnershipTradingName = None,
      tradingName = tradingName,
      subcontractorType = Some("soleTrader"),
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
