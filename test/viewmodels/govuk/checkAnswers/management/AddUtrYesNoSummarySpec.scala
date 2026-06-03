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

package viewmodels.govuk.checkAnswers.management

import models.{CheckMode, UserAnswers}
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pages.management.AddUtrYesNoPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import viewmodels.checkAnswers.management.AddUtrYesNoSummary

class AddUtrYesNoSummarySpec extends AnyFreeSpec with Matchers {

  implicit val messages: Messages = stubMessages()

  "AddUtrYesNoSummary.row" - {

    "must return a SummaryListRow with 'Yes' when the answer is true" in {

      val answers =
        UserAnswers("test-id")
          .set(AddUtrYesNoPage, true)
          .success
          .value

      val maybeRow = AddUtrYesNoSummary.row(answers)
      maybeRow shouldBe defined

      val row = maybeRow.value

      val expectedKeyText =
        messages("management.finalValidations.addUtrYesNo.checkYourAnswersLabel")
      row.key.content.asHtml.toString should include(expectedKeyText)

      val expectedValue = messages("site.yes")
      row.value.content.asHtml.toString should include(expectedValue)

      row.actions shouldBe defined
      val actions = row.actions.value.items
      actions should have size 1

      val action = actions.head

      val expectedChangeText = messages("site.change")
      val expectedHref       =
        controllers.management.routes.AddUtrYesNoController.onPageLoad(CheckMode).url
      val expectedHiddenText =
        messages("management.finalValidations.addUtrYesNo.change.hidden")

      action.content.asHtml.toString    should include(expectedChangeText)
      action.href                     shouldBe expectedHref
      action.visuallyHiddenText.value shouldBe expectedHiddenText
    }

    "must return a SummaryListRow with 'No' when the answer is false" in {

      val answers =
        UserAnswers("test-id")
          .set(AddUtrYesNoPage, false)
          .success
          .value

      val maybeRow = AddUtrYesNoSummary.row(answers)
      maybeRow shouldBe defined

      val row = maybeRow.value

      val expectedValue = messages("site.no")
      row.value.content.asHtml.toString should include(expectedValue)
    }

    "must return None when the answer does not exist" in {

      val answers = UserAnswers("test-id")

      AddUtrYesNoSummary.row(answers) shouldBe None
    }
  }
}
