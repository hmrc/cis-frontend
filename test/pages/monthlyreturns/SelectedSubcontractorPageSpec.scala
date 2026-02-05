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

  private val index = 0

  "SelectedSubcontractorIdPage" - {

    "have the correct path" in {
      SelectedSubcontractorIdPage(index).path mustBe (JsPath \ "subcontractors" \ index \ "subcontractorId")
    }

    "have the correct toString" in {
      SelectedSubcontractorIdPage(index).toString mustBe "subcontractorId"
    }
  }

  "SelectedSubcontractorNamePage" - {

    "have the correct path" in {
      SelectedSubcontractorNamePage(index).path mustBe (JsPath \ "subcontractors" \ index \ "name")
    }

    "have the correct toString" in {
      SelectedSubcontractorNamePage(index).toString mustBe "name"
    }
  }

  "SelectedSubcontractorPaymentsMadePage" - {

    "have the correct path" in {
      SelectedSubcontractorPaymentsMadePage(index).path mustBe (JsPath \ "subcontractors" \ index \ "paymentsMade")
    }

    "have the correct toString" in {
      SelectedSubcontractorPaymentsMadePage(index).toString mustBe "paymentsMade"
    }
  }

  "SelectedSubcontractorMaterialCostsPage" - {

    "have the correct path" in {
      SelectedSubcontractorMaterialCostsPage(index).path mustBe (JsPath \ "subcontractors" \ index \ "materialCosts")
    }

    "have the correct toString" in {
      SelectedSubcontractorMaterialCostsPage(index).toString mustBe "materialCosts"
    }
  }

  "SelectedSubcontractorTaxDeductedPage" - {

    "have the correct path" in {
      SelectedSubcontractorTaxDeductedPage(index).path mustBe (JsPath \ "subcontractors" \ index \ "taxDeducted")
    }

    "have the correct toString" in {
      SelectedSubcontractorTaxDeductedPage(index).toString mustBe "taxDeducted"
    }
  }

  "SelectedSubcontractorPage" - {

    "have the correct path" in {
      SelectedSubcontractorPage(index).path mustBe (JsPath \ "subcontractors" \ index)
    }

    "have the correct toString" in {
      SelectedSubcontractorPage(index).toString mustBe "subcontractor-0"
    }
  }

  "SelectedSubcontractorPage.all" - {

    "have the correct path" in {
      SelectedSubcontractorPage.all.path mustBe (JsPath \ "subcontractors")
    }

    "have the correct toString" in {
      SelectedSubcontractorPage.all.toString mustBe "subcontractors"
    }
  }
}