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
  }
}
