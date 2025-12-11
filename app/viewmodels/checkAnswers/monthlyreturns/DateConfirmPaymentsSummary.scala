package viewmodels.checkAnswers.monthlyreturns

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.monthlyreturns.DateConfirmPaymentsPage
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.DateTimeFormats.dateTimeFormat
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object DateConfirmPaymentsSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DateConfirmPaymentsPage).map {
      answer =>

        implicit val lang: Lang = messages.lang

        SummaryListRowViewModel(
          key     = "dateConfirmPayments.checkYourAnswersLabel",
          value   = ValueViewModel(answer.format(dateTimeFormat())),
          actions = Seq(
            ActionItemViewModel("site.change", routes.DateConfirmPaymentsController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("dateConfirmPayments.change.hidden"))
          )
        )
    }
}
