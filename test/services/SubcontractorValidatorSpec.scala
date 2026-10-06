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
import models.validation.*
import models.monthlyreturns.Subcontractor
import models.validation.SubcontractorValidationField.*
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock

class SubcontractorValidatorSpec extends SpecBase {

  private val subcontractorDetailsValidator     = mock[SubcontractorDetailsValidator]
  private val subcontractorCompanyValidator     = mock[SubcontractorCompanyValidator]
  private val subcontractorIndividualValidator  = mock[SubcontractorIndividualValidator]
  private val subcontractorPartnershipValidator = mock[SubcontractorPartnershipValidator]
  private val subcontractorTrustValidator       = mock[SubcontractorTrustValidator]

  private val validator =
    new SubcontractorValidator(
      subcontractorDetailsValidator,
      subcontractorCompanyValidator,
      subcontractorIndividualValidator,
      subcontractorPartnershipValidator,
      subcontractorTrustValidator
    )

  "SubcontractorValidator.validate" - {

    "return no failures when all validators return no failures" in {
      val subcontractor =
        validSubcontractor()

      val subcontractors =
        Seq(subcontractor)

      stubEmpty(subcontractors)

      validator.validate(subcontractors) mustBe Nil
    }

    "merge failures returned by the validators for the same subcontractor" in {
      val subcontractor =
        validSubcontractor()

      val subcontractors =
        Seq(subcontractor)

      val utrFailure =
        FieldValidationFailure(
          field = Utr,
          value = Some("1234567890")
        )

      val tradingNameFailure =
        FieldValidationFailure(
          field = TradingName,
          value = Some("invalid")
        )

      when(
        subcontractorDetailsValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorCompanyValidator.validate(subcontractors)
      ).thenReturn(
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(utrFailure)
          )
        )
      )

      when(
        subcontractorIndividualValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorPartnershipValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorTrustValidator.validate(subcontractors)
      ).thenReturn(
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(tradingNameFailure)
          )
        )
      )

      validator.validate(subcontractors) mustBe
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              utrFailure,
              tradingNameFailure
            )
          )
        )
    }

    "throw when a subcontractor has an invalid subcontractor type" in {
      val subcontractor =
        mock[Subcontractor]

      when(subcontractor.subcontractorType)
        .thenReturn(Some("invalid"))

      val exception =
        intercept[IllegalStateException] {
          validator.validate(
            Seq(subcontractor)
          )
        }

      exception.getMessage mustBe
        "Invalid subcontractorType: Some(invalid)"
    }

    "throw when a subcontractor has no subcontractor type" in {
      val subcontractor =
        mock[Subcontractor]

      when(subcontractor.subcontractorType)
        .thenReturn(None)

      val exception =
        intercept[IllegalStateException] {
          validator.validate(
            Seq(subcontractor)
          )
        }

      exception.getMessage mustBe
        "Invalid subcontractorType: None"
    }
  }

  "SubcontractorValidator.validateFields" - {

    "return distinct failed fields grouped by subcontractor id" in {
      val subcontractor =
        validSubcontractor()

      val subcontractors =
        Seq(subcontractor)

      val failures =
        List(
          FieldValidationFailure(
            field = Utr,
            value = Some("1234567890")
          ),
          FieldValidationFailure(
            field = Utr,
            value = Some("1234567890")
          ),
          FieldValidationFailure(
            field = TradingName,
            value = Some("invalid")
          )
        )

      when(
        subcontractorDetailsValidator.validate(subcontractors)
      ).thenReturn(
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = failures
          )
        )
      )

      when(
        subcontractorCompanyValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorIndividualValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorPartnershipValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorTrustValidator.validate(subcontractors)
      ).thenReturn(Nil)

      validator.validateFields(subcontractors) mustBe
        Map(
          1L -> Seq(
            Utr,
            TradingName
          )
        )
    }
  }

  "SubcontractorValidator.validateFieldsFor" - {

    "return failed fields for the requested subcontractor" in {
      val subcontractor =
        validSubcontractor()

      val subcontractors =
        Seq(subcontractor)

      when(
        subcontractorDetailsValidator.validate(subcontractors)
      ).thenReturn(
        List(
          SubcontractorValidationFailure(
            subcontractorId = 1L,
            failedFields = List(
              FieldValidationFailure(
                field = Utr,
                value = Some("1234567890")
              )
            )
          )
        )
      )

      when(
        subcontractorCompanyValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorIndividualValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorPartnershipValidator.validate(subcontractors)
      ).thenReturn(Nil)

      when(
        subcontractorTrustValidator.validate(subcontractors)
      ).thenReturn(Nil)

      validator.validateFieldsFor(
        subcontractorId = 1L,
        subcontractors = subcontractors
      ) mustBe Seq(Utr)
    }

    "return empty when the requested subcontractor has no failed fields" in {
      val subcontractor =
        validSubcontractor()

      val subcontractors =
        Seq(subcontractor)

      stubEmpty(subcontractors)

      validator.validateFieldsFor(
        subcontractorId = 999L,
        subcontractors = subcontractors
      ) mustBe Seq.empty
    }
  }

  private def validSubcontractor(): Subcontractor = {
    val subcontractor =
      mock[Subcontractor]

    when(subcontractor.subcontractorType)
      .thenReturn(Some("company"))

    subcontractor
  }

  private def stubEmpty(
    subcontractors: Seq[Subcontractor]
  ): Unit = {
    when(
      subcontractorDetailsValidator.validate(subcontractors)
    ).thenReturn(Nil)

    when(
      subcontractorCompanyValidator.validate(subcontractors)
    ).thenReturn(Nil)

    when(
      subcontractorIndividualValidator.validate(subcontractors)
    ).thenReturn(Nil)

    when(
      subcontractorPartnershipValidator.validate(subcontractors)
    ).thenReturn(Nil)

    when(
      subcontractorTrustValidator.validate(subcontractors)
    ).thenReturn(Nil)
  }
}
