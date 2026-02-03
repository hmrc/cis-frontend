package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class EnterYourEmailAddressPageSpec extends SpecBase {

  "EnterYourEmailAddressPage" - {

    "have the correct path" in {
      EnterYourEmailAddressPage.path mustBe (JsPath \ "enterYourEmailAddress")
    }

    "have the correct toString" in {
      EnterYourEmailAddressPage.toString mustBe "enterYourEmailAddress"
    }
  }
}
