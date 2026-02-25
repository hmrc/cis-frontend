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
import models.ReturnType
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.SubmissionSuccessView

class SubmissionSuccessViewSpec extends SpecBase {

  "SubmissionSuccessView" - {

    "must render the page with the correct content" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title                              must include(messages("monthlyreturns.submissionSuccess.title"))
      doc.select(".govuk-panel__title").text must include(
        messages("monthlyreturns.submissionSuccess.heading")
      )
      doc.select(".govuk-panel__body").text  must include(reference)
      doc.select("p.govuk-body").text        must include(
        messages("monthlyreturns.submissionSuccessful.submitted.line", submittedTime, submittedDate)
      )
      doc.select("h2").text                  must include(
        messages("monthlyreturns.submissionSuccessful.submissionDetails.heading")
      )

      val summaryText: String = doc.select(".govuk-summary-list").text()
      summaryText must include(contractorName)
      summaryText must include(empRef)
      summaryText must include(messages(s"monthlyreturns.returnType.${submissionType.toString}"))
      summaryText must include(periodEnd)

      doc.select("p.govuk-body").text      must include(
        messages("monthlyreturns.submissionSuccessful.confirmationOfSuccessfulSubmission", email)
      )
      doc.select("a.govuk-link").text      must include(
        messages("monthlyreturns.submissionSuccessful.submissionHistory.link")
      )
      doc.select(".govuk-inset-text").text must include(
        messages("monthlyreturns.submissionSuccessful.inset")
      )
      doc.select("a.govuk-link").text      must include(messages("monthlyreturns.submissionSuccessful.print"))
      doc.select("a.govuk-link").text      must include(
        messages("monthlyreturns.submissionSuccessful.backToManageYourCISReturn.link")
      )
      doc.select("a.govuk-link").text      must include(
        messages("monthlyreturns.submissionSuccessful.questionsAboutReturnSubmission.link")
      )
      doc.select("h2").text                must include(
        messages("monthlyreturns.submissionSuccessful.feedback.heading")
      )
      doc.select("p.govuk-body").text      must include(messages("monthlyreturns.submissionSuccessful.feedback.p1"))
      doc.select("a.govuk-link").text      must include(
        messages("monthlyreturns.submissionSuccessful.feedback.p2.link")
      )
    }
  }

  trait Setup {

    val app: Application            = applicationBuilder().build()
    val view: SubmissionSuccessView = app.injector.instanceOf[SubmissionSuccessView]

    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               =
      play.api.i18n.MessagesImpl(
        play.api.i18n.Lang.defaultLang,
        app.injector.instanceOf[play.api.i18n.MessagesApi]
      )

    val reference                  = "ABC1234567890123456789"
    val periodEnd                  = "5 Jan 2026"
    val submittedTime              = "10:30am"
    val submittedDate              = "6 Jan 2026"
    val contractorName             = "Test Contractor Ltd"
    val empRef                     = "123/AB456"
    val email                      = "test@test.com"
    val submissionType: ReturnType = ReturnType.MonthlyNilReturn

    val html: HtmlFormat.Appendable = view(
      reference = reference,
      periodEnd = periodEnd,
      submittedTime = submittedTime,
      submittedDate = submittedDate,
      contractorName = contractorName,
      empRef = empRef,
      email = email,
      submissionType = submissionType
    )
  }
}
