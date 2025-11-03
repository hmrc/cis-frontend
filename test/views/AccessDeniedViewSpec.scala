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

package views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.AccessDeniedView

class AccessDeniedViewSpec extends SpecBase {

  "AccessDeniedView" - {

    "must render the page with the correct title, heading and link" in new Setup {
      private val html: HtmlFormat.Appendable = view()
      private val doc: Document               = Jsoup.parse(html.body)

      doc.title                                 must include(messages("accessDenied.title"))
      doc.select("h1").text                     must include(messages("accessDenied.heading"))
      doc.getElementsByClass("govuk-link").text must include(messages("accessDenied.link"))
    }
  }

  trait Setup {
    private val app: Application                  = applicationBuilder().build()
    val view: AccessDeniedView                    = app.injector.instanceOf[AccessDeniedView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
