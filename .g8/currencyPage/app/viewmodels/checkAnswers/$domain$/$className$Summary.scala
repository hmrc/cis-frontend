package viewmodels.checkAnswers.$domain$

import config.CurrencyFormatter.currencyFormat
import controllers.$domain$.routes
import models.{CheckMode, UserAnswers}
import pages.$domain$.$className$Page
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object $className$Summary  {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get($className$Page).map {
      answer =>

        SummaryListRowViewModel(
          key     = "$domain$.$className;format="decap"$.checkYourAnswersLabel",
          value   = ValueViewModel(currencyFormat(answer)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.$className$Controller.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("$domain$.$className;format="decap"$.change.hidden"))
          )
        )
    }
}
