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

import models.ReturnType.MonthlyStandardReturn
import models.UserAnswers
import models.amend.AmendmentDetails
import org.scalatest.TryValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec
import pages.amend.AmendmentDetailsPage
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.libs.json.*

import java.time.LocalDate

class GetMonthlyReturnForEditRequestSpec extends AnyWordSpec with Matchers with TryValues {

  "GetMonthlyReturnForEditRequest JSON format" should {

    "serialize to JSON" in {
      val model = GetMonthlyReturnForEditRequest(
        instanceId = "instance-123",
        taxMonth = 5,
        taxYear = 2024,
        isAmendment = true
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "instanceId"  -> "instance-123",
        "taxMonth"    -> 5,
        "taxYear"     -> 2024,
        "isAmendment" -> true
      )
    }

    "deserialize from JSON" in {
      val json = Json.obj(
        "instanceId"  -> "instance-123",
        "taxMonth"    -> 5,
        "taxYear"     -> 2024,
        "isAmendment" -> true
      )

      val result = json.as[GetMonthlyReturnForEditRequest]

      result mustBe GetMonthlyReturnForEditRequest(
        instanceId = "instance-123",
        taxMonth = 5,
        taxYear = 2024,
        isAmendment = true
      )
    }
  }

  "UpdateMonthlyReturnRequest.fromUserAnswers" should {

    "build a request without AmendmentDetailsPage" in {
      val ua = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value

      val result = GetMonthlyReturnForEditRequest.fromUserAnswers(ua)

      result shouldBe Right(
        GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2024,
          taxMonth = 3,
          isAmendment = false
        )
      )
    }

    "build a request with AmendmentDetailsPage" in {
      val amendmentDetails = AmendmentDetails(
        instanceId = "CIS-123",
        taxYear = 2024,
        taxMonth = 3,
        contractorName = "Test Contractor Ltd",
        originalReturnType = MonthlyStandardReturn,
        acceptedTime = Some("2025-04-01T12:00:00Z")
      )

      val ua = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value
        .set(AmendmentDetailsPage, amendmentDetails)
        .success
        .value

      val result = GetMonthlyReturnForEditRequest.fromUserAnswers(ua)

      result shouldBe Right(
        GetMonthlyReturnForEditRequest(
          instanceId = "CIS-123",
          taxYear = 2024,
          taxMonth = 3,
          isAmendment = true
        )
      )
    }

    "when CisIdPage is missing" in {
      val ua = UserAnswers("test-user")

      val result = GetMonthlyReturnForEditRequest.fromUserAnswers(ua)

      result shouldBe Left("Missing CisIdPage")
    }

    "when DateConfirmPaymentsPage is missing" in {
      val ua = UserAnswers("test-user")
        .set(CisIdPage, "CIS-123")
        .success
        .value

      val result = GetMonthlyReturnForEditRequest.fromUserAnswers(ua)

      result shouldBe Left("Missing DateConfirmPayments")
    }
  }
}
