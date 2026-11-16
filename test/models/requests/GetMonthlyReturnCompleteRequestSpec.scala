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
import models.amend.AmendmentDetails
import pages.amend.AmendmentDetailsPage
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyNilReturn, MonthlyStandardReturn}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, ReturnTypePage}

import java.time.LocalDate

class GetMonthlyReturnCompleteRequestSpec extends SpecBase {

  private val instanceId = "test-instance-id"
  private val taxDate    = LocalDate.of(2024, 6, 5)

  "GetMonthlyReturnCompleteRequest.fromUserAnswers" - {

    "return a request with amendment Y when AmendmentDetailsPage exists" in {

      val amendmentDetails = AmendmentDetails(
        instanceId = instanceId,
        taxYear = 2024,
        taxMonth = 6,
        contractorName = "Test Contractor",
        originalReturnType = MonthlyStandardReturn,
        acceptedTime = None
      )

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
        .set(AmendmentDetailsPage, amendmentDetails)
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

      val amendmentDetails = AmendmentDetails(
        instanceId = instanceId,
        taxYear = 2024,
        taxMonth = 6,
        contractorName = "Test Contractor Ltd",
        originalReturnType = MonthlyStandardReturn,
        acceptedTime = None
      )

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
        .set(AmendmentDetailsPage, amendmentDetails)
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

  "GetMonthlyReturnCompleteRequest.fromUserAnswers with id" - {

    "return a request with amendment N for an ID-scoped nil return" in {
      val id = "test-id"

      val userAnswers = emptyUserAnswers
        .setOrException(CisIdPage(id), instanceId)
        .setOrException(DateConfirmPaymentsPage(id), taxDate)
        .setOrException(ReturnTypePage(id), MonthlyNilReturn)

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers, id)

      result mustBe Right(
        GetMonthlyReturnCompleteRequest(
          instanceId = instanceId,
          taxYear = 2024,
          taxMonth = 6,
          amendment = "N"
        )
      )
    }

    "return a request with amendment Y for an ID-scoped amended nil return" in {
      val id = "test-id"

      val userAnswers = emptyUserAnswers
        .setOrException(CisIdPage(id), instanceId)
        .setOrException(DateConfirmPaymentsPage(id), taxDate)
        .setOrException(ReturnTypePage(id), MonthlyAmendedNilReturn)

      val result =
        GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers, id)

      result mustBe Right(
        GetMonthlyReturnCompleteRequest(
          instanceId = instanceId,
          taxYear = 2024,
          taxMonth = 6,
          amendment = "Y"
        )
      )
    }

    "return an error when required ID-scoped answers are missing" in {
      val id = "test-id"

      val userAnswers = emptyUserAnswers
        .setOrException(CisIdPage, instanceId)
        .setOrException(DateConfirmPaymentsPage, taxDate)
        .setOrException(ReturnTypePage, MonthlyNilReturn)

      GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers, id) mustBe Left("Missing CisIdPage")

      val withCisId = userAnswers.setOrException(CisIdPage(id), instanceId)

      GetMonthlyReturnCompleteRequest.fromUserAnswers(withCisId, id) mustBe Left("Missing DateConfirmPaymentsPage")

      val withDate = withCisId.setOrException(DateConfirmPaymentsPage(id), taxDate)

      GetMonthlyReturnCompleteRequest.fromUserAnswers(withDate, id) mustBe Left("Missing ReturnTypePage")
    }

    "use answers from the correct ID-scoped journey" in {
      val userAnswers = emptyUserAnswers
        .setOrException(CisIdPage("first-id"), "CIS-123")
        .setOrException(DateConfirmPaymentsPage("first-id"), LocalDate.of(2024, 3, 1))
        .setOrException(ReturnTypePage("first-id"), MonthlyNilReturn)
        .setOrException(CisIdPage("second-id"), "CIS-456")
        .setOrException(DateConfirmPaymentsPage("second-id"), LocalDate.of(2024, 4, 1))
        .setOrException(ReturnTypePage("second-id"), MonthlyAmendedNilReturn)

      val firstResult  = GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers, "first-id").toOption.get
      val secondResult = GetMonthlyReturnCompleteRequest.fromUserAnswers(userAnswers, "second-id").toOption.get

      firstResult.instanceId mustBe "CIS-123"
      firstResult.taxMonth mustBe 3
      firstResult.amendment mustBe "N"

      secondResult.instanceId mustBe "CIS-456"
      secondResult.taxMonth mustBe 4
      secondResult.amendment mustBe "Y"
    }
  }
}
