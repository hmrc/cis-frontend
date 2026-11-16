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
import models.ReturnType.MonthlyAmendedNilReturn
import models.{ReturnType, UserAnswers}
import pages.monthlyreturns.*
import play.api.libs.json.Json

import java.time.LocalDate

class DeleteAllMonthlyReturnItemsRequestSpec extends SpecBase {

  "DeleteAllMonthlyReturnItemsRequest" - {

    "must serialize and deserialize JSON" in {
      val model = DeleteAllMonthlyReturnItemsRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N"
      )

      val json = Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 1,
        "amendment"  -> "N"
      )

      Json.toJson(model) mustBe json
      json.as[DeleteAllMonthlyReturnItemsRequest] mustBe model
    }

    "must build from UserAnswers" in {
      val userAnswers =
        UserAnswers("id")
          .set(CisIdPage, "abc-123")
          .success
          .value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 1, 1))
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value

      DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(userAnswers) mustBe
        Right(
          DeleteAllMonthlyReturnItemsRequest(
            instanceId = "abc-123",
            taxYear = 2025,
            taxMonth = 1,
            amendment = "N"
          )
        )
    }

    "must return Left when required answers are missing" in {
      DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(UserAnswers("id")) mustBe
        Left("Missing CisId")
    }

    "fromUserAnswers with a journey id" - {

      val id = "5f0c1a2e-0000-4000-8000-000000000001"

      def scoped: UserAnswers =
        emptyUserAnswers
          .set(CisIdPage(id), "CIS-123").success.value
          .set(DateConfirmPaymentsPage(id), LocalDate.of(2025, 3, 5)).success.value
          .set(ReturnTypePage(id), MonthlyAmendedNilReturn).success.value

      "build the request from the scoped answers" in {
        DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(scoped, id) mustBe
          Right(DeleteAllMonthlyReturnItemsRequest("CIS-123", 2025, 3, MonthlyAmendedNilReturn.amendmentFlag))
      }

      "ignore the legacy unscoped answers" in {
        val legacyOnly = emptyUserAnswers
          .set(CisIdPage, "CIS-123").success.value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 3, 5)).success.value
          .set(ReturnTypePage, MonthlyAmendedNilReturn).success.value

        DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(legacyOnly, id) mustBe Left("Missing CisId")
      }

      "not read another journey's answers" in {
        DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(scoped, "another-flow-id") mustBe Left("Missing CisId")
      }

      "report each missing scoped answer" in {
        val noDate = emptyUserAnswers.set(CisIdPage(id), "CIS-123").success.value
        DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(noDate, id) mustBe Left("Missing DateConfirmPayments")

        val noType = noDate.set(DateConfirmPaymentsPage(id), LocalDate.of(2025, 3, 5)).success.value
        DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(noType, id) mustBe Left("Missing ReturnType")
      }
    }
  }
}
