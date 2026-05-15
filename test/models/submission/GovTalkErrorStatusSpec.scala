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

import models.submission.GovTalkErrorStatus.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

class GovTalkErrorStatusSpec extends AnyWordSpec with Matchers {

  "GovTalkErrorStatus JSON" should {

    "round-trip RecoverableError" in {
      val model = RecoverableError("3000", "boom")
      val json  = Json.parse("""{ "kind": "RecoverableError", "errorCode": "3000", "errorText": "boom" }""")

      Json.toJson(model: GovTalkErrorStatus) mustBe json
      json.as[GovTalkErrorStatus] mustBe model
    }

    "round-trip FatalError" in {
      val model = FatalError("9999", "fatal text")
      val json  = Json.parse("""{ "kind": "FatalError", "errorCode": "9999", "errorText": "fatal text" }""")

      Json.toJson(model: GovTalkErrorStatus) mustBe json
      json.as[GovTalkErrorStatus] mustBe model
    }

    "round-trip DepartmentalError" in {
      val model = DepartmentalError("dept text")
      val json  = Json.parse("""{ "kind": "DepartmentalError", "errorText": "dept text" }""")

      Json.toJson(model: GovTalkErrorStatus) mustBe json
      json.as[GovTalkErrorStatus] mustBe model
    }

    "round-trip ServerError" in {
      val model = ServerError(503)
      val json  = Json.parse("""{ "kind": "ServerError", "httpStatus": 503 }""")

      Json.toJson(model: GovTalkErrorStatus) mustBe json
      json.as[GovTalkErrorStatus] mustBe model
    }

    "round-trip NoResponse" in {
      val model: GovTalkErrorStatus = NoResponse
      val json                      = Json.parse("""{ "kind": "NoResponse" }""")

      Json.toJson(model) mustBe json
      json.as[GovTalkErrorStatus] mustBe NoResponse
    }

    "round-trip OtherStatus" in {
      val model: GovTalkErrorStatus = OtherStatus
      val json                      = Json.parse("""{ "kind": "OtherStatus" }""")

      Json.toJson(model) mustBe json
      json.as[GovTalkErrorStatus] mustBe OtherStatus
    }

    "fail to read an unknown kind" in {
      val json = Json.parse("""{ "kind": "Bogus" }""")

      json.validate[GovTalkErrorStatus].isError mustBe true
    }
  }
}
