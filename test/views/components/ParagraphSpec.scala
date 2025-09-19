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
import views.html.components.Paragraph
import play.api.test.FakeRequest
import play.api.i18n.{DefaultMessagesApi, Lang, Messages, MessagesImpl}
import org.jsoup.Jsoup
import play.twirl.api.Html

class ParagraphSpec extends SpecBase with Matchers {

  "paragraph" - {
    "must render the correct paragraph text in the output HTML" in new Setup {
      val paragraphText = testText
      val html          = p(paragraphText)
      val para          = getParagraphElement(html)
      para.size mustBe 1
      para.text mustBe paragraphText
    }

    "must render with default CSS class" in new Setup {
      val html = p(testText)
      val para = getParagraphElement(html)
      para.attr("class").trim mustBe "govuk-body"
    }

    "must render with bold styling when enabled" in new Setup {
      val html      = p(testText, bold = true)
      val para      = getParagraphElement(html)
      val classAttr = para.attr("class")
      classAttr must include("govuk-body")
      classAttr must include("govuk-!-font-weight-bold")
    }

    "must not render bold styling when disabled" in new Setup {
      val html = p(testText, bold = false)
      val para = getParagraphElement(html)
      para.attr("class").trim mustBe "govuk-body"
    }

    "must render with custom CSS classes" in new Setup {
      val customClass = "custom-paragraph-class"
      val html        = p(testText, extraClasses = customClass)
      val para        = getParagraphElement(html)
      para.attr("class").trim mustBe s"govuk-body $customClass"
    }

    "must render with both bold styling and custom classes" in new Setup {
      val customClass = "custom-paragraph-class"
      val html        = p(testText, bold = true, extraClasses = customClass)
      val para        = getParagraphElement(html)
      para.attr("class").trim mustBe s"govuk-body $customClass govuk-!-font-weight-bold"
    }

    "must handle empty string text" in new Setup {
      val html = p("")
      val para = getParagraphElement(html)
      para.size mustBe 1
      para.text mustBe ""
    }

    "must handle empty extra classes" in new Setup {
      val html = p(testText, extraClasses = "")
      val para = getParagraphElement(html)
      para.attr("class").trim mustBe "govuk-body"
    }

    "must interpolate args into message text" in new Setup {
      val testMessages: Messages = MessagesImpl(
        Lang("en"),
        new DefaultMessagesApi(Map("en" -> Map("paragraph.with.arg" -> "Hello {0}")))
      )

      val html = p(message = "paragraph.with.arg", args = Seq("World"))(testMessages)
      val para = getParagraphElement(html)
      para.text mustBe "Hello World"
    }

    "must render Html args (e.g. a link)" in new Setup {
      val testMessages: Messages = MessagesImpl(
        Lang("en"),
        new DefaultMessagesApi(Map("en" -> Map("paragraph.with.html" -> "Contact {0}")))
      )

      val link = Html("""<a class="govuk-link" href="#">HMRC</a>""")
      val html = p(message = "paragraph.with.html", args = Seq(link))(testMessages)
      val para = getParagraphElement(html)

      para.text mustBe "Contact HMRC"
      Jsoup.parse(html.body).select("a.govuk-link").size mustBe 1
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val p                                         = app.injector.instanceOf[Paragraph]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val testText = "Test paragraph content."

    def getParagraphElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("p")
    }
  }
}
