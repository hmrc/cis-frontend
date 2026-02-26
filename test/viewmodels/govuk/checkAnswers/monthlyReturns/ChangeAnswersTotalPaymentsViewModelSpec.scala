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

package viewmodels.govuk.checkAnswers.monthlyReturns

import base.SpecBase
import models.monthlyreturns.SelectedSubcontractor
import viewmodels.checkAnswers.monthlyreturns.ChangeAnswersTotalPaymentsViewModel

class ChangeAnswersTotalPaymentsViewModelSpec extends SpecBase {

  "ChangeAnswersTotalPaymentsViewModel.fromModel" - {

    "must map all fields when values are present" in {

      val subcontractor = SelectedSubcontractor(
        id = 123L,
        name = "Tyne Test Ltd",
        totalPaymentsMade = Some(1200),
        costOfMaterials = Some(500),
        totalTaxDeducted = Some(240)
      )

      val result = ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor)

      result.id mustBe 123L
      result.name mustBe "Tyne Test Ltd"
      result.totalPaymentsMade mustBe "1200"
      result.costOfMaterials mustBe "500"
      result.totalTaxDeducted mustBe "240"
    }

    "must map missing optional values to empty strings" in {

      val subcontractor = SelectedSubcontractor(
        id = 1L,
        name = "Tyne Test Ltd",
        totalPaymentsMade = None,
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val result = ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor)

      result.id mustBe 1L
      result.name mustBe "Tyne Test Ltd"
      result.totalPaymentsMade mustBe ""
      result.costOfMaterials mustBe ""
      result.totalTaxDeducted mustBe ""
    }

    "must handle a mix of present and missing optional values" in {

      val subcontractor = SelectedSubcontractor(
        id = 42L,
        name = "Mixed Co",
        totalPaymentsMade = Some(999),
        costOfMaterials = None,
        totalTaxDeducted = Some(111)
      )

      val result = ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor)

      result.id mustBe 42L
      result.name mustBe "Mixed Co"
      result.totalPaymentsMade mustBe "999"
      result.costOfMaterials mustBe ""
      result.totalTaxDeducted mustBe "111"
    }

    "must preserve zeros when provided" in {

      val subcontractor = SelectedSubcontractor(
        id = 2L,
        name = "Zero Co",
        totalPaymentsMade = Some(0),
        costOfMaterials = Some(0),
        totalTaxDeducted = Some(0)
      )

      val result = ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor)

      result.totalPaymentsMade mustBe "0"
      result.costOfMaterials mustBe "0"
      result.totalTaxDeducted mustBe "0"
    }
  }
}
