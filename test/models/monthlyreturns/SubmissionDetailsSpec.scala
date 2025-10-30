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

import models.submission.SubmissionDetails
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.Instant

class SubmissionDetailsSpec extends AnyWordSpec with Matchers {

  "SubmissionDetails JSON format" should {

    "round-trip (writes -> reads) with all fields populated" in {
      val submittedAt = Instant.parse("2025-01-15T10:30:00Z")
      val model       = SubmissionDetails(
        id = "sub-12345",
        status = "PENDING",
        irMark = "IR-MARK-67890",
        submittedAt = submittedAt
      )

      val js = Json.toJson(model)
      js.as[SubmissionDetails] mustBe model
    }

    "parse valid JSON with all fields" in {
      val json = Json.parse(
        """
          |{
          |  "id": "sub-12345",
          |  "status": "COMPLETED",
          |  "irMark": "IR-MARK-67890",
          |  "submittedAt": "2025-01-15T10:30:00Z"
          |}
        """.stripMargin
      )

      val parsed = json.as[SubmissionDetails]
      parsed.id mustBe "sub-12345"
      parsed.status mustBe "COMPLETED"
      parsed.irMark mustBe "IR-MARK-67890"
      parsed.submittedAt mustBe Instant.parse("2025-01-15T10:30:00Z")
    }

    "fail to parse when id field is missing" in {
      val jsonMissing = Json.parse(
        """
          |{
          |  "status": "PENDING",
          |  "irMark": "IR-MARK-67890",
          |  "submittedAt": "2025-01-15T10:30:00Z"
          |}
        """.stripMargin
      )

      jsonMissing.validate[SubmissionDetails].isError mustBe true
    }

    "fail to parse when status field is missing" in {
      val jsonMissing = Json.parse(
        """
          |{
          |  "id": "sub-12345",
          |  "irMark": "IR-MARK-67890",
          |  "submittedAt": "2025-01-15T10:30:00Z"
          |}
        """.stripMargin
      )

      jsonMissing.validate[SubmissionDetails].isError mustBe true
    }

    "fail to parse when irMark field is missing" in {
      val jsonMissing = Json.parse(
        """
          |{
          |  "id": "sub-12345",
          |  "status": "PENDING",
          |  "submittedAt": "2025-01-15T10:30:00Z"
          |}
        """.stripMargin
      )

      jsonMissing.validate[SubmissionDetails].isError mustBe true
    }

    "fail to parse when submittedAt field is missing" in {
      val jsonMissing = Json.parse(
        """
          |{
          |  "id": "sub-12345",
          |  "status": "PENDING",
          |  "irMark": "IR-MARK-67890"
          |}
        """.stripMargin
      )

      jsonMissing.validate[SubmissionDetails].isError mustBe true
    }

    "fail to parse when submittedAt has invalid format" in {
      val jsonInvalid = Json.parse(
        """
          |{
          |  "id": "sub-12345",
          |  "status": "PENDING",
          |  "irMark": "IR-MARK-67890",
          |  "submittedAt": "invalid-date"
          |}
        """.stripMargin
      )

      jsonInvalid.validate[SubmissionDetails].isError mustBe true
    }

    "serialize to JSON correctly" in {
      val submittedAt = Instant.parse("2025-01-15T10:30:00Z")
      val model       = SubmissionDetails(
        id = "sub-12345",
        status = "PENDING",
        irMark = "IR-MARK-67890",
        submittedAt = submittedAt
      )

      val json = Json.toJson(model)
      (json \ "id").as[String] mustBe "sub-12345"
      (json \ "status").as[String] mustBe "PENDING"
      (json \ "irMark").as[String] mustBe "IR-MARK-67890"
      (json \ "submittedAt").as[Instant] mustBe submittedAt
    }
  }
}
