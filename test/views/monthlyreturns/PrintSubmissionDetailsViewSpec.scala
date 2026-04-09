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

package views.monthlyreturns

import base.SpecBase
import models.monthlyreturns.SubcontractorPayment
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.*
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.PrintSubmissionDetailsView

class PrintSubmissionDetailsViewSpec extends SpecBase {

  "PrintSubmissionDetailsView" - {

    "must render all details correctly including subcontractor table" in new Setup {

      val doc: Document = Jsoup.parse(html.toString)
      doc.title must include(messages("monthlyreturns.printSubmissionDetails.title", monthYear))

      doc.select("h1").text must include(
        messages("monthlyreturns.printSubmissionDetails.heading", monthYear)
      )

      doc.select("h2").text must include(
        messages("monthlyreturns.printSubmissionDetails.submissionDetails.heading")
      )

      doc.select("p.govuk-body").text must include(
        messages(
          "monthlyreturns.printSubmissionDetails.submissionDetails.p",
          submittedTime,
          submittedDate
        )
      )

      val summaryText = doc.select(".govuk-summary-list").text()
      summaryText must include(receiptReferenceNumber)
      summaryText must include(messages(submissionType))
      summaryText must include(contractorName)
      summaryText must include(payeReference)

      doc.select("h2").text must include(
        messages("monthlyreturns.printSubmissionDetails.paymentDetails.heading")
      )

      summaryText must include(subcontractors.size.toString)
      summaryText must include(messages(totalPaymentsMade))
      summaryText must include(totalCostOfMaterials)
      summaryText must include(totalTaxDeducted)

      doc.select("h2").text must include(
        messages("monthlyreturns.printSubmissionDetails.paymentsMadeToSubcontractors.heading")
      )

      val tableHead = doc.select("table thead tr").text()
      tableHead must include(
        messages("monthlyreturns.printSubmissionDetails.paymentsMadeToSubcontractors.subcontractor")
      )
      tableHead must include(
        messages("monthlyreturns.printSubmissionDetails.paymentsMadeToSubcontractors.paymentsMade")
      )
      tableHead must include(
        messages("monthlyreturns.printSubmissionDetails.paymentsMadeToSubcontractors.costOfMaterials")
      )
      tableHead must include(messages("monthlyreturns.printSubmissionDetails.paymentsMadeToSubcontractors.taxDeducted"))

      val tableRows = doc.select("table tbody tr")
      tableRows.size mustBe subcontractors.size
      subcontractors.zipWithIndex.foreach { case (sub, idx) =>
        val row = tableRows.get(idx).text()
        row must include(sub.name)
        row must include(sub.paymentsMade)
        row must include(sub.costOfMaterials)
        row must include(sub.taxDeducted)
      }

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.printSubmissionDetails.printThisPage.link")
      )

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.printSubmissionDetails.monthlyReturnHistory.link")
      )
    }
  }

  trait Setup {

    val app: Application                 = applicationBuilder().build()
    val view: PrintSubmissionDetailsView = app.injector.instanceOf[PrintSubmissionDetailsView]

    implicit val request: Request[_] = FakeRequest()

    implicit val messages: Messages =
      MessagesImpl(
        Lang.defaultLang,
        app.injector.instanceOf[MessagesApi]
      )

    val monthYear              = "April 2026"
    val submittedTime          = "8:46am"
    val submittedDate          = "16 March 2025"
    val receiptReferenceNumber = "ABC123456789"
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

    lazy val html: HtmlFormat.Appendable = view(
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
    )
  }
}
