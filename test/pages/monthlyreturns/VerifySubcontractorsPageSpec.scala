package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class VerifySubcontractorsPageSpec extends SpecBase {

  "VerifySubcontractorsPage" - {
    "have the correct path" in {
      VerifySubcontractorsPage.path mustBe (JsPath \ "verifySubcontractors")
    }

    "have the correct toString" in {
      VerifySubcontractorsPage.toString mustBe "verifySubcontractors"
    }

    "have the correct scoped path" in {
      VerifySubcontractorsPage("CIS-ORG-1").path mustBe (JsPath \ "monthlyReturn" \ "CIS-ORG-1" \ "verifySubcontractors")
    }

    "have the correct scoped toString" in {
      VerifySubcontractorsPage("CIS-ORG-1").toString mustBe "verifySubcontractors"
    }
  }
}
