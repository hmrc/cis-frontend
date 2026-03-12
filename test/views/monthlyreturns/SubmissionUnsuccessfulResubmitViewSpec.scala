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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.SubmissionUnsuccessfulResubmitView

class SubmissionUnsuccessfulResubmitViewSpec extends SpecBase {

  "SubmissionUnsuccessfulResubmitView" - {

    "must render the page with the correct content" in new Setup {
      val html: HtmlFormat.Appendable = view()
      val doc: Document               = Jsoup.parse(html.toString)

      doc.title             must include(messages("monthlyreturns.submissionUnsuccessfulResubmit.title"))
      doc.select("h1").text must include(messages("monthlyreturns.submissionUnsuccessfulResubmit.heading"))

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p1.links.prefix")
      )
      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p1.links.text")
      )
      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p1.links.suffix")
      )

      doc.select(".govuk-warning-text").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p2.warning")
      )

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p3.links.prefix")
      )
      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p3.links.text")
      )

      doc.select("p.govuk-body").text must include(messages("monthlyreturns.submissionUnsuccessfulResubmit.p4"))

      val listItems: Elements = doc.select("ul.govuk-list--bullet li")

      listItems.get(0).text mustBe messages("monthlyreturns.submissionUnsuccessfulResubmit.list.l1")
      listItems.get(1).text mustBe messages("monthlyreturns.submissionUnsuccessfulResubmit.list.l2")

      doc.select("p.govuk-body").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p5.links.text")
      )
      doc.select("a.govuk-link").text must include(
        messages("monthlyreturns.submissionUnsuccessfulResubmit.p5.links.text")
      )
    }
  }

  trait Setup {

    val app: Application                         = applicationBuilder().build()
    val view: SubmissionUnsuccessfulResubmitView = app.injector.instanceOf[SubmissionUnsuccessfulResubmitView]

    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               =
      play.api.i18n.MessagesImpl(
        play.api.i18n.Lang.defaultLang,
        app.injector.instanceOf[play.api.i18n.MessagesApi]
      )
  }
}
