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
import models.UserAnswers
import play.api.libs.json.Json
import viewmodels.checkAnswers.monthlyreturns.SubcontractorDetailsAddedBuilder

import java.time.Instant

class SubcontractorDetailsAddedBuilderSpec extends SpecBase {

  "SubcontractorDetailsAddedBuilder.build" - {

    "must return Some(viewModel) with hasIncomplete=true, only display rows with details added, and single heading" in {
      val ua = UserAnswers(
        id = "test-user",
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId"  -> 1001L,
              "name"             -> "TyneWear Ltd",
              "totalPaymentMade" -> 1000.00,
              "costOfMaterials"  -> 200.00,
              "totalTaxDeducted" -> 200.00
            ),
            Json.obj(
              "subcontractorId"  -> 1002L,
              "name"             -> "Northern Trades Ltd"
            ),
            Json.obj(
              "subcontractorId"  -> 1003L,
              "name"             -> "BuildRight Construction"
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val vmOpt = SubcontractorDetailsAddedBuilder.build(ua)
      vmOpt mustBe defined

      val vm = vmOpt.value
      vm.hasIncomplete mustBe true

      vm.rows.size mustBe 1
      vm.rows.head.name mustBe "TyneWear Ltd"
      vm.rows.head.detailsAdded mustBe true

      vm.headingKey mustBe "monthlyreturns.subcontractorDetailsAdded.heading.single"
      vm.headingArgs mustBe empty
    }

    "must return Some(viewModel) with hasIncomplete=false and multiple heading when count > 1" in {
      val ua = UserAnswers(
        id = "test-user",
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId"  -> 2001L,
              "name"             -> "A Ltd",
              "totalPaymentMade" -> 1.0,
              "costOfMaterials"  -> 1.0,
              "totalTaxDeducted" -> 1.0
            ),
            Json.obj(
              "subcontractorId"  -> 2002L,
              "name"             -> "B Ltd",
              "totalPaymentMade" -> 2.0,
              "costOfMaterials"  -> 2.0,
              "totalTaxDeducted" -> 2.0
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val vmOpt = SubcontractorDetailsAddedBuilder.build(ua)
      vmOpt mustBe defined

      val vm = vmOpt.value
      vm.hasIncomplete mustBe false
      vm.rows.size mustBe 2

      vm.headingKey mustBe "monthlyreturns.subcontractorDetailsAdded.heading.multiple"
      vm.headingArgs mustBe Seq(Integer.valueOf(2))
    }

    "must return None when zero subcontractors have details added" in {
      val ua = UserAnswers(
        id = "test-user",
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj("subcontractorId" -> 1L, "name" -> "Incomplete 1"),
            Json.obj("subcontractorId" -> 2L, "name" -> "Incomplete 2")
          )
        ),
        lastUpdated = Instant.now
      )

      SubcontractorDetailsAddedBuilder.build(ua) mustBe None
    }
  }

  "SubcontractorDetailsAddedBuilder.allSelectedHaveDetails" - {

    "must return false when at least one selected subcontractor is missing details" in {
      val ua = UserAnswers(
        id = "test-user",
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId"  -> 1L,
              "name"             -> "Complete",
              "totalPaymentMade" -> 1.0,
              "costOfMaterials"  -> 1.0,
              "totalTaxDeducted" -> 1.0
            ),
            Json.obj(
              "subcontractorId"  -> 2L,
              "name"             -> "Incomplete"
            )
          )
        ),
        lastUpdated = Instant.now
      )

      SubcontractorDetailsAddedBuilder.allSelectedHaveDetails(ua) mustBe false
    }
  }
}
