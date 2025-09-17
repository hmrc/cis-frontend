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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.scalatest.matchers.must.Matchers
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.components.PrintButton

class PrintButtonSpec extends SpecBase with Matchers {

  "PrintButton" - {

    "must render an <input type=\"button\"> with default id and secondary styling" in new Setup {
      val html: HtmlFormat.Appendable = printButton("monthlyreturns.submissionSuccessful.print")

      buttonGroup(html).size mustBe 1

      val input: Elements = inputById(html, "print-button")
      input.size mustBe 1
      input.attr("type") mustBe "button"
      input.hasClass("govuk-button--secondary") mustBe true
      input.attr("data-module") mustBe "govuk-button"
      input.attr("value") mustBe messages("monthlyreturns.submissionSuccessful.print")

      printScript(html).size mustBe 1
    }

    "must not include secondary styling when secondary = false" in new Setup {
      val html: HtmlFormat.Appendable = printButton("monthlyreturns.submissionSuccessful.print", id = "print-top", secondary = false)
      val input: Elements = inputById(html, "print-top")
      input.size mustBe 1
      input.hasClass("govuk-button--secondary") mustBe false
      input.attr("type") mustBe "button"
    }

    "must apply the provided id" in new Setup {
      val html: HtmlFormat.Appendable = printButton("monthlyreturns.submissionSuccessful.print", id = "print-bottom")
      inputById(html, "print-bottom").size mustBe 1
    }
  }

  trait Setup {
    private val app = applicationBuilder().build()
    val printButton: PrintButton = app.injector.instanceOf[PrintButton]

    implicit val request: RequestHeader = FakeRequest()
    implicit val messages: Messages = MessagesImpl(Lang.defaultLang, app.injector.instanceOf[MessagesApi])

    def docOf(html: HtmlFormat.Appendable): Document =
      Jsoup.parse(html.body)

    def select(html: HtmlFormat.Appendable, cssSelector: String): Elements =
      docOf(html).select(cssSelector)

    def buttonGroup(html: HtmlFormat.Appendable): Elements =
      select(html, "div.govuk-button-group")

    def inputById(html: HtmlFormat.Appendable, id: String): Elements =
      select(html, s"input.govuk-button#$id")

    def printScript(html: HtmlFormat.Appendable): Elements =
      select(html, """script[src*="/assets/javascripts/print.js"]""")
  }
}