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

package models.agent

import base.SpecBase
import org.scalatest.matchers.should.Matchers.shouldEqual
import play.api.libs.json.{JsSuccess, Json}

class AgentClientDataSpec extends SpecBase {

  "AgentClientData" - {

    "serialize to JSON and deserialize from JSON correctly" in {
      val data = AgentClientData(
        uniqueId = "123",
        taxOfficeNumber = "456",
        taxOfficeReference = "789",
        schemeName = Some("Test Scheme")
      )

      val json = Json.toJson(data)
      (json \ "uniqueId").as[String]           shouldEqual "123"
      (json \ "taxOfficeNumber").as[String]    shouldEqual "456"
      (json \ "taxOfficeReference").as[String] shouldEqual "789"
      (json \ "schemeName").asOpt[String]      shouldEqual Some("Test Scheme")

      val fromJson = Json.fromJson[AgentClientData](json)
      fromJson shouldEqual JsSuccess(data)
    }

    "handle None for schemeName in JSON" in {
      val data = AgentClientData(
        uniqueId = "abc",
        taxOfficeNumber = "def",
        taxOfficeReference = "ghi",
        schemeName = None
      )

      val json = Json.toJson(data)
      (json \ "schemeName").asOpt[String] shouldEqual None

      val fromJson = Json.fromJson[AgentClientData](json)
      fromJson shouldEqual JsSuccess(data)
    }
  }

}
