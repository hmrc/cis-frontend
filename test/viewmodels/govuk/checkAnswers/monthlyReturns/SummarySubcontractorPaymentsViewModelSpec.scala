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

package viewmodels.checkAnswers.monthlyreturns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers

class SummarySubcontractorPaymentsViewModelSpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = Helpers.stubMessages()

  "SummarySubcontractorPaymentsViewModel" - {

    "must produce exactly 3 rows" in {
      val vm = SummarySubcontractorPaymentsViewModel(4, BigDecimal(3600), BigDecimal(900), BigDecimal(540))
      vm.rows.size mustBe 3
    }

    "must set subcontractorCount correctly" in {
      val vm = SummarySubcontractorPaymentsViewModel(4, BigDecimal(3600), BigDecimal(900), BigDecimal(540))
      vm.subcontractorCount mustBe 4
    }

    "must format totalPayments with currency symbol in the first row value" in {
      val vm = SummarySubcontractorPaymentsViewModel(1, BigDecimal(500), BigDecimal(0), BigDecimal(0))
      vm.rows.head.value.content.asHtml.toString must include("£500")
    }

    "must format totalMaterialsCost with currency symbol in the second row value" in {
      val vm = SummarySubcontractorPaymentsViewModel(1, BigDecimal(0), BigDecimal(200.50), BigDecimal(0))
      vm.rows(1).value.content.asHtml.toString must include("£200.50")
    }

    "must format totalCisDeductions with currency symbol in the third row value" in {
      val vm = SummarySubcontractorPaymentsViewModel(1, BigDecimal(0), BigDecimal(0), BigDecimal(540))
      vm.rows(2).value.content.asHtml.toString must include("£540")
    }

    "must use the correct message keys for row labels" in {
      val vm = SummarySubcontractorPaymentsViewModel(1, BigDecimal(100), BigDecimal(200), BigDecimal(300))

      vm.rows.head.key.content.asHtml.toString must include(
        "monthlyreturns.summarySubcontractorPayments.totalPayments.label"
      )
      vm.rows(1).key.content.asHtml.toString must include(
        "monthlyreturns.summarySubcontractorPayments.totalMaterialsCost.label"
      )
      vm.rows(2).key.content.asHtml.toString must include(
        "monthlyreturns.summarySubcontractorPayments.totalCisDeductions.label"
      )
    }

    "must produce no action links on any row" in {
      val vm = SummarySubcontractorPaymentsViewModel(2, BigDecimal(500), BigDecimal(100), BigDecimal(80))
      vm.rows.foreach(row => row.actions mustBe None)
    }

    "must handle zero values" in {
      val vm = SummarySubcontractorPaymentsViewModel(0, BigDecimal(0), BigDecimal(0), BigDecimal(0))
      vm.rows.head.value.content.asHtml.toString  must include("£0")
      vm.rows(1).value.content.asHtml.toString    must include("£0")
      vm.rows(2).value.content.asHtml.toString    must include("£0")
    }

    "must format decimal values correctly" in {
      val vm = SummarySubcontractorPaymentsViewModel(1, BigDecimal(999.99), BigDecimal(500.50), BigDecimal(100.25))
      vm.rows.head.value.content.asHtml.toString must include("£999.99")
      vm.rows(1).value.content.asHtml.toString   must include("£500.50")
      vm.rows(2).value.content.asHtml.toString   must include("£100.25")
    }
  }
}
