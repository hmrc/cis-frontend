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

package views.contractordetails

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.contractordetails.ContractorDetailsView

class ContractorDetailsViewSpec extends SpecBase {

  "ContractorDetailsView" - {

    "should display the correct title" in new Setup {
      doc.title must include(messages("contractorDetails.title"))
    }

    "should display the correct heading" in new Setup {
      doc.select("h1").text must include(messages("contractorDetails.heading"))
    }

    "should display paragraph 1" in new Setup {
      doc.select("p").text() must include(messages("contractorDetails.p1"))
    }

    "should display paragraph 2" in new Setup {
      doc.select("p").text() must include(messages("contractorDetails.p2"))
    }

    "should display the Continue button" in new Setup {
      doc.select("button").text() must include(messages("site.continue"))
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: ContractorDetailsView               = app.injector.instanceOf[ContractorDetailsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
    val html: HtmlFormat.Appendable               = view()
    val doc: Document                             = Jsoup.parse(html.body)
  }
}
