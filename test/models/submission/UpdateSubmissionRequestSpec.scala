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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.libs.json.*
import java.time.Instant

class UpdateSubmissionRequestSpec extends AnyWordSpec with Matchers {

  "UpdateSubmissionRequest JSON" should {

    "read a full payload with all optional fields present" in {
      val js = Json.parse(
        """
          |{
          |  "instanceId": "1",
          |  "taxYear": 2025,
          |  "taxMonth": 3,
          |  "hmrcMarkGenerated": "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
          |  "submittableStatus": "ACCEPTED",
          |  "amendment": "N",
          |  "hmrcMarkGgis": "GGIRmark",
          |  "submissionRequestDate": "2025-01-02T03:04:05Z",
          |  "acceptedTime": "10:15:30 GMT",
          |  "emailRecipient": "test@test.com",
          |  "agentId": "AG123",
          |  "govtalkErrorCode": "1020",
          |  "govtalkErrorType": "fatal",
          |  "govtalkErrorMessage": "Boom"
          |}
        """.stripMargin
      )

      val out = js.as[UpdateSubmissionRequest]

      out.instanceId mustBe "1"
      out.taxYear mustBe 2025
      out.taxMonth mustBe 3
      out.hmrcMarkGenerated.value mustBe "Dj5TVJDyRYCn9zta5EdySeY4fyA="
      out.submittableStatus mustBe "ACCEPTED"
      out.amendment.value mustBe "N"
      out.hmrcMarkGgis.value mustBe "GGIRmark"
      out.submissionRequestDate.value mustBe Instant.parse("2025-01-02T03:04:05Z")
      out.acceptedTime.value mustBe "10:15:30 GMT"
      out.emailRecipient.value mustBe "test@test.com"
      out.agentId.value mustBe "AG123"
      out.govtalkErrorCode.value mustBe "1020"
      out.govtalkErrorType.value mustBe "fatal"
      out.govtalkErrorMessage.value mustBe "Boom"
    }

    "read when only required fields are present" in {
      val js = Json.parse(
        """
          |{
          |  "instanceId": "1",
          |  "taxYear": 2024,
          |  "taxMonth": 4,
          |  "submittableStatus": "PENDING"
          |}
        """.stripMargin
      )

      val out = js.as[UpdateSubmissionRequest]
      out.instanceId mustBe "1"
      out.taxYear mustBe 2024
      out.taxMonth mustBe 4
      out.submittableStatus mustBe "PENDING"

      out.hmrcMarkGenerated mustBe None
      out.amendment mustBe None
      out.hmrcMarkGgis mustBe None
      out.submissionRequestDate mustBe None
      out.acceptedTime mustBe None
      out.emailRecipient mustBe None
      out.agentId mustBe None
      out.govtalkErrorCode mustBe None
      out.govtalkErrorType mustBe None
      out.govtalkErrorMessage mustBe None
    }

    "ignore unknown fields" in {
      val js = Json.parse(
        """
          |{
          |  "instanceId": "1",
          |  "taxYear": 2025,
          |  "taxMonth": 7,
          |  "submittableStatus": "ACCEPTED",
          |  "extra": "ignored",
          |  "another": 123
          |}
        """.stripMargin
      )

      val out = js.as[UpdateSubmissionRequest]
      out.instanceId mustBe "1"
      out.taxYear mustBe 2025
      out.taxMonth mustBe 7
      out.submittableStatus mustBe "ACCEPTED"
    }

    "round-trip (write → read) preserves values" in {
      val model = UpdateSubmissionRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 12,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        submittableStatus = "SUBMITTED_NO_RECEIPT",
        amendment = Some("Y"),
        hmrcMarkGgis = Some("GGIRmark"),
        submissionRequestDate = Some(Instant.parse("2025-03-04T05:06:07Z")),
        acceptedTime = Some("11:22:33 GMT"),
        emailRecipient = Some("test@test.com"),
        agentId = Some("Agent"),
        govtalkErrorCode = Some("1020"),
        govtalkErrorType = Some("fatal"),
        govtalkErrorMessage = Some("Heads-up")
      )

      val js  = Json.toJson(model)
      val out = js.as[UpdateSubmissionRequest]
      out mustBe model
    }

    "fail when a required field is missing (instanceId)" in {
      val js = Json.parse(
        """
          |{
          |  "taxYear": 2025,
          |  "taxMonth": 1,
          |  "submittableStatus": "ACCEPTED"
          |}
        """.stripMargin
      )

      val res = js.validate[UpdateSubmissionRequest]
      res.asEither.fold(
        errs => errs.exists { case (path, _) => path.toString == "/instanceId" } mustBe true,
        _ => fail("Expected validation error for missing instanceId")
      )
    }

    "fail when a numeric field has wrong type (taxYear as string)" in {
      val js = Json.parse(
        """
          |{
          |  "instanceId": "1",
          |  "taxYear": "2025",
          |  "taxMonth": 2,
          |  "submittableStatus": "PENDING"
          |}
        """.stripMargin
      )

      val res = js.validate[UpdateSubmissionRequest]
      res.isError mustBe true
      res.asEither.fold(
        errs => errs.exists { case (path, _) => path.toString == "/taxYear" } mustBe true,
        _ => fail("Expected validation error for taxYear type mismatch")
      )
    }
  }
}
