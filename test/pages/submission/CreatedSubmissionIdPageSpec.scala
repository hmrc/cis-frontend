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
