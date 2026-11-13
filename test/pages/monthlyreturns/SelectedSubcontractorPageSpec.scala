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

package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class SelectedSubcontractorPageSpec extends SpecBase {

  private val index  = 0
  private val testId = "testId"

  "SelectedSubcontractorIdPage" - {

    "have the correct path and toString" in {
      SelectedSubcontractorIdPage(index).path mustBe (JsPath \ "subcontractors" \ index.toString \ "subcontractorId")
      SelectedSubcontractorIdPage(index).toString mustBe "subcontractorId"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorIdPage(
        index,
        Some(testId)
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors" \ index.toString \ "subcontractorId")
      SelectedSubcontractorIdPage(index, Some(testId)).toString mustBe "subcontractorId"
    }
  }

  "SelectedSubcontractorNamePage" - {

    "have the correct path and toString" in {
      SelectedSubcontractorNamePage(index).path mustBe (JsPath \ "subcontractors" \ index.toString \ "name")
      SelectedSubcontractorNamePage(index).toString mustBe "name"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorNamePage(
        index,
        Some(testId)
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors" \ index.toString \ "name")
      SelectedSubcontractorNamePage(index, Some(testId)).toString mustBe "name"
    }
  }

  "SelectedSubcontractorPaymentsMadePage" - {

    "have the correct path and toString" in {
      SelectedSubcontractorPaymentsMadePage(
        index
      ).path mustBe (JsPath \ "subcontractors" \ index.toString \ "totalPaymentsMade")
      SelectedSubcontractorPaymentsMadePage(index).toString mustBe "totalPaymentsMade"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorPaymentsMadePage(
        index,
        Some(testId)
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors" \ index.toString \ "totalPaymentsMade")
      SelectedSubcontractorPaymentsMadePage(index, Some(testId)).toString mustBe "totalPaymentsMade"
    }
  }

  "SelectedSubcontractorMaterialCostsPage" - {

    "have the correct path and toString" in {
      SelectedSubcontractorMaterialCostsPage(
        index
      ).path mustBe (JsPath \ "subcontractors" \ index.toString \ "costOfMaterials")
      SelectedSubcontractorMaterialCostsPage(index).toString mustBe "costOfMaterials"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorMaterialCostsPage(
        index,
        Some(testId)
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors" \ index.toString \ "costOfMaterials")
      SelectedSubcontractorMaterialCostsPage(index, Some(testId)).toString mustBe "costOfMaterials"
    }
  }

  "SelectedSubcontractorTaxDeductedPage" - {

    "have the correct path and toString" in {
      SelectedSubcontractorTaxDeductedPage(
        index
      ).path mustBe (JsPath \ "subcontractors" \ index.toString \ "totalTaxDeducted")
      SelectedSubcontractorTaxDeductedPage(index).toString mustBe "totalTaxDeducted"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorTaxDeductedPage(
        index,
        Some(testId)
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors" \ index.toString \ "totalTaxDeducted")
      SelectedSubcontractorTaxDeductedPage(index, Some(testId)).toString mustBe "totalTaxDeducted"
    }
  }

  "SelectedSubcontractorPage" - {

    "have the correct path and toString" in {
      SelectedSubcontractorPage(index).path mustBe (JsPath \ "subcontractors" \ index.toString)
      SelectedSubcontractorPage(index).toString mustBe "subcontractor-0"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorPage(
        index,
        Some(testId)
      ).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors" \ index.toString)
      SelectedSubcontractorPage(index, Some(testId)).toString mustBe "subcontractor-0"
    }
  }

  "SelectedSubcontractorPage.all" - {

    "have the correct path and toString" in {
      SelectedSubcontractorPage.all.path mustBe (JsPath \ "subcontractors")
      SelectedSubcontractorPage.all.toString mustBe "subcontractors"
    }

    "have the correct path and toString with id" in {
      SelectedSubcontractorPage.all(Some(testId)).path mustBe (JsPath \ "monthlyReturn" \ testId \ "subcontractors")
      SelectedSubcontractorPage.all(Some(testId)).toString mustBe "subcontractors"
    }
  }
}
