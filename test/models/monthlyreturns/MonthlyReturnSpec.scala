/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.Json

import java.time.LocalDateTime

class MonthlyReturnSpec extends SpecBase {

  "MonthlyReturn" - {

    "read and write a fully-populated object" in {
      val monthlyReturn = MonthlyReturn(
        monthlyReturnId = 12345L,
        taxYear = 2024,
        taxMonth = 10,
        nilReturnIndicator = Some("Y"),
        decEmpStatusConsidered = Some("Y"),
        decAllSubsVerified = Some("Y"),
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        status = Some("STARTED"),
        lastUpdate = Some(LocalDateTime.of(2024, 10, 15, 14, 30)),
        amendment = Some("N"),
        supersededBy = None
      )

      val json = Json.toJson(monthlyReturn)
      val parsed = json.as[MonthlyReturn]

      parsed mustBe monthlyReturn
    }

    "read and write with minimal fields" in {
      val monthlyReturn = MonthlyReturn(
        monthlyReturnId = 12345L,
        taxYear = 2024,
        taxMonth = 10
      )

      val json = Json.toJson(monthlyReturn)
      val parsed = json.as[MonthlyReturn]

      parsed mustBe monthlyReturn
    }

    "handle null values correctly" in {
      val json = Json.obj(
        "monthlyReturnId" -> 12345L,
        "taxYear" -> 2024,
        "taxMonth" -> 10,
        "nilReturnIndicator" -> null,
        "decEmpStatusConsidered" -> null,
        "decAllSubsVerified" -> null,
        "decInformationCorrect" -> null,
        "decNoMoreSubPayments" -> null,
        "decNilReturnNoPayments" -> null,
        "status" -> null,
        "lastUpdate" -> null,
        "amendment" -> null,
        "supersededBy" -> null
      )

      val parsed = json.as[MonthlyReturn]

      parsed.monthlyReturnId mustBe 12345L
      parsed.taxYear mustBe 2024
      parsed.taxMonth mustBe 10
      parsed.nilReturnIndicator mustBe None
      parsed.decEmpStatusConsidered mustBe None
      parsed.decAllSubsVerified mustBe None
      parsed.decInformationCorrect mustBe None
      parsed.decNoMoreSubPayments mustBe None
      parsed.decNilReturnNoPayments mustBe None
      parsed.status mustBe None
      parsed.lastUpdate mustBe None
      parsed.amendment mustBe None
      parsed.supersededBy mustBe None
    }
  }
}
