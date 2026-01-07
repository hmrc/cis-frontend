package viewmodels.checkAnswers.monthlyreturns

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.monthlyreturns.VerifySubcontractorsPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

object VerifySubcontractorsSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(VerifySubcontractorsPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"verifySubcontractors.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "verifySubcontractors.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.VerifySubcontractorsController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("verifySubcontractors.change.hidden"))
          )
        )
    }
}
