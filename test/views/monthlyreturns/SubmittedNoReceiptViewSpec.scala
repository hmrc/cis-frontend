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

package views.monthlyreturns

import base.SpecBase
import models.ReturnType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import viewmodels.checkAnswers.monthlyreturns.SubmittedNoReceiptViewModel
import views.html.monthlyreturns.SubmittedNoReceiptView

class SubmittedNoReceiptViewSpec extends SpecBase {

  "SubmittedNoReceiptView" - {

    "must render the page when email is provided" in new Setup {

      val doc: Document = Jsoup.parse(html.toString)

      doc.title must include(
        messages("monthlyreturns.submittedNoReceipt.title", returnTypeMessage)
      )

      doc.select(".govuk-panel__title").text must include(
        messages("monthlyreturns.submittedNoReceipt.heading", returnTypeMessage)
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.p1", submittedTime, submittedDate)
      )

      doc.select("h2").text must include(
        messages("monthlyreturns.submittedNoReceipt.details.h2")
      )

      val summaryText: String = doc.select(".govuk-summary-list").text()

      summaryText must include(contractorName)
      summaryText must include(empRef)
      summaryText must include(messages(s"monthlyreturns.returnType.${submissionType.toString}"))
      summaryText must include(periodEnd)

      doc.select("p.govuk-body").text must include(email)

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.submissionHistory.p")
      )

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submittedNoReceipt.submissionHistory.link")
      )

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submittedNoReceipt.backToManageYourCISReturn.link")
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.questionsAboutReturnSubmission.p")
      )

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submittedNoReceipt.questionsAboutReturnSubmission.link")
      )

      doc.select("h2").text must include(
        messages("monthlyreturns.submittedNoReceipt.feedback.heading")
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.feedback.p1")
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.feedback.p2")
      )

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submittedNoReceipt.feedback.p2.link")
      )

      doc.select("h2").text must not include
        messages("monthlyreturns.submittedNoReceipt.whatHappensNext.heading")
    }

    "must render the page when email is empty" in new Setup {

      override val email: String = ""

      val doc: Document = Jsoup.parse(html.toString)

      doc.title must include(
        messages("monthlyreturns.submittedNoReceipt.title", returnTypeMessage)
      )

      doc.select(".govuk-panel__title").text must include(
        messages("monthlyreturns.submittedNoReceipt.heading", returnTypeMessage)
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.p1", submittedTime, submittedDate)
      )

      doc.select("h2").text must include(
        messages("monthlyreturns.submittedNoReceipt.details.h2")
      )

      val summaryText: String = doc.select(".govuk-summary-list").text()

      summaryText must include(contractorName)
      summaryText must include(empRef)
      summaryText must include(messages(s"monthlyreturns.returnType.${submissionType.toString}"))
      summaryText must include(periodEnd)

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.submissionHistory.p")
      )

      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submittedNoReceipt.submissionHistory.link")
      )

      doc.select("h2").text must include(
        messages("monthlyreturns.submittedNoReceipt.whatHappensNext.heading")
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submittedNoReceipt.whatHappensNext.p")
      )
    }
  }

  trait Setup {

    def email: String                = "test@test.com"
    val app: Application             = applicationBuilder().build()
    val view: SubmittedNoReceiptView = app.injector.instanceOf[SubmittedNoReceiptView]

    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               =
      play.api.i18n.MessagesImpl(
        play.api.i18n.Lang.defaultLang,
        app.injector.instanceOf[play.api.i18n.MessagesApi]
      )

    val periodEnd                  = "February 2026"
    val submittedTime              = "10:30am"
    val submittedDate              = "6 Jan 2026"
    val contractorName             = "Test Contractor Ltd"
    val empRef                     = "123/AB456"
    val submissionType: ReturnType = ReturnType.MonthlyNilReturn
    val cisId                      = "1"

    val returnTypeMessage: String = {
      val raw = messages(s"monthlyreturns.returnType.${submissionType.toString}")
      s"${raw.head.toUpper}${raw.tail.toLowerCase}"
    }

    lazy val vm: SubmittedNoReceiptViewModel = SubmittedNoReceiptViewModel(
      periodEnd = periodEnd,
      submittedTime = submittedTime,
      submittedDate = submittedDate,
      contractorName = contractorName,
      empRef = empRef,
      email = email,
      submissionType = submissionType,
      cisId = cisId
    )

    lazy val html: HtmlFormat.Appendable = view(vm)
  }
}
