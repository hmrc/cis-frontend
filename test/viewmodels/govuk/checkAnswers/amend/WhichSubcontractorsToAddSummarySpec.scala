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

package viewmodels.govuk.checkAnswers.amend

import base.SpecBase
import models.CheckMode
import org.scalatest.OptionValues
import pages.amend.WhichSubcontractorsToAddPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.checkAnswers.amend.WhichSubcontractorsToAddSummary

class WhichSubcontractorsToAddSummarySpec extends SpecBase with OptionValues {

  private implicit val messages: Messages = Helpers.stubMessages()

  "WhichSubcontractorsToAddSummary" - {

    "when answer is present" - {

      "must return a SummaryListRow with selected subcontractor IDs" in {
        val selectedIds = Set("1", "2")

        val answers = emptyUserAnswers
          .set(WhichSubcontractorsToAddPage, selectedIds)
          .success
          .value

        val result = WhichSubcontractorsToAddSummary.row(answers).value

        result.key.content.asHtml.toString must include(
          messages("amend.whichSubcontractorsToAdd.checkYourAnswersLabel")
        )

        result.value.content.asHtml.toString must include("1")
        result.value.content.asHtml.toString must include("2")

        result.actions.value.items.head.href mustBe
          controllers.amend.routes.WhichSubcontractorsToAddController
            .onPageLoad(CheckMode)
            .url

        result.actions.value.items.head.visuallyHiddenText.value mustBe
          messages("amend.whichSubcontractorsToAdd.change.hidden")

        result.actions.value.items.head.attributes must contain("id" -> "which-subcontractors-to-add")
      }
    }

    "when answer is not present" - {

      "must return None" in {
        val result = WhichSubcontractorsToAddSummary.row(emptyUserAnswers)

        result mustBe None
      }
    }
  }
}
