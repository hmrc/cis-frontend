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
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.JourneyRecoveryStartAgainView

class JourneyRecoveryStartAgainViewSpec extends SpecBase with Matchers {

  "JourneyRecoveryStartAgainView" - {

    "must render the page with the correct heading and paragraph with link" in new Setup {
      val html = view()
      val doc  = Jsoup.parse(html.body)

      doc.title                                 must include(messages("journeyRecovery.startAgain.title"))
      doc.select("h1").text                     must include(messages("journeyRecovery.startAgain.heading"))
      doc.select("p").text                      must include(messages("journeyRecovery.startAgain.guidance.p1"))
      doc.select("p").text                      must include(messages("journeyRecovery.startAgain.guidance.p2"))
      doc.getElementsByClass("govuk-link").text must include(messages("journeyRecovery.startAgain.guidance.link"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[JourneyRecoveryStartAgainView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
