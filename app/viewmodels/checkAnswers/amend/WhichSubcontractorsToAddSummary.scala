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

package viewmodels.checkAnswers.amend

import controllers.amend.routes
import models.{CheckMode, UserAnswers}
import models.amend.WhichSubcontractorsToAdd
import pages.amend.WhichSubcontractorsToAddPage
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.govuk.summarylist._
import viewmodels.implicits._

object WhichSubcontractorsToAddSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] = {
    val subcontractorMap = WhichSubcontractorsToAdd.mockSubcontractors.map(s => s.id -> s.name).toMap

    answers.get(WhichSubcontractorsToAddPage).map { selectedIds =>
      val value = ValueViewModel(
        HtmlContent(
          selectedIds
            .map(id => HtmlFormat.escape(subcontractorMap.getOrElse(id, id)).toString)
            .mkString(",<br>")
        )
      )

      SummaryListRowViewModel(
        key = "amend.whichSubcontractorsToAdd.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", routes.WhichSubcontractorsToAddController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("amend.whichSubcontractorsToAdd.change.hidden"))
            .withAttribute("id" -> "which-subcontractors-to-add")
        )
      )
    }
  }
}
