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
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.monthlyreturns.SelectSubcontractorsFormData
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

class SelectSubcontractorsControllerSpec extends SpecBase {

  "SelectSubcontractors Controller" - {
    val formProvider   = new SelectSubcontractorsFormProvider()
    val form           = formProvider()
    val subcontractors = Seq(
      SelectSubcontractorsViewModel(1, "Alice, A", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(2, "Bob, B", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(3, "Charles, C", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(4, "Dave, D", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(5, "Elise, E", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(6, "Frank, F", "Yes", "Unknown", "Unknown")
    )
    val formData       = SelectSubcontractorsFormData(
      List(
        1, 2, 3, 4, 5, 6
      )
    )

    val formDataDeselected = SelectSubcontractorsFormData(Nil)

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

    "must return OK with no data for a GET /empty" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadEmpty().url
          )

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, Nil)(request, messages(application)).toString
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
        contentAsString(result) mustEqual view(form.fill(formData), subcontractors)(
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
        contentAsString(result) mustEqual view(form.fill(formDataDeselected), subcontractors)(
          request,
          messages(application)
        ).toString
      }
    }

    "onSubmit must return view and retain form data" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url
          ).withBody(
            AnyContentAsFormUrlEncoded(
              Map(
                "subcontractorsToInclude.0" -> Seq("1"),
                "subcontractorsToInclude.1" -> Seq("2"),
                "subcontractorsToInclude.2" -> Seq("3"),
                "subcontractorsToInclude.3" -> Seq("4"),
                "subcontractorsToInclude.4" -> Seq("5"),
                "subcontractorsToInclude.5" -> Seq("6")
              )
            )
          )

        val result = route(application, request).value

        val view = application.injector.instanceOf[SelectSubcontractorsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form.fill(formData),
          subcontractors
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "onSubmit must return BAD_REQUEST and the view with errors when invalid data is submitted" in {
      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(
            POST,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url
          ).withBody(
            AnyContentAsFormUrlEncoded(
              Map(
                "subcontractorsToInclude.0" -> Seq("invalid")
              )
            )
          )

        val result = route(application, request).value

        val view      = application.injector.instanceOf[SelectSubcontractorsView]
        val boundForm = form.bind(
          Map(
            "subcontractorsToInclude.0" -> "invalid"
          )
        )

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, subcontractors)(
          request,
          messages(application)
        ).toString
      }
    }
  }
}
