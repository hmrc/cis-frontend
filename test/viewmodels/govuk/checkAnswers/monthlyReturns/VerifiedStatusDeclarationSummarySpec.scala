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

package viewmodels.govuk.checkAnswers.monthlyReturns

import base.SpecBase
import models.CheckMode
import org.scalatest.OptionValues
import pages.monthlyreturns.VerifiedStatusDeclarationPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.checkAnswers.monthlyreturns.VerifiedStatusDeclarationSummary

class VerifiedStatusDeclarationSummarySpec extends SpecBase with OptionValues {

  private implicit val messages: Messages = Helpers.stubMessages()

  "VerifiedStatusDeclarationSummary" - {

    "when answer is present" - {

      "must return a SummaryListRow with 'Yes' when answer is true" in {
        val answers = emptyUserAnswers
          .set(VerifiedStatusDeclarationPage, true)
          .success
          .value

        val result = VerifiedStatusDeclarationSummary.row(answers).value

        result.key.content.asHtml.toString must include(
          messages("monthlyreturns.verifiedStatusDeclaration.checkYourAnswersLabel")
        )

        result.value.content.asHtml.toString must include(messages("site.yes"))

        result.actions.value.items.head.href mustBe
          controllers.monthlyreturns.routes.VerifiedStatusDeclarationController
            .onPageLoad(CheckMode)
            .url

        result.actions.value.items.head.visuallyHiddenText.value mustBe
          messages("monthlyreturns.verifiedStatusDeclaration.change.hidden")
      }

      "must return a SummaryListRow with 'No' when answer is false" in {
        val answers = emptyUserAnswers
          .set(VerifiedStatusDeclarationPage, false)
          .success
          .value

        val result = VerifiedStatusDeclarationSummary.row(answers).value

        result.key.content.asHtml.toString must include(
          messages("monthlyreturns.verifiedStatusDeclaration.checkYourAnswersLabel")
        )

        result.value.content.asHtml.toString must include(messages("site.no"))

        result.actions.value.items.head.href mustBe
          controllers.monthlyreturns.routes.VerifiedStatusDeclarationController
            .onPageLoad(CheckMode)
            .url

        result.actions.value.items.head.visuallyHiddenText.value mustBe
          messages("monthlyreturns.verifiedStatusDeclaration.change.hidden")
      }
    }

    "when answer is not present" - {

      "must return None" in {
        val answers = emptyUserAnswers

        val result = VerifiedStatusDeclarationSummary.row(answers)

        result mustBe None
      }
    }
  }
}
