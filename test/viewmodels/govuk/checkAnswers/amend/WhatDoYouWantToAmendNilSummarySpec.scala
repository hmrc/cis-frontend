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


import models.amend.WhatDoYouWantToAmendNil
import models.{CheckMode, UserAnswers}
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import pages.amend.WhatDoYouWantToAmendNilPage
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.*
import viewmodels.checkAnswers.amend.WhatDoYouWantToAmendNilSummary

class WhatDoYouWantToAmendNilSummarySpec extends AnyFreeSpec with Matchers{

  implicit val messages: Messages = stubMessages()

  "WhatDoYouWantToAmendNilSummary.row" - {

    "must return a SummaryListRow when the answer exists" in {
      val answers =
        UserAnswers("test-id")
          .set(WhatDoYouWantToAmendNilPage, WhatDoYouWantToAmendNil.AmendNilReturn)
          .success
          .value

      val maybeRow = WhatDoYouWantToAmendNilSummary.row(answers)
      maybeRow shouldBe defined

      val row = maybeRow.value

      row.key.content.asHtml.toString should include(messages("whatDoYouWantToAmendNil.checkYourAnswersLabel"))

      val expectedValue = messages("whatDoYouWantToAmendNil.amendNilReturn")
      row.value.content.asHtml.toString should include(expectedValue)

      row.actions shouldBe defined
      val actions = row.actions.value.items
      actions should have size 1

      val changeAction = actions.head
      changeAction.content.asHtml.toString should include(messages("site.change"))

      changeAction.href shouldBe controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad(CheckMode).url
      changeAction.visuallyHiddenText.value shouldBe messages("whatDoYouWantToAmendNil.change.hidden")
    }

    "must return None when the answer does not exist" in {
      val answers = UserAnswers("test-id")
      WhatDoYouWantToAmendNilSummary.row(answers) shouldBe None
    }
  }
}
