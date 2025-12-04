package controllers.monthlyreturns

import base.SpecBase
import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.NoSubcontractorsView

class NoSubcontractorsControllerSpec extends SpecBase {

  "NoSubcontractors Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.NoSubcontractorsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[NoSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, messages(application)).toString
      }
    }
  }
}
