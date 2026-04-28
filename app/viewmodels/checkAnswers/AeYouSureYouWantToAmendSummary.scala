package viewmodels.checkAnswers

import controllers.routes
import models.{CheckMode, UserAnswers}
import pages.AeYouSureYouWantToAmendPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object AeYouSureYouWantToAmendSummary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(AeYouSureYouWantToAmendPage).map {
      answer =>

        val value = ValueViewModel(
          HtmlContent(
            HtmlFormat.escape(messages(s"aeYouSureYouWantToAmend.$answer"))
          )
        )

        SummaryListRowViewModel(
          key     = "aeYouSureYouWantToAmend.checkYourAnswersLabel",
          value   = value,
          actions = Seq(
            ActionItemViewModel("site.change", routes.AeYouSureYouWantToAmendController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("aeYouSureYouWantToAmend.change.hidden"))
          )
        )
    }
}
