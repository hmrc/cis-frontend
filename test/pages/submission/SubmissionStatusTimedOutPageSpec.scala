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
import org.scalatest.OptionValues.*
import org.scalatest.TryValues.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsPath

class SubmissionStatusTimedOutPageSpec extends AnyWordSpec with Matchers {

  "SubmissionStatusTimedOutPage" should {
    "have the correct path with submission ID" in {
      val page = SubmissionStatusTimedOutPage("sub-123")
      page.path mustBe (JsPath \ "submission" \ "sub-123" \ "timedOut")
    }

    "have the correct toString" in {
      val page = SubmissionStatusTimedOutPage("sub-123")
      page.toString mustBe "timedOut"
    }

    "set, get, and remove a value in UserAnswers" in {
      val page = SubmissionStatusTimedOutPage("sub-123")

      val ua1 = UserAnswers("test").set(page, true).success.value
      ua1.get(page).value mustBe true

      val ua2 = ua1.remove(page).success.value
      ua2.get(page) mustBe None
    }

    "handle false values correctly" in {
      val page = SubmissionStatusTimedOutPage("sub-123")

      val ua = UserAnswers("test").set(page, false).success.value
      ua.get(page).value mustBe false
    }

    "create different paths for different submission IDs" in {
      val page1 = SubmissionStatusTimedOutPage("sub-123")
      val page2 = SubmissionStatusTimedOutPage("sub-456")

      page1.path mustBe (JsPath \ "submission" \ "sub-123" \ "timedOut")
      page2.path mustBe (JsPath \ "submission" \ "sub-456" \ "timedOut")

      val ua = UserAnswers("test")
        .set(page1, true)
        .success
        .value
        .set(page2, false)
        .success
        .value

      ua.get(page1).value mustBe true
      ua.get(page2).value mustBe false
    }
  }
}
