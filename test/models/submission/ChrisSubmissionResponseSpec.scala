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
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.libs.json.{JsSuccess, Json}

class ChrisSubmissionResponseSpec extends AnyWordSpec with Matchers {

  "ResponseEndPointDto JSON" should {

    "read with both fields present" in {
      val js = Json.parse(
        """
          |{
          |  "url": "https://poll.example/CID123",
          |  "pollIntervalSeconds": 15
          |}
        """.stripMargin
      )

      js.validate[ResponseEndPointDto] mustBe JsSuccess(
        ResponseEndPointDto("https://poll.example/CID123", 15)
      )
    }

    "write to expected JSON" in {
      val ep = ResponseEndPointDto("https://poll.example/CID123", 15)
      val js = Json.toJson(ep)

      (js \ "url").as[String] mustBe "https://poll.example/CID123"
      (js \ "pollIntervalSeconds").as[Int] mustBe 15
    }
  }

  "ChrisSubmissionResponse JSON" should {

    "read with all fields including responseEndPoint and error" in {
      val js = Json.parse(
        """
          |{
          |  "submissionId": "sub-123",
          |  "status": "PENDING",
          |  "hmrcMarkGenerated": "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          |  "correlationId": "CID123",
          |  "gatewayTimestamp": "2025-01-01T00:00:00Z",
          |  "responseEndPoint": {
          |    "url": "https://poll.example/CID123",
          |    "pollIntervalSeconds": 10
          |  },
          |  "error": { "number": "5005", "type": "fatal", "text": "Boom" }
          |}
        """.stripMargin
      )

      val out = js.as[ChrisSubmissionResponse]

      out.submissionId mustBe "sub-123"
      out.status mustBe "PENDING"
      out.hmrcMarkGenerated mustBe "Dj5TVJDyRYCn9zta5EdySeY4fyA="
      out.correlationId.value mustBe "CID123"
      out.gatewayTimestamp.value mustBe "2025-01-01T00:00:00Z"

      val ep = out.responseEndPoint.value
      ep.url mustBe "https://poll.example/CID123"
      ep.pollIntervalSeconds mustBe 10

      val err = out.error.value
      (err \ "number").as[String] mustBe "5005"
      (err \ "type").as[String] mustBe "fatal"
      (err \ "text").as[String] mustBe "Boom"
    }

    "read when optional fields are absent" in {
      val js = Json.parse(
        """
          |{
          |  "submissionId": "sub-abc",
          |  "status": "SUBMITTED",
          |  "hmrcMarkGenerated": "Dj5TVJDyRYCn9zta5EdySeY4fyA="
          |}
        """.stripMargin
      )

      val out = js.as[ChrisSubmissionResponse]
      out.submissionId mustBe "sub-abc"
      out.status mustBe "SUBMITTED"
      out.hmrcMarkGenerated mustBe "Dj5TVJDyRYCn9zta5EdySeY4fyA="
      out.correlationId mustBe None
      out.responseEndPoint mustBe None
      out.gatewayTimestamp mustBe None
      out.error mustBe None
    }

    "ignore unknown/extra fields on read" in {
      val js = Json.parse(
        """
          |{
          |  "submissionId": "sub-123",
          |  "status": "ACCEPTED",
          |  "hmrcMarkGenerated": "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          |  "nextPollInSeconds": 12,
          |  "someExtra": "ignored"
          |}
        """.stripMargin
      )

      val out = js.as[ChrisSubmissionResponse]
      out.submissionId mustBe "sub-123"
      out.status mustBe "ACCEPTED"
      out.hmrcMarkGenerated mustBe "Dj5TVJDyRYCn9zta5EdySeY4fyA="
    }

    "fail to read when a required field is missing" in {
      val jsMissing = Json.parse(
        """
          |{
          |  "status": "PENDING",
          |  "hmrcMarkGenerated": "Dj5TVJDyRYCn9zta5EdySeY4fyA="
          |}
        """.stripMargin
      )

      val res = jsMissing.validate[ChrisSubmissionResponse]
      res.isError mustBe true
      res.asEither.swap.exists(_.exists { case (path, errs) =>
        path.toString() == "/submissionId" && errs.nonEmpty
      }) mustBe true
    }

    "round-trip (write then read) preserves values" in {
      val model = ChrisSubmissionResponse(
        submissionId = "sub-123",
        status = "PENDING",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        correlationId = Some("CID123"),
        responseEndPoint = Some(ResponseEndPointDto("https://poll.example/RT", 15)),
        gatewayTimestamp = Some("2025-01-02T03:04:05Z"),
        error = Some(Json.obj("number" -> "5005", "type" -> "business", "text" -> "Error"))
      )

      val js  = Json.toJson(model)
      val out = js.as[ChrisSubmissionResponse]

      out mustBe model
    }

  }
}
