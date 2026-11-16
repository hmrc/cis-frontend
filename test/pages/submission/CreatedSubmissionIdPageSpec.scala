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
import play.api.libs.json.JsPath

class CreatedSubmissionIdPageSpec extends SpecBase {

  "CreatedSubmissionIdPage" - {

    "have the correct path" in {
      CreatedSubmissionIdPage("2026-11").path mustBe (JsPath \ "submissionCreated" \ "2026-11" \ "submissionId")
    }
  }

  "CreatedSubmissionIdForFlowPage" - {

    "have the correct path" in {
      CreatedSubmissionIdForFlowPage(
        "test-id",
        "2026-11"
      ).path mustBe (JsPath \ "monthlyReturn" \ "test-id" \ "submissionCreated" \ "2026-11" \ "submissionId")
    }
  }
}
