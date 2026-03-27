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
import forms.monthlyreturns.DeleteNilMonthlyReturnFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.DeleteNilMonthlyReturnView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DeleteNilMonthlyReturnViewSpec extends SpecBase {

  "DeleteNilMonthlyReturnView" - {

    "must render the page with the correct html elements (English) including monthYear" in new Setup {
      val doc: Document = Jsoup.parse(htmlEn.toString)

      doc.title must include(messagesEn("monthlyreturns.deleteNilMonthlyReturn.title", monthYearEn))
      doc.title must include(monthYearEn)

      doc.select("h1").text must include(messagesEn("monthlyreturns.deleteNilMonthlyReturn.heading", monthYearEn))
      doc.select("h1").text must include(monthYearEn)

      doc.getElementsByClass("govuk-button").text must include(messagesEn("site.continue"))
    }

    "must render the page with the correct html elements (Welsh) including monthYear" in new Setup {
      val doc: Document = Jsoup.parse(htmlCy.toString)

      doc.title must include(messagesCy("monthlyreturns.deleteNilMonthlyReturn.title", monthYearCy))
      doc.title must include(monthYearCy)

      doc.select("h1").text must include(messagesCy("monthlyreturns.deleteNilMonthlyReturn.heading", monthYearCy))
      doc.select("h1").text must include(monthYearCy)

      doc.getElementsByClass("govuk-button").text must include(messagesCy("site.continue"))
    }

    "must render radio buttons with correct values (English)" in new Setup {
      val doc: Document = Jsoup.parse(htmlEn.toString)

      doc.select("input[type=radio][value=true]").size() mustBe 1
      doc.select("input[type=radio][value=false]").size() mustBe 1

      doc.select("label[for=value_0]").text() must include(messagesEn("site.yes"))
      doc.select("label[for=value_1]").text() must include(messagesEn("site.no"))
    }

    "must render radio buttons with correct values (Welsh)" in new Setup {
      val doc: Document = Jsoup.parse(htmlCy.toString)

      doc.select("input[type=radio][value=true]").size() mustBe 1
      doc.select("input[type=radio][value=false]").size() mustBe 1

      doc.select("label[for=value_0]").text() must include(messagesCy("site.yes"))
      doc.select("label[for=value_1]").text() must include(messagesCy("site.no"))
    }

    "must pre-populate the form when user has previously answered 'true' (English)" in new Setup {
      val filledForm: Form[Boolean]         = form.fill(true)
      val filledHtml: HtmlFormat.Appendable = view(filledForm, monthYearEn, NormalMode)(request, messagesEn)
      val doc: Document                     = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=true]").hasAttr("checked") mustBe true
      doc.select("input[value=false]").hasAttr("checked") mustBe false
    }

    "must pre-populate the form when user has previously answered 'false' (Welsh)" in new Setup {
      val filledForm: Form[Boolean]         = form.fill(false)
      val filledHtml: HtmlFormat.Appendable = view(filledForm, monthYearCy, NormalMode)(request, messagesCy)
      val doc: Document                     = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=true]").hasAttr("checked") mustBe false
      doc.select("input[value=false]").hasAttr("checked") mustBe true
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: DeleteNilMonthlyReturnView          = app.injector.instanceOf[DeleteNilMonthlyReturnView]
    val formProvider                              = new DeleteNilMonthlyReturnFormProvider()
    val form: Form[Boolean]                       = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()

    private val messagesApi = app.injector.instanceOf[play.api.i18n.MessagesApi]

    val messagesEn: Messages = play.api.i18n.MessagesImpl(Lang("en"), messagesApi)
    val messagesCy: Messages = play.api.i18n.MessagesImpl(Lang("cy"), messagesApi)

    private val confirmPaymentsDate: LocalDate = LocalDate.of(2026, 3, 1)

    val monthYearEn: String =
      confirmPaymentsDate.format(DateTimeFormatter.ofPattern("MMMM uuuu").withLocale(Locale.UK))

    val monthYearCy: String =
      confirmPaymentsDate.format(DateTimeFormatter.ofPattern("MMMM uuuu").withLocale(Locale.forLanguageTag("cy")))

    val htmlEn: HtmlFormat.Appendable = view(form, monthYearEn, NormalMode)(request, messagesEn)
    val htmlCy: HtmlFormat.Appendable = view(form, monthYearCy, NormalMode)(request, messagesCy)
  }
}
