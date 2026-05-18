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

import base.SpecBase
import models.{ReturnType, UserAnswers}
import pages.monthlyreturns.*
import play.api.libs.json.Json

import java.time.LocalDate

class SelectedSubcontractorsRequestSpec extends SpecBase {

  "SelectedSubcontractorsRequest" - {

    "must serialize and deserialize JSON" in {
      val model = SelectedSubcontractorsRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        selectedSubcontractorIds = Seq(1L, 2L),
        amendment = "N"
      )

      val json = Json.obj(
        "instanceId"               -> "1",
        "taxYear"                  -> 2025,
        "taxMonth"                 -> 1,
        "selectedSubcontractorIds" -> Json.arr(1L, 2L),
        "amendment"                -> "N"
      )

      Json.toJson(model) mustBe json
      json.as[SelectedSubcontractorsRequest] mustBe model
    }

    "must build from UserAnswers" in {
      val userAnswers =
        UserAnswers("id")
          .set(CisIdPage, "1")
          .success
          .value
          .set(DateConfirmPaymentsPage, LocalDate.of(2025, 1, 1))
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyNilReturn)
          .success
          .value

      SelectedSubcontractorsRequest.from(userAnswers, Seq(1L, 2L)) mustBe
        SelectedSubcontractorsRequest(
          instanceId = "1",
          taxYear = 2025,
          taxMonth = 1,
          selectedSubcontractorIds = Seq(1L, 2L),
          amendment = "N"
        )
    }
  }
}
