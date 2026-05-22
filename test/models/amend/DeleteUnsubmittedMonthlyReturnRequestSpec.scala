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

package models.amend

import base.SpecBase
import models.ReturnType.MonthlyAmendedStandardReturn
import pages.monthlyreturns.*

import java.time.LocalDate

class DeleteUnsubmittedMonthlyReturnRequestSpec extends SpecBase {

  "DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers" - {

    "build a request from user answers" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, "1")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 4, 1))
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers(userAnswers) mustBe
        DeleteUnsubmittedMonthlyReturnRequest(
          instanceId = "1",
          taxYear = 2026,
          taxMonth = 4,
          amendment = MonthlyAmendedStandardReturn.amendmentFlag
        )
    }

    "throw when CisIdPage is missing" in {
      val userAnswers = emptyUserAnswers
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 4, 1))
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val exception = intercept[RuntimeException] {
        DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers(userAnswers)
      }

      exception.getMessage mustBe "Missing instanceId"
    }

    "throw when DateConfirmPaymentsPage is missing" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, "1")
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val exception = intercept[RuntimeException] {
        DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers(userAnswers)
      }

      exception.getMessage mustBe "Missing confirmed payment date"
    }

    "throw when ReturnTypePage is missing" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, "1")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2026, 4, 1))
        .success
        .value

      val exception = intercept[RuntimeException] {
        DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers(userAnswers)
      }

      exception.getMessage mustBe "Missing return type"
    }
  }
}
