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

package viewmodels.govuk.monthlyreturns

import models.{CheckMode, UserAnswers}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import pages.monthlyreturns.SubmitInactivityRequestPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.checkAnswers.monthlyreturns.SubmitInactivityRequestSummary

class SubmitInactivityRequestSummarySpec extends AnyFreeSpec with Matchers with OptionValues with TryValues {

  private implicit val messages: Messages = Helpers.stubMessages()

  "SubmitInactivityRequestSummary" - {

    "must return None when answer is not present" in {
      val answers = UserAnswers("id")
      SubmitInactivityRequestSummary.row(answers) mustBe None
    }

    "must return a row when answer is true" in {
      val answers = UserAnswers("id").set(SubmitInactivityRequestPage, true).success.value
      val result  = SubmitInactivityRequestSummary.row(answers).value

      result.value.content.asHtml.toString must include("monthlyreturns.submitInactivityRequest.yes")
      result.actions.value.items.head.href mustBe
        controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(CheckMode).url
    }

    "must return a row when answer is false" in {
      val answers = UserAnswers("id").set(SubmitInactivityRequestPage, false).success.value
      val result  = SubmitInactivityRequestSummary.row(answers).value

      result.value.content.asHtml.toString must include("monthlyreturns.submitInactivityRequest.no")
    }
  }
}
