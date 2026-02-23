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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

case class SummarySubcontractorPaymentsViewModel(
  subcontractorCount: Int,
  rows: Seq[SummaryListRow]
)

object SummarySubcontractorPaymentsViewModel {

  private def formatAmount(amt: BigDecimal): String =
    f"£$amt%.2f".replace(".00", "")

  def apply(
    subcontractorCount: Int,
    totalPayments: BigDecimal,
    totalMaterialsCost: BigDecimal,
    totalCisDeductions: BigDecimal
  )(implicit messages: Messages): SummarySubcontractorPaymentsViewModel =
    SummarySubcontractorPaymentsViewModel(
      subcontractorCount = subcontractorCount,
      rows = Seq(
        SummaryListRowViewModel(
          key   = "monthlyreturns.summarySubcontractorPayments.totalPayments.label",
          value = ValueViewModel(Text(formatAmount(totalPayments)))
        ),
        SummaryListRowViewModel(
          key   = "monthlyreturns.summarySubcontractorPayments.totalMaterialsCost.label",
          value = ValueViewModel(Text(formatAmount(totalMaterialsCost)))
        ),
        SummaryListRowViewModel(
          key   = "monthlyreturns.summarySubcontractorPayments.totalCisDeductions.label",
          value = ValueViewModel(Text(formatAmount(totalCisDeductions)))
        )
      )
    )
}
