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
import models.monthlyreturns.SelectedSubcontractor
import play.api.libs.json.JsPath

class SelectedSubcontractorPageSpec extends SpecBase {

  private val index = 0

  "SelectedSubcontractorIdPage" - {

    "have the correct path" in {
      SelectedSubcontractorIdPage(index).path mustBe (JsPath \ "subcontractors" \ index.toString \ "subcontractorId")
    }

    "have the correct toString" in {
      SelectedSubcontractorIdPage(index).toString mustBe "subcontractorId"
    }
  }

  "SelectedSubcontractorNamePage" - {

    "have the correct path" in {
      SelectedSubcontractorNamePage(index).path mustBe (JsPath \ "subcontractors" \ index.toString \ "name")
    }

    "have the correct toString" in {
      SelectedSubcontractorNamePage(index).toString mustBe "name"
    }
  }

  "SelectedSubcontractorPaymentsMadePage" - {

    "have the correct path" in {
      SelectedSubcontractorPaymentsMadePage(
        index
      ).path mustBe (JsPath \ "subcontractors" \ index.toString \ "totalPaymentsMade")
    }

    "have the correct toString" in {
      SelectedSubcontractorPaymentsMadePage(index).toString mustBe "totalPaymentsMade"
    }
  }

  "SelectedSubcontractorMaterialCostsPage" - {

    "have the correct path" in {
      SelectedSubcontractorMaterialCostsPage(
        index
      ).path mustBe (JsPath \ "subcontractors" \ index.toString \ "costOfMaterials")
    }

    "have the correct toString" in {
      SelectedSubcontractorMaterialCostsPage(index).toString mustBe "costOfMaterials"
    }
  }

  "SelectedSubcontractorTaxDeductedPage" - {

    "have the correct path" in {
      SelectedSubcontractorTaxDeductedPage(
        index
      ).path mustBe (JsPath \ "subcontractors" \ index.toString \ "totalTaxDeducted")
    }

    "have the correct toString" in {
      SelectedSubcontractorTaxDeductedPage(index).toString mustBe "totalTaxDeducted"
    }
  }

  "SelectedSubcontractorPage" - {

    "have the correct path" in {
      SelectedSubcontractorPage(index).path mustBe (JsPath \ "subcontractors" \ index.toString)
    }

    "have the correct toString" in {
      SelectedSubcontractorPage(index).toString mustBe "subcontractor-0"
    }
  }

  "SelectedSubcontractorForFlowPage" - {

    "have the correct path" in {
      SelectedSubcontractorForFlowPage(
        "test-id",
        index
      ).path mustBe (JsPath \ "monthlyReturn" \ "test-id" \ "subcontractors" \ index.toString)
    }

    "have the correct toString" in {
      SelectedSubcontractorForFlowPage("test-id", index).toString mustBe "subcontractor-0"
    }
  }

  "SelectedSubcontractorPage.all" - {

    "have the correct path" in {
      SelectedSubcontractorPage.all.path mustBe (JsPath \ "subcontractors")
    }

    "have the correct toString" in {
      SelectedSubcontractorPage.all.toString mustBe "subcontractors"
    }

    "have the correct path with id" in {
      SelectedSubcontractorPage.all("test-id").path mustBe (JsPath \ "monthlyReturn" \ "test-id" \ "subcontractors")
    }

    "have the correct toString with id" in {
      SelectedSubcontractorPage.all("test-id").toString mustBe "subcontractors"
    }
  }

  "SelectedSubcontractorForFlowPage" - {

    val id = "org_flow-1"

    "have the correct scoped path" in {
      SelectedSubcontractorForFlowPage(id, index).path mustBe
        (JsPath \ "monthlyReturn" \ id \ "subcontractors" \ index.toString)
    }

    "use different paths for different journeys" in {
      SelectedSubcontractorForFlowPage("org_flow-1", index).path must not be
        SelectedSubcontractorForFlowPage("org_flow-2", index).path
    }

    "use different paths for different indexes" in {
      SelectedSubcontractorForFlowPage(id, 1).path must not be
        SelectedSubcontractorForFlowPage(id, 2).path
    }

    "have the same toString as the legacy page" in {
      SelectedSubcontractorForFlowPage(id, index).toString mustBe "subcontractor-0"
      SelectedSubcontractorForFlowPage(id, 3).toString mustBe SelectedSubcontractorPage(3).toString
    }

    "retain the legacy unscoped paths" in {
      SelectedSubcontractorPage(index).path mustBe (JsPath \ "subcontractors" \ index.toString)
      SelectedSubcontractorPage.all.path mustBe (JsPath \ "subcontractors")
    }
  }

  "SelectedSubcontractorForFlowPage.all" - {

    val id = "org_flow-1"

    "have the correct scoped path" in {
      SelectedSubcontractorPage.all(id).path mustBe (JsPath \ "monthlyReturn" \ id \ "subcontractors")
    }

    "use different paths for different journeys" in {
      SelectedSubcontractorPage.all("org_flow-1").path must not be
        SelectedSubcontractorPage.all("org_flow-2").path
    }

    "have the correct toString" in {
      SelectedSubcontractorPage.all(id).toString mustBe "subcontractors"
    }
  }

  "SelectedSubcontractorForFlowPage with UserAnswers" - {

    val firstId       = "org_flow-1"
    val secondId      = "org_flow-2"
    val subcontractor =
      SelectedSubcontractor(
        id = 1L,
        name = "Test Subcontractor",
        totalPaymentsMade = Some(BigDecimal(100)),
        costOfMaterials = Some(BigDecimal(20)),
        totalTaxDeducted = Some(BigDecimal(10))
      )

    "store and retrieve a subcontractor under the scoped path" in {
      val ua = emptyUserAnswers
        .set(SelectedSubcontractorForFlowPage(firstId, 1), subcontractor)
        .success
        .value

      ua.get(SelectedSubcontractorForFlowPage(firstId, 1)) mustBe Some(subcontractor)
      ua.get(SelectedSubcontractorPage.all(firstId)).exists(_.nonEmpty) mustBe true
    }

    "not write to the legacy unscoped keys" in {
      val ua = emptyUserAnswers
        .set(SelectedSubcontractorForFlowPage(firstId, 1), subcontractor)
        .success
        .value

      ua.get(SelectedSubcontractorPage(1)) mustBe None
      ua.get(SelectedSubcontractorPage.all) mustBe None
    }

    "not read answers belonging to another journey" in {
      val ua = emptyUserAnswers
        .set(SelectedSubcontractorForFlowPage(firstId, 1), subcontractor)
        .success
        .value

      ua.get(SelectedSubcontractorForFlowPage(secondId, 1)) mustBe None
      ua.get(SelectedSubcontractorPage.all(secondId)).exists(_.nonEmpty) mustBe false
    }

    "clear only the scoped subcontractors when removing all" in {
      val ua = emptyUserAnswers
        .set(SelectedSubcontractorForFlowPage(firstId, 1), subcontractor)
        .success
        .value
        .set(SelectedSubcontractorForFlowPage(secondId, 1), subcontractor)
        .success
        .value
        .remove(SelectedSubcontractorPage.all(firstId))
        .success
        .value

      ua.get(SelectedSubcontractorForFlowPage(firstId, 1)) mustBe None
      ua.get(SelectedSubcontractorForFlowPage(secondId, 1)) mustBe Some(subcontractor)
    }
  }
}
