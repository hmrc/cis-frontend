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

class CisTaxpayerSpec extends AnyWordSpec with Matchers {

  "CisTaxpayer JSON format" should {

    "round-trip (writes -> reads) with all fields populated" in {
      val model = CisTaxpayer(
        uniqueId = "CIS-123",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB456",
        aoDistrict = Some("01"),
        aoPayType = Some("P"),
        aoCheckCode = Some("99"),
        aoReference = Some("123/AB456"),
        validBusinessAddr = Some("Y"),
        correlation = Some("corr-1"),
        ggAgentId = Some("AGENT-1"),
        employerName1 = Some("Test Ltd"),
        employerName2 = Some("Group"),
        agentOwnRef = Some("Ref-1"),
        schemeName = Some("Scheme-X"),
        utr = Some("1234567890"),
        enrolledSig = Some("Y")
      )

      val js = Json.toJson(model)
      js.as[CisTaxpayer] mustBe model
    }

    "parse minimal JSON with only required fields" in {
      val json =
        Json.parse(
          """
            |{
            |  "uniqueId": "CIS-123",
            |  "taxOfficeNumber": "123",
            |  "taxOfficeRef": "AB456"
            |}
          """.stripMargin
        )

      val parsed = json.as[CisTaxpayer]
      parsed.uniqueId mustBe "CIS-123"
      parsed.taxOfficeNumber mustBe "123"
      parsed.taxOfficeRef mustBe "AB456"
      parsed.aoDistrict mustBe None
      parsed.schemeName mustBe None
    }

    "fail to parse when a required field is missing" in {
      val jsonMissing =
        Json.parse(
          """
            |{
            |  "uniqueId": "CIS-123",
            |  "taxOfficeNumber": "123"
            |}
          """.stripMargin
        )

      jsonMissing.validate[CisTaxpayer].isError mustBe true
    }
  }
}