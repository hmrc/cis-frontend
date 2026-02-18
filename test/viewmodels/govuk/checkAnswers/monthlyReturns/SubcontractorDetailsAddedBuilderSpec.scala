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
import models.monthlyreturns.SelectedSubcontractor
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.libs.json.Json
import viewmodels.checkAnswers.monthlyreturns.{SubcontractorDetailsAddedBuilder, SubcontractorDetailsAddedViewModel}

import java.time.Instant

class SubcontractorDetailsAddedBuilderSpec extends SpecBase {

  private def baseUa: UserAnswers =
    UserAnswers(id = "test-user", data = Json.obj(), lastUpdated = Instant.now)

  private def uaWithSubcontractors(subs: (Int, SelectedSubcontractor)*): UserAnswers =
    subs.foldLeft(baseUa) { case (ua, (idx, sub)) =>
      ua.set(SelectedSubcontractorPage(idx), sub).getOrElse(ua)
    }

  private def completeSub(
    id: Long,
    name: String,
    payments: BigDecimal = 1,
    materials: BigDecimal = 1,
    tax: BigDecimal = 1
  ): SelectedSubcontractor =
    SelectedSubcontractor(
      id = id,
      name = name,
      totalPaymentsMade = Some(payments),
      costOfMaterials = Some(materials),
      totalTaxDeducted = Some(tax)
    )

  private def incompleteSub(id: Long, name: String): SelectedSubcontractor =
    SelectedSubcontractor(
      id = id,
      name = name,
      totalPaymentsMade = None,
      costOfMaterials = None,
      totalTaxDeducted = None
    )

  "SubcontractorDetailsAddedBuilder.build" - {

    "must return Some(viewModel) with hasIncomplete=true, only display completed rows, and single heading" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd", payments = 1000, materials = 200, tax = 200),
        2 -> incompleteSub(1002L, "Northern Trades Ltd"),
        3 -> incompleteSub(1003L, "BuildRight Construction")
      )

      val vm = SubcontractorDetailsAddedBuilder.build(ua).value

      vm.hasIncomplete mustBe true
      vm.rows.map(_.name) mustBe Seq("TyneWear Ltd")
      vm.rows.foreach(_.detailsAdded mustBe true)

      vm.headingKey mustBe "monthlyreturns.subcontractorDetailsAdded.heading.single"
      vm.headingArgs mustBe empty
    }

    "must return Some(viewModel) with hasIncomplete=false and multiple heading when more than one completed" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(2001L, "A Ltd"),
        2 -> completeSub(2002L, "B Ltd")
      )

      val vm = SubcontractorDetailsAddedBuilder.build(ua).value

      vm.hasIncomplete mustBe false
      vm.rows.map(_.name) mustBe Seq("A Ltd", "B Ltd")
      vm.rows.foreach(_.detailsAdded mustBe true)

      vm.headingKey mustBe "monthlyreturns.subcontractorDetailsAdded.heading.multiple"
      vm.headingArgs mustBe Seq(Integer.valueOf(2))
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
