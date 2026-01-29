package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class SubmitInactivityRequestPageSpec extends SpecBase {

  "TotalTaxDeductedPage" - {

    "have the correct path" in {
      SubmitInactivityRequestPage.path mustBe (JsPath \ "submitInactivityRequest")
    }

    "have the correct toString" in {
      SubmitInactivityRequestPage.toString mustBe "submitInactivityRequest"
    }
  }
  }