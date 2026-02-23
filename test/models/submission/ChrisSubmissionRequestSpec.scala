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

package models.submission

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.YearMonth

class ChrisSubmissionRequestSpec extends AnyWordSpec with Matchers {

  "ChrisSubmissionRequest.from" should {
    "map booleans to 'yes'/'no' and YearMonth to ISO yyyy-MM" in {
      val yearMonth = YearMonth.of(2025, 3)
      val dto       = ChrisSubmissionRequest.from(
        utr = "1234567890",
        aoReference = "123/AB456",
        informationCorrect = true,
        inactivity = false,
        monthYear = yearMonth,
        email = "test@test.com",
        isAgent = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456"
      )

      dto.utr mustBe "1234567890"
      dto.aoReference mustBe "123/AB456"
      dto.informationCorrect mustBe "yes"
      dto.inactivity mustBe "no"
      dto.monthYear mustBe "2025-03"
      dto.email mustBe "test@test.com"
      dto.isAgent mustBe false
      dto.clientTaxOfficeNumber mustBe "123"
      dto.clientTaxOfficeRef mustBe "AB456"
    }
  }

  "ChrisSubmissionRequest JSON format" should {
    "write to the expected JSON shape" in {
      val dto = ChrisSubmissionRequest(
        utr = "1234567890",
        aoReference = "123/AB456",
        informationCorrect = "yes",
        inactivity = "no",
        monthYear = "2025-03",
        email = "test@test.com",
        isAgent = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456"
      )

      val json     = Json.toJson(dto)
      val expected = Json.parse("""{
          |  "utr": "1234567890",
          |  "aoReference": "123/AB456",
          |  "informationCorrect": "yes",
          |  "inactivity": "no",
          |  "monthYear": "2025-03",
          |  "email": "test@test.com",
          |  "isAgent": false,
          |  "clientTaxOfficeNumber" : "123",
          |  "clientTaxOfficeRef": "AB456"
          |}""".stripMargin)

      json mustBe expected
    }

    "read from JSON into the model" in {
      val json = Json.parse("""{
          |  "utr": "1234567890",
          |  "aoReference": "123/AB456",
          |  "informationCorrect": "yes",
          |  "inactivity": "no",
          |  "monthYear": "2025-03",
          |  "email": "test@test.com",
          |  "isAgent": false,
          |  "clientTaxOfficeNumber" : "123",
          |  "clientTaxOfficeRef": "AB456"
          |}""".stripMargin)

      val result = json.validate[ChrisSubmissionRequest]
      result.isSuccess mustBe true

      val dto = result.get
      dto.utr mustBe "1234567890"
      dto.aoReference mustBe "123/AB456"
      dto.informationCorrect mustBe "yes"
      dto.inactivity mustBe "no"
      dto.monthYear mustBe "2025-03"
      dto.email mustBe "test@test.com"
      dto.isAgent mustBe false
      dto.clientTaxOfficeNumber mustBe "123"
      dto.clientTaxOfficeRef mustBe "AB456"
    }
  }
}
