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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import play.api.libs.json.JsPath
import models.UserAnswers

class SubmissionStatusPageSpec extends AnyWordSpec with Matchers {
  "SubmissionStatusPage" should {
    "have the correct path" in {
      SubmissionStatusPage.path mustBe (JsPath \ "submission" \ "status")
    }
    "have the correct toString" in {
      SubmissionStatusPage.toString mustBe "submissionStatus"
    }
    "set, get, and remove a value in UserAnswers" in {
      val ua1 = UserAnswers("test").set(SubmissionStatusPage, "PENDING").success.value
      ua1.get(SubmissionStatusPage).value mustBe "PENDING"
      val ua2 = ua1.remove(SubmissionStatusPage).success.value
      ua2.get(SubmissionStatusPage) mustBe None
    }
  }
}
