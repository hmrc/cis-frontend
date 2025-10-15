package controllers.monthlyreturns

import base.SpecBase
import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.AlreadySubmittedView

class AlreadySubmittedControllerSpec extends SpecBase {

  "AlreadySubmitted Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AlreadySubmittedView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view()(request, applicationConfig, messages(application)).toString
      }
    }
  }
}
