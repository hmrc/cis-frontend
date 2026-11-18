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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.Helpers
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.checkAnswers.monthlyreturns.{ChangeAnswersTotalPaymentsViewModel, CheckAnswersTotalPaymentsViewModel, TotalPaymentsSummary}
import java.util.UUID

class TotalPaymentsSummarySpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = Helpers.stubMessages()
  private val index                       = 1
  private val flowId                      = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

  private val checkVm =
    CheckAnswersTotalPaymentsViewModel(
      id = 123L,
      name = "TyneWear Ltd",
      totalPaymentsMade = "100",
      costOfMaterials = "50",
      totalTaxDeducted = "10"
    )

  private val changeVm =
    ChangeAnswersTotalPaymentsViewModel(
      id = 123L,
      name = "TyneWear Ltd",
      totalPaymentsMade = "100",
      costOfMaterials = "50",
      totalTaxDeducted = "10"
    )

  "rowsForCheckAnswers" in {
    val rows: Seq[SummaryListRow] = TotalPaymentsSummary.rowsForCheckAnswers(checkVm, index)
    rows must have size 3
  }

  "rowsForChangeAnswers" in {
    val rows: Seq[SummaryListRow] = TotalPaymentsSummary.rowsForChangeAnswers(changeVm, index)
    rows must have size 3
  }

  "rowsForCheckAnswers with ID must return UUID-scoped change links" in {
    val rows: Seq[SummaryListRow] = TotalPaymentsSummary.rowsForCheckAnswers(checkVm, index, Some(flowId))

    rows must have size 3

    rows(0).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.PaymentDetailsController
        .onPageLoadWithId(flowId, models.CheckMode, index, None)
        .url

    rows(1).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.CostOfMaterialsController
        .onPageLoadWithId(flowId, models.CheckMode, index, None)
        .url

    rows(2).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.TotalTaxDeductedController
        .onPageLoadWithId(flowId, models.CheckMode, index, None)
        .url
  }

  "rowsForCheckAnswers with no ID must return legacy change links" in {
    val rows: Seq[SummaryListRow] = TotalPaymentsSummary.rowsForCheckAnswers(checkVm, index, None)

    rows must have size 3

    rows(0).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.PaymentDetailsController
        .onPageLoad(models.CheckMode, index, None)
        .url

    rows(1).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.CostOfMaterialsController
        .onPageLoad(models.CheckMode, index, None)
        .url

    rows(2).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.TotalTaxDeductedController
        .onPageLoad(models.CheckMode, index, None)
        .url
  }

  "rowsForChangeAnswers with ID must return UUID-scoped change links" in {
    val rows: Seq[SummaryListRow] = TotalPaymentsSummary.rowsForChangeAnswers(changeVm, index, Some(flowId))

    rows must have size 3

    rows(0).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.PaymentDetailsController
        .onPageLoadWithId(flowId, models.CheckMode, index, Some("changeAnswers"))
        .url

    rows(1).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.CostOfMaterialsController
        .onPageLoadWithId(flowId, models.CheckMode, index, Some("changeAnswers"))
        .url

    rows(2).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.TotalTaxDeductedController
        .onPageLoadWithId(flowId, models.CheckMode, index, Some("changeAnswers"))
        .url
  }

  "rowsForChangeAnswers with no ID must return legacy change links" in {
    val rows: Seq[SummaryListRow] = TotalPaymentsSummary.rowsForChangeAnswers(changeVm, index, None)

    rows must have size 3

    rows(0).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.PaymentDetailsController
        .onPageLoad(models.CheckMode, index, Some("changeAnswers"))
        .url

    rows(1).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.CostOfMaterialsController
        .onPageLoad(models.CheckMode, index, Some("changeAnswers"))
        .url

    rows(2).actions.get.items.head.href mustBe
      controllers.monthlyreturns.routes.TotalTaxDeductedController
        .onPageLoad(models.CheckMode, index, Some("changeAnswers"))
        .url
  }
}
