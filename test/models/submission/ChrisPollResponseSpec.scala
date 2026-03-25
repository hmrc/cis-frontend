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
import play.api.libs.json.{JsSuccess, Json}

class ChrisPollResponseSpec extends AnyWordSpec with Matchers {

  "ChrisPollResponse JSON" should {

    "read with all fields present" in {
      val js = Json.parse(
        """
          |{
          |  "status": "PENDING",
          |  "pollUrl": "https://poll.example.com/submission/123",
          |  "intervalSeconds": 1,
          |  "lastMessageDate": "2026-03-20T10:15:30Z"
          |}
        """.stripMargin
      )

      js.validate[ChrisPollResponse] mustBe JsSuccess(
        ChrisPollResponse(
          "PENDING",
          Some("https://poll.example.com/submission/123"),
          Some(1),
          Some("2026-03-20T10:15:30Z")
        )
      )
    }

    "read when pollUrl is absent" in {
      val js = Json.parse(
        """
          |{
          |  "status": "SUBMITTED"
          |}
        """.stripMargin
      )

      val out = js.as[ChrisPollResponse]
      out.status mustBe "SUBMITTED"
      out.pollUrl mustBe None
      out.intervalSeconds mustBe None
      out.lastMessageDate mustBe None
    }

    "write to expected JSON with pollUrl present" in {
      val model = ChrisPollResponse(
        "SUBMITTED",
        Some("https://poll.example.com/submission/456"),
        Some(1),
        Some("2026-03-20T10:15:30Z")
      )
      val js    = Json.toJson(model)

      (js \ "status").as[String] mustBe "SUBMITTED"
      (js \ "pollUrl").as[String] mustBe "https://poll.example.com/submission/456"
      (js \ "intervalSeconds").as[Int] mustBe 1
      (js \ "lastMessageDate").as[String] mustBe "2026-03-20T10:15:30Z"
    }

    "write to expected JSON when pollUrl is None" in {
      val model = ChrisPollResponse("SUBMITTED", None, None, None)
      val js    = Json.toJson(model)

      (js \ "status").as[String] mustBe "SUBMITTED"
      (js \ "pollUrl").asOpt[String] mustBe None
      (js \ "intervalSeconds").asOpt[Int] mustBe None
      (js \ "lastMessageDate").asOpt[String] mustBe None
    }

    "round-trip (write then read) preserves values with pollUrl" in {
      val model = ChrisPollResponse(
        "ACCEPTED",
        Some("https://poll.example.com/submission/789"),
        Some(1),
        Some("2026-03-20T10:15:30Z")
      )

      val js  = Json.toJson(model)
      val out = js.as[ChrisPollResponse]

      out mustBe model
    }

    "round-trip (write then read) preserves values without pollUrl" in {
      val model = ChrisPollResponse("ACCEPTED", None, None, None)

      val js  = Json.toJson(model)
      val out = js.as[ChrisPollResponse]

      out mustBe model
    }

    "read various status values with pollUrl" in {
      val statuses = List("PENDING", "ACCEPTED", "SUBMITTED", "FATAL_ERROR", "DEPARTMENTAL_ERROR")

      statuses.foreach { status =>
        val js = Json.obj(
          "status"          -> status,
          "pollUrl"         -> "https://example.com/poll",
          "intervalSeconds" -> 1,
          "lastMessageDate" -> "2026-03-20T10:15:30Z"
        )

        val out = js.as[ChrisPollResponse]
        out.status mustBe status
        out.pollUrl mustBe Some("https://example.com/poll")
        out.intervalSeconds mustBe Some(1)
        out.lastMessageDate mustBe Some("2026-03-20T10:15:30Z")
      }
    }

    "read various status values without pollUrl" in {
      val statuses = List("PENDING", "ACCEPTED", "SUBMITTED", "FATAL_ERROR", "DEPARTMENTAL_ERROR")

      statuses.foreach { status =>
        val js = Json.obj(
          "status" -> status
        )

        val out = js.as[ChrisPollResponse]
        out.status mustBe status
        out.pollUrl mustBe None
        out.intervalSeconds mustBe None
        out.lastMessageDate mustBe None
      }
    }

    "fail to read when status field is missing" in {
      val jsMissing = Json.parse(
        """
          |{
          |  "pollUrl": "https://poll.example.com/submission/123",
          |  "intervalSeconds": 1,
          |  "lastMessageDate": "2026-03-20T10:15:30Z"
          |}
        """.stripMargin
      )

      val res = jsMissing.validate[ChrisPollResponse]
      res.isError mustBe true
      res.asEither.swap.exists(_.exists { case (path, errs) =>
        path.toString == "/status" && errs.nonEmpty
      }) mustBe true
    }

    "ignore unknown/extra fields on read" in {
      val js = Json.parse(
        """
          |{
          |  "status": "SUBMITTED",
          |  "pollUrl": "https://poll.example.com/submission/123",
          |  "intervalSeconds": 1,
          |  "lastMessageDate": "2026-03-20T10:15:30Z",
          |  "extraField": "ignored",
          |  "anotherField": 123
          |}
        """.stripMargin
      )

      val out = js.as[ChrisPollResponse]
      out.status mustBe "SUBMITTED"
      out.pollUrl mustBe Some("https://poll.example.com/submission/123")
      out.intervalSeconds mustBe Some(1)
      out.lastMessageDate mustBe Some("2026-03-20T10:15:30Z")
    }
  }
}
