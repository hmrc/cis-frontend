package pages.submission

import base.SpecBase
import play.api.libs.json.JsPath

class SubmissionJourneyCompletedPageSpec extends SpecBase {

  "SubmissionJourneyCompletedPage" - {

    "must have the correct path" in {
      SubmissionJourneyCompletedPage.path mustBe JsPath \ "submissionJourneyCompleted"
    }

    "must have the correct toString value" in {
      SubmissionJourneyCompletedPage.toString mustBe "submissionJourneyCompleted"
    }
  }
}
