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

package models.monthlyreturns

import models.ReturnType.MonthlyNilReturn
import models.UserAnswers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues
import pages.monthlyreturns.*
import play.api.libs.json.{JsSuccess, Json}

import java.time.LocalDate

class UpdateMonthlyReturnRequestSpec extends AnyWordSpec with Matchers with TryValues {

  "UpdateMonthlyReturnRequest.format" should {

    "serialize and deserialize (round-trip) model" in {
      val model = UpdateMonthlyReturnRequest(
        instanceId = "instance-123",
        taxYear = 2024,
        taxMonth = 1,
        amendment = "N",
        nilReturnIndicator = "N",
        status = "STARTED"
      )

      val json = Json.toJson(model)
      json.validate[UpdateMonthlyReturnRequest] shouldBe JsSuccess(model)
    }
  }

  "UpdateMonthlyReturnRequest.fromUserAnswers" should {

    "build a request for MonthlyNilReturn (minimal required answers)" in {
      val ua = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value
        .set(CisIdPage, "CIS-123")
        .success
        .value
        .set(DateConfirmNilPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua)

      result shouldBe Right(
        UpdateMonthlyReturnRequest(
          instanceId = "CIS-123",
          taxYear = 2024,
          taxMonth = 3,
          amendment = "N",
          decNilReturnNoPayments = Some("Y"),
          decInformationCorrect = None,
          nilReturnIndicator = "Y",
          status = "STARTED",
          version = None
        )
      )
    }
  }
}
