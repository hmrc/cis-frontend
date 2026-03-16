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

package models.submission

import models.submission.SubcontractorType.SoleTrader
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*

class ChrisStandardMonthlyReturnSpec extends AnyWordSpec with Matchers {

  "ChrisStandardDeclarations JSON format" should {
    "round-trip" in {
      val model = ChrisStandardDeclarations(
        employmentStatus = "yes",
        verification = "yes"
      )

      val json = Json.obj(
        "employmentStatus" -> "yes",
        "verification"     -> "yes"
      )

      Json.toJson(model) mustBe json
      json.validate[ChrisStandardDeclarations] mustBe JsSuccess(model)
      Json.toJson(model).validate[ChrisStandardDeclarations].get mustBe model
    }
  }

  "ChrisPersonName JSON format" should {
    "round-trip with middle name" in {
      val model = ChrisPersonName(first = "Chris", middle = Some("J"), last = "Smith")

      val json = Json.obj(
        "first"  -> "Chris",
        "middle" -> "J",
        "last"   -> "Smith"
      )

      Json.toJson(model) mustBe json
      json.validate[ChrisPersonName] mustBe JsSuccess(model)
    }

    "round-trip without middle name" in {
      val model = ChrisPersonName(first = "Chris", middle = None, last = "Smith")

      val json = Json.obj(
        "first" -> "Chris",
        "last"  -> "Smith"
      )

      Json.toJson(model) mustBe json
      json.validate[ChrisPersonName] mustBe JsSuccess(model)
    }
  }

  "ChrisStandardSubcontractor JSON format" should {
    "round-trip (minimal fields + a couple of optionals)" in {
      val model = ChrisStandardSubcontractor(
        subcontractorType = SoleTrader,
        name = Some(ChrisPersonName("Chris", None, "Smith")),
        tradingName = None,
        partnershipTradingName = None,
        utr = Some("1234567890"),
        crn = None,
        nino = Some("AA123456A"),
        verificationNumber = None,
        totalPayments = Some(BigDecimal("1000.50")),
        costOfMaterials = None,
        totalDeducted = Some(BigDecimal("200.00"))
      )

      val json = Json.obj(
        "subcontractorType" -> Json.toJson(model.subcontractorType),
        "name"              -> Json.obj(
          "first" -> "Chris",
          "last"  -> "Smith"
        ),
        "utr"               -> "1234567890",
        "nino"              -> "AA123456A",
        "totalPayments"     -> BigDecimal("1000.50"),
        "totalDeducted"     -> BigDecimal("200.00")
      )

      Json.toJson(model) mustBe json
      json.validate[ChrisStandardSubcontractor] mustBe JsSuccess(model)
    }
  }

  "ChrisStandardMonthlyReturn JSON format" should {
    "round-trip" in {
      val subcontractor = ChrisStandardSubcontractor(
        subcontractorType = SoleTrader,
        name = Some(ChrisPersonName("Chris", Some("J"), "Smith")),
        tradingName = None,
        partnershipTradingName = None,
        utr = Some("1234567890"),
        crn = None,
        nino = None,
        verificationNumber = Some("V123456"),
        totalPayments = Some(BigDecimal("500.00")),
        costOfMaterials = Some(BigDecimal("50.00")),
        totalDeducted = Some(BigDecimal("100.00"))
      )

      val model = ChrisStandardMonthlyReturn(
        subcontractors = Seq(subcontractor),
        declarations = ChrisStandardDeclarations(employmentStatus = "yes", verification = "yes")
      )

      val json = Json.obj(
        "subcontractors" -> Json.arr(
          Json.obj(
            "subcontractorType"  -> Json.toJson(subcontractor.subcontractorType),
            "name"               -> Json.obj(
              "first"  -> "Chris",
              "middle" -> "J",
              "last"   -> "Smith"
            ),
            "utr"                -> "1234567890",
            "verificationNumber" -> "V123456",
            "totalPayments"      -> BigDecimal("500.00"),
            "costOfMaterials"    -> BigDecimal("50.00"),
            "totalDeducted"      -> BigDecimal("100.00")
          )
        ),
        "declarations"   -> Json.obj(
          "employmentStatus" -> "yes",
          "verification"     -> "yes"
        )
      )

      Json.toJson(model) mustBe json
      json.validate[ChrisStandardMonthlyReturn] mustBe JsSuccess(model)
      Json.toJson(model).validate[ChrisStandardMonthlyReturn].get mustBe model
    }
  }
}
