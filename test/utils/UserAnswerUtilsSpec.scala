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

import base.SpecBase
import models.UserAnswers
import models.monthlyreturns.SelectedSubcontractor
import pages.monthlyreturns.SelectedSubcontractorPage
import utils.UserAnswerUtils.*

class UserAnswerUtilsSpec extends SpecBase {

  "UserAnswerUtils.firstIncompleteSubcontractorIndex" - {

    "returns 1 when there are no subcontractors" in {
      UserAnswers("id").firstIncompleteSubcontractorIndex mustBe 1
    }

    "returns 1 when all subcontractors are complete" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(0), completeSub(0))
        .setOrException(SelectedSubcontractorPage(1), completeSub(1))

      ua.firstIncompleteSubcontractorIndex mustBe 1
    }

    "returns the index of the single incomplete subcontractor" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(3), incompleteSub(3))

      ua.firstIncompleteSubcontractorIndex mustBe 3
    }

    "returns the minimum index among multiple incomplete subcontractors" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(0), completeSub(0))
        .setOrException(SelectedSubcontractorPage(2), incompleteSub(2))
        .setOrException(SelectedSubcontractorPage(5), incompleteSub(5))

      ua.firstIncompleteSubcontractorIndex mustBe 2
    }
  }

  private def completeSub(index: Int) = SelectedSubcontractor(
    id = index.toLong,
    name = s"Sub $index",
    totalPaymentsMade = Some(100),
    costOfMaterials = Some(50),
    totalTaxDeducted = Some(20)
  )

  private def incompleteSub(index: Int) = SelectedSubcontractor(
    id = index.toLong,
    name = s"Sub $index",
    totalPaymentsMade = None,
    costOfMaterials = None,
    totalTaxDeducted = None
  )
}