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
import org.apache.pekko.actor.setup.Setup
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.NoSubcontractorsView

class NoSubcontractorsViewSpec extends SpecBase {

  "NoSubcontractorsView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.title             must include(messages("noSubcontractors.title"))
      doc.select("h1").text must include(messages("noSubcontractors.heading"))
      doc.select("p").text  must include(messages("noSubcontractors.p1"))
      doc.select("p").text  must include(messages("noSubcontractors.p2"))
      doc.select("th").text must include(messages("noSubcontractors.table.name.th"))
      doc.select("th").text must include(messages("noSubcontractors.table.verification.th"))
      doc.select("th").text must include(messages("noSubcontractors.table.verificationNumber.th"))
      doc.select("th").text must include(messages("noSubcontractors.table.tax.th"))
      doc.select("th").text must include(messages("noSubcontractors.table.month.th"))
      doc.select("td").text must include(messages("noSubcontractors.table.fill"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[NoSubcontractorsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view()
  }
}
