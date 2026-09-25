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

package models.requests

import base.SpecBase
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyNilReturn, MonthlyStandardReturn}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, ReturnTypePage}

import java.time.LocalDate

class GetMonthlyReturnCompleteRequestSpec extends SpecBase {

  private val instanceId = "test-instance-id"
  private val taxDate    = LocalDate.of(2024, 6, 5)

  "GetMonthlyReturnCompleteRequest.fromUserAnswers" - {

    "return a request with amendment Y for an amended standard return" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, instanceId)
        .success
        .value
        .set(DateConfirmPaymentsPage, taxDate)
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Right(
        GetMonthlyReturnCompleteRequest(
          instanceId = instanceId,
          taxYear = 2024,
          taxMonth = 6,
          amendment = "Y"
        )
      )
    }

    "return a request with amendment Y for an amended nil return" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, instanceId)
        .success
        .value
        .set(DateConfirmPaymentsPage, taxDate)
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedNilReturn)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Right(
        GetMonthlyReturnCompleteRequest(
          instanceId = instanceId,
          taxYear = 2024,
          taxMonth = 6,
          amendment = "Y"
        )
      )
    }

    "return a request with amendment N for a standard return" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, instanceId)
        .success
        .value
        .set(DateConfirmPaymentsPage, taxDate)
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Right(
        GetMonthlyReturnCompleteRequest(
          instanceId = instanceId,
          taxYear = 2024,
          taxMonth = 6,
          amendment = "N"
        )
      )
    }

    "return a request with amendment N for a nil return" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, instanceId)
        .success
        .value
        .set(DateConfirmPaymentsPage, taxDate)
        .success
        .value
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Right(
        GetMonthlyReturnCompleteRequest(
          instanceId = instanceId,
          taxYear = 2024,
          taxMonth = 6,
          amendment = "N"
        )
      )
    }

    "return an error when CisIdPage is missing" in {
      val userAnswers = emptyUserAnswers
        .set(DateConfirmPaymentsPage, taxDate)
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Left("Missing CisIdPage")
    }

    "return an error when DateConfirmPaymentsPage is missing" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, instanceId)
        .success
        .value
        .set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Left("Missing DateConfirmPaymentsPage")
    }

    "return an error when ReturnTypePage is missing" in {
      val userAnswers = emptyUserAnswers
        .set(CisIdPage, instanceId)
        .success
        .value
        .set(DateConfirmPaymentsPage, taxDate)
        .success
        .value

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers)

      result mustBe Left("Missing ReturnTypePage")
    }
  }
}
