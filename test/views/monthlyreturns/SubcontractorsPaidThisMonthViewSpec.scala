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
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.SubcontractorsPaidThisMonthView

class SubcontractorsPaidThisMonthViewSpec extends SpecBase {

  "SubcontractorsPaidThisMonthView" - {
    "must render the page with the right header, paragraphs, links and table content" in new Setup {
      val html: HtmlFormat.Appendable = view(schemeRef, periodEnding, selectedSubcontractors)
      val doc: Document               = Jsoup.parse(html.body)

      doc.title             must include(messages("monthlyreturns.subcontractorsPaidThisMonth.title"))
      doc.select("h1").text must include(messages("monthlyreturns.subcontractorsPaidThisMonth.heading"))

      doc.select("p").text must include(
        messages("monthlyreturns.subcontractorsPaidThisMonth.p1", selectedSubcontractors.size)
      )
      doc.select("p").text must include(messages("monthlyreturns.subcontractorsPaidThisMonth.p2"))

      doc.getElementsByClass("govuk-link").text must include(
        messages("monthlyreturns.subcontractorsPaidThisMonth.table.addDetails.link")
      )
      doc.getElementsByClass("govuk-link").text must include(
        messages("monthlyreturns.subcontractorsPaidThisMonth.table.remove.link")
      )
      doc.getElementsByClass("govuk-link").text must include(
        messages("monthlyreturns.subcontractorsPaidThisMonth.addAnotherSubcontractor.link")
      )
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: SubcontractorsPaidThisMonthView     = app.injector.instanceOf[SubcontractorsPaidThisMonthView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val schemeRef                           = "Scheme 123, Ref 123/AB456"
    val periodEnding                        = "Period ending 5 January 2026"
    val selectedSubcontractors: Seq[String] = Seq("BuildRight Construction", "Northern Trades Ltd", "TyneWear Ltd")
  }
}
