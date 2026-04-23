package pages.amend

import base.SpecBase
import play.api.libs.json.JsPath

class WhichSubcontractorsToAddPageSpec extends SpecBase {

  "WhichSubcontractorsToAddPage" - {

    "have the correct path" in {
      WhichSubcontractorsToAddPage.path mustBe (JsPath \ "whichSubcontractorsToAdd")
    }

    "have the correct toString" in {
      WhichSubcontractorsToAddPage.toString mustBe "whichSubcontractorsToAdd"
    }
  }
}
