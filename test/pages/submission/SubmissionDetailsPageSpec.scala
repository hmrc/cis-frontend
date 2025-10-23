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

package pages.submission

import models.UserAnswers
import models.submission.SubmissionDetails
import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsPath

import java.time.Instant

class SubmissionDetailsPageSpec extends AnyWordSpec with Matchers {

  "SubmissionDetailsPage" should {
    "have the correct path" in {
      SubmissionDetailsPage.path mustBe (JsPath \ "submission" \ "submissionDetails")
    }

    "have the correct toString" in {
      SubmissionDetailsPage.toString mustBe "submissionDetails"
    }

    "set, get, and remove a value in UserAnswers" in {
      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )

      val ua1 = UserAnswers("test").set(SubmissionDetailsPage, submissionDetails).success.value
      ua1.get(SubmissionDetailsPage).value mustBe submissionDetails

      val ua2 = ua1.remove(SubmissionDetailsPage).success.value
      ua2.get(SubmissionDetailsPage) mustBe None
    }
  }
}
