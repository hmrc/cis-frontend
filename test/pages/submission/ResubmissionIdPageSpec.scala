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

package pages.submission

import base.SpecBase
import models.UserAnswers
import play.api.libs.json.JsPath

class ResubmissionIdPageSpec extends SpecBase {

  "ResubmissionIdPage" - {

    "have the correct path" in {
      ResubmissionIdPage.path mustBe (JsPath \ "submission" \ "resubmissionId")
    }

    "have the correct toString" in {
      ResubmissionIdPage.toString mustBe "resubmissionId"
    }

    "set, get, and remove a value in UserAnswers" in {
      val ua1 = UserAnswers("test")
        .set(ResubmissionIdPage, 12345L)
        .success
        .value

      ua1.get(ResubmissionIdPage).value mustBe 12345L

      val ua2 = ua1
        .remove(ResubmissionIdPage)
        .success
        .value

      ua2.get(ResubmissionIdPage) mustBe None
    }
  }
}
