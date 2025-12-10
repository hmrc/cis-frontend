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
import views.html.monthlyreturns.FileYourMonthlyCisReturnView

class FileYourMonthlyCisReturnViewSpec extends SpecBase {

  "FileYourMonthlyCisReturnView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.title             must include(messages("fileYourMonthlyCisReturn.title"))
      doc.select("h1").text must include(messages("fileYourMonthlyCisReturn.heading"))
      doc.select("p").text  must include(messages("fileYourMonthlyCisReturn.p1"))
      doc.select("p").text  must include(messages("fileYourMonthlyCisReturn.p2"))

      val startButton = doc.select("a.govuk-button--start")
      startButton.isEmpty mustBe false
      startButton.text must include("Start")
      startButton.attr("href") mustBe "#"
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[FileYourMonthlyCisReturnView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view()
  }
}
