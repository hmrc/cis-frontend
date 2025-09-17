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

package views.components

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import views.html.components.H2
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup

class H2Spec extends SpecBase with Matchers {

  "h2" - {
    "must render the correct heading text in the output HTML" in new Setup {
      val headingText = testText
      val html        = h2(headingText)
      val heading     = getHeadingElement(html)
      heading.size mustBe 1
      heading.text mustBe headingText
    }

    "must render with default CSS class" in new Setup {
      val html    = h2(testText)
      val heading = getHeadingElement(html)
      heading.attr("class") mustBe "govuk-heading-m"
    }

    "must render with custom CSS class" in new Setup {
      val customClass = "custom-heading-class"
      val html        = h2(testText, classes = customClass)
      val heading     = getHeadingElement(html)
      heading.attr("class") mustBe customClass
    }

    "must render with ID when provided" in new Setup {
      val id      = "sub-heading"
      val html    = h2(testText, id = Some(id))
      val heading = getHeadingElement(html)
      heading.attr("id") mustBe id
    }

    "must not render ID attribute when not provided" in new Setup {
      val html    = h2(testText)
      val heading = getHeadingElement(html)
      heading.hasAttr("id") mustBe false
    }

    "must render with both custom class and ID" in new Setup {
      val customClass = "custom-heading-class"
      val id          = "sub-heading"
      val html        = h2(testText, classes = customClass, id = Some(id))
      val heading     = getHeadingElement(html)
      heading.attr("class") mustBe customClass
      heading.attr("id") mustBe id
    }

    "must handle empty string text" in new Setup {
      val html    = h2("")
      val heading = getHeadingElement(html)
      heading.size mustBe 1
      heading.text mustBe ""
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val h2                                        = app.injector.instanceOf[H2]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val testText = "Test Heading"

    def getHeadingElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("h2")
    }
  }
}
