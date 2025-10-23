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
import play.api.libs.json.*

class CreateAndTrackSubmissionResponseSpec extends AnyWordSpec with Matchers {

  "read when required field is present" in {
    val js = Json.parse("""{ "submissionId": "sub-123" }""")
    js.as[CreateAndTrackSubmissionResponse] mustBe
      CreateAndTrackSubmissionResponse("sub-123")
  }

  "fail to read when submissionId is missing" in {
    val js  = Json.parse("""{ }""")
    val res = js.validate[CreateAndTrackSubmissionResponse]

    res.asEither.fold(
      errs => errs.exists { case (path, _) => path.toString == "/submissionId" } mustBe true,
      _ => fail("Expected validation to fail when submissionId is missing")
    )
  }

  "ignore unknown/extra fields" in {
    val js = Json.parse(
      """
        |{
        |  "submissionId": "sub-123",
        |  "extra": "ignored",
        |  "another": 123
        |}
      """.stripMargin
    )
    js.as[CreateAndTrackSubmissionResponse] mustBe
      CreateAndTrackSubmissionResponse("sub-123")
  }

  "write to expected JSON" in {
    val model = CreateAndTrackSubmissionResponse("sub-123")
    val js    = Json.toJson(model)

    (js \ "submissionId").as[String] mustBe "sub-123"
    js.as[JsObject].keys must contain("submissionId")
  }

  "round-trip (write then read) preserves values" in {
    val model = CreateAndTrackSubmissionResponse("sub-123")
    Json.toJson(model).as[CreateAndTrackSubmissionResponse] mustBe model
  }

}
