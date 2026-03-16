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
import org.scalatest.OptionValues
import pages.monthlyreturns.EmploymentStatusDeclarationPage
import play.api.i18n.Messages
import viewmodels.checkAnswers.monthlyreturns.ReturnTypeSummary
import play.api.test.Helpers._

class ReturnTypeSummarySpec extends SpecBase with OptionValues {

  private implicit val messages: Messages = stubMessages()

  "ReturnTypeSummary" - {

    "must return a SummaryListRow with Monthly Nil Return text when EmploymentStatusDeclarationPage is not set" in {

      val result = ReturnTypeSummary.row(emptyUserAnswers).value

      result.key.content.asHtml.toString   must include(
        messages("monthlyreturns.returnType.checkYourAnswersLabel")
      )
      result.value.content.asHtml.toString must include(
        messages("monthlyreturns.returnType.value")
      )
    }

    "must return a SummaryListRow with Monthly Return text when EmploymentStatusDeclarationPage is set" in {

      val answers = emptyUserAnswers.set(EmploymentStatusDeclarationPage, true).success.value

      val result = ReturnTypeSummary.row(answers).value

      result.key.content.asHtml.toString   must include(
        messages("monthlyreturns.returnType.checkYourAnswersLabel")
      )
      result.value.content.asHtml.toString must include(
        messages("monthlyreturns.returnType.monthlyReturnValue")
      )
    }
  }
}
