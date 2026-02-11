package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class SubcontractorDetailsAddedPageSpec extends SpecBase {

  "SubcontractorDetailsAddedPage" - {

    "must have the correct toString and path" in {
      SubcontractorDetailsAddedPage.toString mustBe "subcontractorDetailsAdded"
      SubcontractorDetailsAddedPage.path mustBe (JsPath \ "subcontractorDetailsAdded")
    }
  }
}
