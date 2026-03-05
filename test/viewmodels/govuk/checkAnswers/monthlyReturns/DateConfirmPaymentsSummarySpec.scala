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
import pages.monthlyreturns.DateConfirmPaymentsPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.checkAnswers.monthlyreturns.DateConfirmPaymentsSummary

import java.time.LocalDate

class DateConfirmPaymentsSummarySpec extends SpecBase with OptionValues {

  private implicit val messages: Messages = Helpers.stubMessages()

  "DateConfirmPaymentsSummary" - {

    "when answer is present" - {

      "must return a SummaryListRow with the formatted return period and no change action" in {
        val date    = LocalDate.of(2025, 2, 5)
        val answers = emptyUserAnswers.set(DateConfirmPaymentsPage, date).success.value

        val result = DateConfirmPaymentsSummary.row(answers).value

        result.key.content.asHtml.toString   must include(
          messages("monthlyreturns.dateConfirmPayments.checkYourAnswersLabel")
        )
        result.value.content.asHtml.toString must include("February 2025")
        result.actions mustBe None
      }
    }

    "when answer is not present" - {

      "must return None" in {
        val result = DateConfirmPaymentsSummary.row(emptyUserAnswers)

        result mustBe None
      }
    }
  }
}
