package config

import base.SpecBase
import play.api.Application

class FrontendAppConfigSpec extends SpecBase {
  "FrontendAppConfig" - {
    "must contain correct values for the provided configuration" in new Setup {
      appConfig.manageYourCisReturn mustBe
        "http://localhost:6996/construction-industry-scheme/management/manage-cis-return/"
    }
  }

  trait Setup {
    val app: Application             = applicationBuilder().build()
    val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]
  }
}
