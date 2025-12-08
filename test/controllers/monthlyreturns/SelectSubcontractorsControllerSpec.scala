/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.monthlyreturns

import base.SpecBase
import controllers.routes
import forms.monthlyreturns.{ConfirmEmailAddressFormProvider, SelectSubcontractorsFormProvider}
import models.monthlyreturns.SelectSubcontractorsFormData
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

class SelectSubcontractorsControllerSpec extends SpecBase {

  "SelectSubcontractors Controller" - {
    val formProvider   = new SelectSubcontractorsFormProvider()
    val form           = formProvider()
    val subcontractors = Seq(
      SelectSubcontractorsViewModel("Alice, A", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Bob, B", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Charles, C", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Dave, D", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Elise, E", "Yes", "Unknown", "Unknown", false),
      SelectSubcontractorsViewModel("Frank, F", "Yes", "Unknown", "Unknown", false)
    )

    val subcontractorsChecked = Seq(
      SelectSubcontractorsViewModel("Alice, A", "Yes", "Unknown", "Unknown", true),
      SelectSubcontractorsViewModel("Bob, B", "Yes", "Unknown", "Unknown", true),
      SelectSubcontractorsViewModel("Charles, C", "Yes", "Unknown", "Unknown", true),
      SelectSubcontractorsViewModel("Dave, D", "Yes", "Unknown", "Unknown", true),
      SelectSubcontractorsViewModel("Elise, E", "Yes", "Unknown", "Unknown", true),
      SelectSubcontractorsViewModel("Frank, F", "Yes", "Unknown", "Unknown", true)
    )

    val formData = SelectSubcontractorsFormData(
      false,
      List(
        true, true, true, true, true, true
      )
    )

    val formDataDeslected = SelectSubcontractorsFormData(
      false,
      List(
        false, false, false, false, false, false
      )
    )

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadNonEmpty(None).url
          )

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, subcontractors)(request, messages(application)).toString
      }
    }

    "must return OK with all checkboxes selected" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadNonEmpty(Some(true)).url
          )

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(formData), subcontractorsChecked)(
          request,
          messages(application)
        ).toString
      }
    }

    "must return OK with all checkboxes deselected" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadNonEmpty(Some(false)).url
          )

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(formDataDeslected), subcontractors)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
