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

class TotalPaymentsSummarySpec extends AnyFreeSpec with Matchers {

  private implicit val messages: Messages = Helpers.stubMessages()
  private val index                       = 1

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
}
