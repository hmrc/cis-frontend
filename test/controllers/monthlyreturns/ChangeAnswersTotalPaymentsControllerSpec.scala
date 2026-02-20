/*
 * Copyright 2026 HM Revenue & Customs
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
import models.monthlyreturns.SelectedSubcontractor
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import viewmodels.checkAnswers.monthlyreturns.ChangeAnswersTotalPaymentsViewModel
import views.html.monthlyreturns.ChangeAnswersTotalPaymentsView

class ChangeAnswersTotalPaymentsControllerSpec extends SpecBase {

  val subcontractorName             = "Tyne Test Ltd"
  val totalPaymentsToSubcontractors = 1200
  val totalCostOfMaterials          = 500
  val totalCisDeductions            = 240
  val index                         = 1

  val subcontractor = SelectedSubcontractor(
    id = 1,
    name = subcontractorName,
    totalPaymentsMade = Some(totalPaymentsToSubcontractors),
    costOfMaterials = Some(totalCostOfMaterials),
    totalTaxDeducted = Some(totalCisDeductions)
  )

  val viewModel: ChangeAnswersTotalPaymentsViewModel = ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor)

  "ChangeAnswersTotalPayments Controller" - {

    "must return OK and the correct view for a GET" in {

      val userAnswersWithSubcontractor =
        emptyUserAnswers.set(SelectedSubcontractorPage(1), subcontractor).success.value

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSubcontractor)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController.onPageLoad(1).url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ChangeAnswersTotalPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(viewModel, index)(request, messages(application)).toString
      }
    }

    "must redirect to SystemError if subcontractor data is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(GET, controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController.onPageLoad(1).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.SystemErrorController.onPageLoad().url)
      }
    }
  }
}
