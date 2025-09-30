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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json._
import java.time.LocalDateTime

class MonthlyReturnResponseSpec extends AnyWordSpec with Matchers {

  "MonthlyReturnDetails JSON format" should {

    "round-trip a full object including lastUpdate" in {
      val ldt = LocalDateTime.of(2025, 9, 27, 12, 34, 56)

      val model = MonthlyReturnDetails(
        monthlyReturnId = 99999L,
        taxYear = 2025,
        taxMonth = 9,
        nilReturnIndicator = Some("Y"),
        decEmpStatusConsidered = Some("Y"),
        decAllSubsVerified = Some("Y"),
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = Some("N"),
        decNilReturnNoPayments = Some("N"),
        status = Some("Open"),
        lastUpdate = Some(ldt),
        amendment = Some("A1"),
        supersededBy = Some(100000L)
      )

      val js = Json.toJson(model)
      js.as[MonthlyReturnDetails] mustBe model
    }

    "parse minimal JSON with only required fields" in {
      val json =
        Json.parse(
          """
            |{
            |  "monthlyReturnId": 1001,
            |  "taxYear": 2024,
            |  "taxMonth": 4
            |}
          """.stripMargin
        )

      val parsed = json.as[MonthlyReturnDetails]
      parsed.monthlyReturnId mustBe 1001L
      parsed.taxYear mustBe 2024
      parsed.taxMonth mustBe 4
      parsed.status mustBe None
      parsed.lastUpdate mustBe None
    }

    "fail to parse when a required field is missing" in {
      val jsonMissing =
        Json.parse(
          """
            |{
            |  "monthlyReturnId": 1001,
            |  "taxYear": 2024
            |}
          """.stripMargin
        )

      jsonMissing.validate[MonthlyReturnDetails].isError mustBe true
    }
  }
}
