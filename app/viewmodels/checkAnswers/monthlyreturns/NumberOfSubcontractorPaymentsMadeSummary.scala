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
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*

import java.util.UUID

object NumberOfSubcontractorPaymentsMadeSummary {

  def row(answers: UserAnswers, flowId: Option[UUID] = None)(implicit messages: Messages): Option[SummaryListRow] = {
    val subJourneyId = flowId match {
      case Some(id) => id.toString
      case None     => None
    }
    val url          = flowId match {
      case Some(id) =>
        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoadWithId(id, CheckMode).url
      case None     =>
        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode).url
    }

    SubcontractorDetailsAddedBuilder.build(answers, subJourneyId).map { addedSubcontractors =>
      SummaryListRowViewModel(
        key = "monthlyreturns.numberOfSubcontractorPaymentsMade.checkYourAnswersLabel",
        value = ValueViewModel(addedSubcontractors.rows.size.toString),
        actions = Seq(
          ActionItemViewModel("site.change", url)
            .withVisuallyHiddenText(messages("monthlyreturns.numberOfSubcontractorPaymentsMade.hidden"))
            .withAttribute("id" -> "change-number-subcontractor-payments-made")
        )
      )
    }
  }
}
