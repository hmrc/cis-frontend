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

package views.components

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.Html
import views.html.components.WarningInsetText

class WarningInsetTextSpec extends SpecBase with Matchers {

  "WarningInsetText" - {

    "must render with correct warning text" in new Setup {
      val warningText = "Warning!"
      val html        = warningInsetText(warningText)

      val warning: Elements = getWarningText(html)
      warning.text must include(warningText)
    }

    "must render a single warning message" in new Setup {
      val html = warningInsetText("Warning!")

      val warnings = getWarningText(html)
      warnings.size mustBe 1
    }

    "must render a warning icon" in new Setup {
      val html = warningInsetText("Warning!")

      val icon = getWarningIcon(html)
      icon.size mustBe 1
    }

    "must apply govuk warning text styling" in new Setup {
      val html = warningInsetText("Warning!")

      val container = getWarningContainer(html)
      container.hasClass("govuk-warning-text") mustBe true
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val warningInsetText: WarningInsetText        = app.injector.instanceOf[WarningInsetText]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    def getWarningText(html: Html): Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select(".govuk-warning-text__text")
    }

    def getWarningIcon(html: Html): Elements =
      Jsoup.parse(html.body).select(".govuk-warning-text__icon")

    def getWarningContainer(html: Html): Elements =
      Jsoup.parse(html.body).select(".govuk-warning-text")
  }
}
