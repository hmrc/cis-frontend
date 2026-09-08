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

package services.finalvalidation

import base.SpecBase
import models.finalvalidation.*
import models.monthlyreturns.Subcontractor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import services.SubcontractorValidator
import services.finalvalidation.FinalValidationService

class FinalValidationServiceSpec extends SpecBase {

  private val subcontractorValidator = mock[SubcontractorValidator]
  private val service                = new FinalValidationService(subcontractorValidator)

  "validate" - {

    "must return no failures when validation returns no failed fields" in {
      val subcontractor = mock[Subcontractor]

      when(subcontractor.subcontractorId).thenReturn(101L)
      when(
        subcontractorValidator.validateFields(
          Seq(subcontractor)
        )
      ).thenReturn(Map.empty)

      val result =
        service.validate(
          Seq(subcontractor)
        )

      result.failures mustBe Seq.empty
    }
  }

  "validateDraftSubcontractor" - {

    "must return no issues when validation returns no failed fields" in {
      val draft              = mock[FinalValidationDraft]
      val draftSubcontractor = mock[FinalValidationDraftSubcontractor]
      val proposed           = mock[FinalValidationSubcontractorDetails]

      when(draft.subcontractor(101L)).thenReturn(Some(draftSubcontractor))
      when(draft.subcontractors).thenReturn(Seq(draftSubcontractor))
      when(draftSubcontractor.subcontractorId).thenReturn(101L)
      when(draftSubcontractor.proposed).thenReturn(proposed)

      when(
        subcontractorValidator.validateFieldsFor(
          any[Long],
          any[Seq[Subcontractor]]
        )
      ).thenReturn(Seq.empty)

      service
        .validateDraftSubcontractor(
          draft,
          101L
        )
        .success
        .value mustBe Seq.empty
    }

    "must fail when the subcontractor is not in the draft" in {
      val draft = mock[FinalValidationDraft]

      when(draft.subcontractor(101L)).thenReturn(None)

      val result =
        service.validateDraftSubcontractor(
          draft,
          101L
        )

      result.failure.exception.getMessage mustBe
        "Subcontractor 101 not found in Final Validation draft"
    }
  }
}
