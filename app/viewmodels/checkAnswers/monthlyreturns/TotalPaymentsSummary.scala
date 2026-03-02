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

import models.CheckMode
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*

object TotalPaymentsSummary {

  def rowsForCheckAnswers(subcontractor: CheckAnswersTotalPaymentsViewModel, index: Int)(implicit
    messages: Messages
  ): Seq[SummaryListRow] =
    Seq(
      row(
        labelKey = "monthlyreturns.checkAnswersTotalPayments.details.totalPaymentsMadeToSubcontractors",
        displayValue = messages("currency.pounds", subcontractor.totalPaymentsMade),
        changeUrl = controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(CheckMode, index, None).url,
        actionId = None
      ),
      row(
        labelKey = "monthlyreturns.checkAnswersTotalPayments.details.totalCostOfMaterials",
        displayValue = messages("currency.pounds", subcontractor.costOfMaterials),
        changeUrl = controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoad(CheckMode, index, None).url,
        actionId = None
      ),
      row(
        labelKey = "monthlyreturns.checkAnswersTotalPayments.details.totalCisDeductions",
        displayValue = messages("currency.pounds", subcontractor.totalTaxDeducted),
        changeUrl = controllers.monthlyreturns.routes.TotalTaxDeductedController.onPageLoad(CheckMode, index, None).url,
        actionId = None
      )
    )

  def rowsForChangeAnswers(subcontractor: ChangeAnswersTotalPaymentsViewModel, index: Int)(implicit
    messages: Messages
  ): Seq[SummaryListRow] =
    Seq(
      row(
        labelKey = "monthlyreturns.changeAnswersTotalPayments.details.totalPaymentsMadeToSubcontractors",
        displayValue = messages("currency.pounds", subcontractor.totalPaymentsMade),
        changeUrl = controllers.monthlyreturns.routes.PaymentDetailsController
          .onPageLoad(CheckMode, index, Some("changeAnswers"))
          .url,
        actionId = Some("total-payment-made")
      ),
      row(
        labelKey = "monthlyreturns.changeAnswersTotalPayments.details.totalCostOfMaterials",
        displayValue = messages("currency.pounds", subcontractor.costOfMaterials),
        changeUrl = controllers.monthlyreturns.routes.CostOfMaterialsController
          .onPageLoad(CheckMode, index, Some("changeAnswers"))
          .url,
        actionId = Some("total-cost-of-materials")
      ),
      row(
        labelKey = "monthlyreturns.changeAnswersTotalPayments.details.totalCisDeductions",
        displayValue = messages("currency.pounds", subcontractor.totalTaxDeducted),
        changeUrl = controllers.monthlyreturns.routes.TotalTaxDeductedController
          .onPageLoad(CheckMode, index, Some("changeAnswers"))
          .url,
        actionId = Some("total-tax-deductions")
      )
    )

  private def row(labelKey: String, displayValue: String, changeUrl: String, actionId: Option[String])(implicit
    messages: Messages
  ): SummaryListRow =
    val label = messages(labelKey)

    val baseAction =
      ActionItemViewModel(
        content = Text(messages("site.change")),
        href = changeUrl
      ).withVisuallyHiddenText(label)

    val action =
      actionId match {
        case Some(id) => baseAction.withAttribute("id", id)
        case None     => baseAction
      }

    SummaryListRowViewModel(
      key = KeyViewModel(Text(label)),
      value = ValueViewModel(Text(displayValue)),
      actions = Seq(action)
    )
}
