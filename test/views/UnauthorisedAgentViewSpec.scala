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
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.UnauthorisedAgentView

class UnauthorisedAgentViewSpec extends SpecBase with Matchers {

  "UnauthorisedAgentViewSpec" - {

    "must render the page with the correct heading and paragraph" in new Setup {
      val html: HtmlFormat.Appendable = view()
      val doc: Document               = Jsoup.parse(html.body)

      doc.title             must include(messages("unauthorised.agent.title"))
      doc.select("h1").text must include(messages("unauthorised.agent.heading"))
      doc.select("p").text  must include(messages("unauthorised.agent.p1"))
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: UnauthorisedAgentView               = app.injector.instanceOf[UnauthorisedAgentView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
