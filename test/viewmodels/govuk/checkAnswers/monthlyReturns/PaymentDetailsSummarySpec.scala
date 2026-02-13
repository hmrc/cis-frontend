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

package viewmodels.checkAnswers

import models.{CheckMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import org.scalatest.TryValues.convertTryToSuccessOrFailure
import pages.monthlyreturns.SelectedSubcontractorPaymentsMadePage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.checkAnswers.monthlyreturns.PaymentDetailsSummary

class PaymentDetailsSummarySpec extends AnyFreeSpec with Matchers with OptionValues {

  private implicit val messages: Messages = Helpers.stubMessages()

  "PaymentDetailsSummary" - {

    "row" - {

      "must return Some(SummaryListRow) when PaymentDetailsPage is answered" in {
        val userAnswers = UserAnswers("id")
          .set(SelectedSubcontractorPaymentsMadePage(1), BigDecimal(1234.56))
          .success
          .value

        val result = PaymentDetailsSummary.row(userAnswers, 1).value

        result.key.content.asHtml.toString   must include("paymentDetails.checkYourAnswersLabel")
        result.value.content.asHtml.toString must include("£1,234.56")
        result.actions.value.items.size mustEqual 1
      }

      "must return None when PaymentDetailsPage is not answered" in {
        val userAnswers = UserAnswers("id")
        val result      = PaymentDetailsSummary.row(userAnswers, 1)

        result mustBe None
      }

      "must include the correct change action URL" in {
        val userAnswers = UserAnswers("id")
          .set(SelectedSubcontractorPaymentsMadePage(1), BigDecimal(1234.56))
          .success
          .value

        val result = PaymentDetailsSummary.row(userAnswers, 1).value
        val action = result.actions.value.items.head

        action.href mustEqual controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(CheckMode, 1).url
      }

      "must include visually hidden text in the change link" in {
        val userAnswers = UserAnswers("id")
          .set(SelectedSubcontractorPaymentsMadePage(1), BigDecimal(9999.99))
          .success
          .value

        val result = PaymentDetailsSummary.row(userAnswers, 1).value
        val action = result.actions.value.items.head

        action.visuallyHiddenText.value mustEqual messages("paymentDetails.change.hidden")
      }

      "must format different BigDecimal values correctly" in {
        val testCases = Seq(
          BigDecimal(0)          -> "£0",
          BigDecimal(100.50)     -> "£100.50",
          BigDecimal(1000000.99) -> "£1,000,000.99"
        )

        testCases.foreach { case (input, expectedOutput) =>
          val userAnswers = UserAnswers("id")
            .set(SelectedSubcontractorPaymentsMadePage(1), input)
            .success
            .value

          val result = PaymentDetailsSummary.row(userAnswers, 1).value

          result.value.content.asHtml.toString must include(expectedOutput)
        }
      }
    }
  }
}
