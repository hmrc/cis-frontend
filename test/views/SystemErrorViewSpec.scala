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
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.SystemErrorView

class SystemErrorViewSpec extends SpecBase {

  "SystemErrorView" - {

    "must render the page with the correct paragraphs, link and incident reference number" in new Setup {
      val incidentReferenceNumber = "P3JXIAHME5VASC10"
      val html                    = view(incidentReferenceNumber)
      val doc                     = Jsoup.parse(html.body)

      doc.title                                 must include(messages("systemError.title"))
      doc.select("h1").text                     must include(messages("systemError.heading"))
      doc.select("p").text                      must include(messages("systemError.p1"))
      doc.select("p").text                      must include(messages("systemError.p2"))
      doc.select("p").text                      must include(messages("systemError.p3"))
      doc.getElementsByClass("govuk-link").text must include(messages("systemError.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[SystemErrorView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
