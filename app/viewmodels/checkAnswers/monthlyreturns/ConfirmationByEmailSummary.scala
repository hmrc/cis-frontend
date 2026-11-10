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

import models.{CheckMode, UserAnswers}
import pages.monthlyreturns.ConfirmationByEmailPage
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.util.UUID

object ConfirmationByEmailSummary {

  def row(answers: UserAnswers, flowId: Option[UUID] = None)(implicit messages: Messages): Option[SummaryListRow] = {
    val subJourneyId = flowId match {
      case Some(id) => id.toString
      case None     => None
    }
    val url          = flowId match {
      case Some(id) =>
        controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(id, CheckMode).url
      case None     =>
        controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(CheckMode).url
    }

    answers.get(ConfirmationByEmailPage(subJourneyId)).map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = "monthlyreturns.confirmationByEmail.checkYourAnswersLabel",
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", url)
            .withVisuallyHiddenText(messages("monthlyreturns.confirmationByEmail.change.hidden"))
            .withAttribute("id" -> "change-confirmation-by-email")
        )
      )
    }
  }
}
