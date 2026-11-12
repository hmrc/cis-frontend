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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.Instant

class ContractorSchemeSpec extends AnyWordSpec with Matchers {

  private val instant = Instant.parse("2025-01-02T03:04:05Z")

  "ContractorScheme JSON format" should {
    "round-trip a fully populated scheme" in {
      val model = ContractorScheme(
        1,
        "inst",
        "123PA00123456",
        "123",
        "AB456",
        Some("1234567890"),
        Some("Name"),
        Some("a@b.com"),
        Some("Y"),
        Some(2),
        Some("Y"),
        Some(3),
        Some(4),
        Some(instant),
        Some(5)
      )
      Json.toJson(model).validate[ContractorScheme] mustBe JsSuccess(model)
    }

    "read a minimal scheme with optionals defaulting to None" in {
      val json = Json.obj(
        "schemeId"                -> 1,
        "instanceId"              -> "inst",
        "accountsOfficeReference" -> "123PA00123456",
        "taxOfficeNumber"         -> "123",
        "taxOfficeReference"      -> "AB456"
      )
      json.as[ContractorScheme] mustBe ContractorScheme(1, "inst", "123PA00123456", "123", "AB456")
    }

    "fail when a required field is missing" in {
      Json.obj("schemeId" -> 1).validate[ContractorScheme].isError mustBe true
    }
  }

  "CreateContractorSchemeParams JSON format" should {
    "round-trip a fully populated value" in {
      val model = CreateContractorSchemeParams(
        "inst",
        "ao",
        "123",
        "AB456",
        Some("utr"),
        Some("n"),
        Some("e@x.com"),
        Some("Y"),
        Some(1),
        Some("N")
      )
      Json.toJson(model).validate[CreateContractorSchemeParams] mustBe JsSuccess(model)
    }

    "omit empty optionals when writing" in {
      val json = Json.toJson(CreateContractorSchemeParams("inst", "ao", "123", "AB456")).as[JsObject]
      json.keys mustBe Set("instanceId", "accountsOfficeReference", "taxOfficeNumber", "taxOfficeReference")
    }

    "fail when a required field is missing" in {
      Json.obj("instanceId" -> "inst").validate[CreateContractorSchemeParams].isError mustBe true
    }
  }

  "UpdateContractorSchemeParams JSON format" should {
    "round-trip a fully populated value" in {
      val model = UpdateContractorSchemeParams(
        9,
        "inst",
        "ao",
        "123",
        "AB456",
        Some("utr"),
        Some("n"),
        Some("e@x.com"),
        Some("Y"),
        Some(1),
        Some("N"),
        Some(7)
      )
      Json.toJson(model).validate[UpdateContractorSchemeParams] mustBe JsSuccess(model)
    }

    "read a minimal value" in {
      val json = Json.obj(
        "schemeId"                -> 9,
        "instanceId"              -> "inst",
        "accountsOfficeReference" -> "ao",
        "taxOfficeNumber"         -> "123",
        "taxOfficeReference"      -> "AB456"
      )
      json.as[UpdateContractorSchemeParams] mustBe UpdateContractorSchemeParams(9, "inst", "ao", "123", "AB456")
    }

    "fail when schemeId is not a number" in {
      Json
        .obj(
          "schemeId"                -> "x",
          "instanceId"              -> "inst",
          "accountsOfficeReference" -> "ao",
          "taxOfficeNumber"         -> "123",
          "taxOfficeReference"      -> "AB456"
        )
        .validate[UpdateContractorSchemeParams]
        .isError mustBe true
    }
  }

  "CreateSchemeResponse JSON format" should {
    "round-trip" in {
      Json.toJson(CreateSchemeResponse(42)) mustBe Json.obj("schemeId" -> 42)
      Json.obj("schemeId" -> 42).as[CreateSchemeResponse] mustBe CreateSchemeResponse(42)
    }

    "fail when schemeId is missing" in {
      Json.obj().validate[CreateSchemeResponse].isError mustBe true
    }
  }
}
