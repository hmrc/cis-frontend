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
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.components.PrintLink

class PrintLinkSpec extends SpecBase {

  "PrintLink" - {

    "must render a govuk link inside a govuk-body paragraph with default id" in new Setup {
      val html: HtmlFormat.Appendable = printLink("monthlyreturns.submissionSuccessful.print")

      paragraph(html).size mustBe 1

      val link: Elements = linkById(html, defaultId)
      link.size mustBe 1
      link.attr("href") mustBe "#"
      link.attr("data-module") mustBe "hmrc-print-link"
      link.text() mustBe messages("monthlyreturns.submissionSuccessful.print")

      link.hasClass("govuk-link") mustBe true
      link.hasClass("hmrc-!-js-visible") mustBe true
      link.hasClass("govuk-!-display-none-print") mustBe true
    }

    "must apply the provided id" in new Setup {
      val html: HtmlFormat.Appendable =
        printLink("monthlyreturns.submissionSuccessful.print", id = "print-bottom")

      val link: Elements = linkById(html, "print-bottom")
      link.size mustBe 1
    }

    "must apply extra classes when provided" in new Setup {
      val html: HtmlFormat.Appendable =
        printLink(
          "monthlyreturns.submissionSuccessful.print",
          id = "print-extra",
          extraClasses = "custom-class"
        )

      val link: Elements = linkById(html, "print-extra")
      link.hasClass("custom-class") mustBe true
    }
  }

  trait Setup {
    private val app          = applicationBuilder().build()
    val printLink: PrintLink = app.injector.instanceOf[PrintLink]
    val defaultId            = "print-link"

    implicit val request: RequestHeader = FakeRequest()
    implicit val messages: Messages     =
      MessagesImpl(Lang.defaultLang, app.injector.instanceOf[MessagesApi])

    def docOf(html: HtmlFormat.Appendable): Document =
      Jsoup.parse(html.body)

    def select(html: HtmlFormat.Appendable, cssSelector: String): Elements =
      docOf(html).select(cssSelector)

    def paragraph(html: HtmlFormat.Appendable): Elements =
      select(html, "p.govuk-body")

    def linkById(html: HtmlFormat.Appendable, id: String): Elements =
      select(html, s"a.govuk-link#$id")
  }
}
