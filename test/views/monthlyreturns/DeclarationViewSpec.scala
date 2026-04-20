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
import models.NormalMode
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.DeclarationView

class DeclarationViewSpec extends SpecBase with Matchers {

  "DeclarationView" - {

    "must render the page with heading, paragraph and button" in new Setup {
      val html = view(NormalMode)
      val doc  = Jsoup.parse(html.body)

      doc.title             must include(messages("monthlyreturns.declaration.title"))
      doc.select("h1").text must include(messages("monthlyreturns.declaration.heading"))

      doc.select("p").text                        must include(messages("monthlyreturns.declaration.paragraph"))
      doc.getElementsByClass("govuk-button").text must include(messages("monthlyreturns.declaration.submit"))
    }

    trait Setup {
      val app                                       = applicationBuilder().build()
      val view                                      = app.injector.instanceOf[DeclarationView]
      implicit val request: play.api.mvc.Request[_] = FakeRequest()
      implicit val messages: Messages               = play.api.i18n.MessagesImpl(
        play.api.i18n.Lang.defaultLang,
        app.injector.instanceOf[play.api.i18n.MessagesApi]
      )
    }
  }
}
