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
import models.ReturnType
import org.scalatest.OptionValues
import pages.monthlyreturns.ReturnTypePage
import play.api.i18n.Messages
import viewmodels.checkAnswers.monthlyreturns.ReturnTypeSummary
import play.api.test.Helpers._

class ReturnTypeSummarySpec extends SpecBase with OptionValues {

  private implicit val messages: Messages = stubMessages()

  "ReturnTypeSummary" - {

    "when answer is present" - {

      "must return a SummaryListRow with Monthly Nil Return text when answer is MonthlyNilReturn" in {

        val answers =
          emptyUserAnswers.set(ReturnTypePage, ReturnType.MonthlyNilReturn).success.value

        val result = ReturnTypeSummary.row(answers).value

        result.key.content.asHtml.toString must include(
          messages("monthlyreturns.returnType.checkYourAnswersLabel")
        )

        result.value.content.asHtml.toString must include(
          messages(s"monthlyreturns.returnType.${ReturnType.MonthlyNilReturn.toString}")
        )
      }

      "must return a SummaryListRow with Monthly Standard Return text when answer is MonthlyStandardReturn" in {

        val answers =
          emptyUserAnswers.set(ReturnTypePage, ReturnType.MonthlyStandardReturn).success.value

        val result = ReturnTypeSummary.row(answers).value

        result.key.content.asHtml.toString must include(
          messages("monthlyreturns.returnType.checkYourAnswersLabel")
        )

        result.value.content.asHtml.toString must include(
          messages(s"monthlyreturns.returnType.${ReturnType.MonthlyStandardReturn.toString}")
        )
      }
    }

    "when answer is not present" - {

      "must return None" in {

        val answers = emptyUserAnswers

        val result = ReturnTypeSummary.row(answers)

        result mustBe None
      }
    }
  }
}
