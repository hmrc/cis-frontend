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
import models.monthlyreturns.SubcontractorPayment
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.monthlyreturns.PrintSubmissionDetailsView

class PrintSubmissionDetailsControllerSpec extends SpecBase {

  "PrintSubmissionDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request =
          FakeRequest(GET, controllers.monthlyreturns.routes.PrintSubmissionDetailsController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[PrintSubmissionDetailsView]

        val monthYear              = "April 2026"
        val submittedTime          = "8:46am"
        val submittedDate          = "16 March 2025"
        val receiptReferenceNumber = "6QEDAHDREBY455GDNCPMDCNDFBDBJSJSJDNDDHDJDZ5"
        val submissionType         = "Monthly return"
        val contractorName         = "PAL 355 Scheme"
        val payeReference          = "123/AB456"
        val totalPaymentsMade      = "£1900"
        val totalCostOfMaterials   = "£616"
        val totalTaxDeducted       = "£380"

        val subcontractors = Seq(
          SubcontractorPayment("BuildRight Construction", "£165", "£95", "£95"),
          SubcontractorPayment("Northern Trades Ltd", "£75", "£55", "£55"),
          SubcontractorPayment("TyneWear Ltd", "£165", "£125", "£55")
        )

        val expectedHtml =
          view(
            monthYear = monthYear,
            submittedTime = submittedTime,
            submittedDate = submittedDate,
            receiptReferenceNumber = receiptReferenceNumber,
            submissionType = submissionType,
            contractorName = contractorName,
            payeReference = payeReference,
            totalPaymentsMade = totalPaymentsMade,
            totalCostOfMaterials = totalCostOfMaterials,
            totalTaxDeducted = totalTaxDeducted,
            subcontractors = subcontractors
          )(request, messages(application)).toString

        status(result) mustEqual OK
        contentAsString(result) mustEqual expectedHtml
      }
    }
  }
}
