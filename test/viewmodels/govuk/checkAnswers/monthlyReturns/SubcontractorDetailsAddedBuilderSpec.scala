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
import play.api.libs.json.{JsObject, Json}
import viewmodels.checkAnswers.monthlyreturns.{SubcontractorDetailsAddedBuilder, SubcontractorDetailsAddedViewModel}

import java.time.Instant

class SubcontractorDetailsAddedBuilderSpec extends SpecBase {

  private val now: Instant = Instant.now

  private def uaWithSubcontractors(subs: (Int, JsObject)*): UserAnswers =
    UserAnswers(
      id = "test-user",
      data = Json.obj(
        "subcontractors" -> JsObject(
          subs.map { case (idx, obj) => idx.toString -> obj }
        )
      ),
      lastUpdated = now
    )

  private def completeSub(
    id: Long,
    name: String,
    payments: BigDecimal = 1,
    materials: BigDecimal = 1,
    tax: BigDecimal = 1
  ): JsObject =
    Json.obj(
      "subcontractorId"   -> id,
      "name"              -> name,
      "totalPaymentsMade" -> payments,
      "costOfMaterials"   -> materials,
      "totalTaxDeducted"  -> tax
    )

  private def incompleteSub(id: Long, name: String): JsObject =
    Json.obj(
      "subcontractorId" -> id,
      "name"            -> name
    )

  private def assertHeadingSingle(vm: SubcontractorDetailsAddedViewModel): Unit = {
    vm.headingKey mustBe "monthlyreturns.subcontractorDetailsAdded.heading.single"
    vm.headingArgs mustBe empty
  }

  private def assertHeadingMultiple(vm: SubcontractorDetailsAddedViewModel, count: Int): Unit = {
    vm.headingKey mustBe "monthlyreturns.subcontractorDetailsAdded.heading.multiple"
    vm.headingArgs mustBe Seq(Integer.valueOf(count))
  }

  "SubcontractorDetailsAddedBuilder.build" - {

    "must return Some(viewModel) with hasIncomplete=true, only display rows with details added, and single heading" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd", payments = 1000, materials = 200, tax = 200),
        2 -> incompleteSub(1002L, "Northern Trades Ltd"),
        3 -> incompleteSub(1003L, "BuildRight Construction")
      )

      val vm = SubcontractorDetailsAddedBuilder.build(ua).value

      vm.hasIncomplete mustBe true
      vm.rows.map(_.name) mustBe Seq("TyneWear Ltd")
      vm.rows.foreach(_.detailsAdded mustBe true)

      assertHeadingSingle(vm)
    }

    "must return Some(viewModel) with hasIncomplete=false and multiple heading when count > 1" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(2001L, "A Ltd", payments = 1, materials = 1, tax = 1),
        2 -> completeSub(2002L, "B Ltd", payments = 2, materials = 2, tax = 2)
      )

      val vm = SubcontractorDetailsAddedBuilder.build(ua).value

      vm.hasIncomplete mustBe false
      vm.rows.map(_.name) mustBe Seq("A Ltd", "B Ltd")
      vm.rows.foreach(_.detailsAdded mustBe true)

      assertHeadingMultiple(vm, 2)
    }

    "must return None when zero subcontractors have details added" in {
      val ua = uaWithSubcontractors(
        1 -> incompleteSub(1L, "Incomplete 1"),
        2 -> incompleteSub(2L, "Incomplete 2")
      )

      SubcontractorDetailsAddedBuilder.build(ua) mustBe None
    }
  }
}
