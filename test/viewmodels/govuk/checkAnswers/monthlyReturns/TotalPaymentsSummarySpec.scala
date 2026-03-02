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
