package controllers.monthlyreturns

import base.SpecBase
import controllers.routes
import forms.monthlyreturns.ConfirmEmailAddressFormProvider
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

class SelectSubcontractorsControllerSpec extends SpecBase {

  "SelectSubcontractors Controller" - {
    val formProvider = new ConfirmEmailAddressFormProvider()
    val form = formProvider()
    val subcontractors = Seq(
      SelectSubcontractorsViewModel("Alice, A", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Bob, B", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Charles, C", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Dave, D", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Elise, E", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Frank, F", "Yes", "Unknown", "Unknown", false)
    )

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.SelectSubcontractorsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, subcontractors)(request, messages(application)).toString
      }
    }
  }
}
