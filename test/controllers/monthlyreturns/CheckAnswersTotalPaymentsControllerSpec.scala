package controllers.monthlyreturns

import base.SpecBase
import controllers.routes
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.CheckAnswersTotalPaymentsView

class CheckAnswersTotalPaymentsControllerSpec extends SpecBase {

  val subcontractorName = "TyneWear Ltd"

  "CheckAnswersTotalPayments Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[CheckAnswersTotalPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(subcontractorName)(request, messages(application)).toString
      }
    }
  }
}
