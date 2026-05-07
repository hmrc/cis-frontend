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

package views.amend

import base.SpecBase
import forms.amend.ConfirmCancelAmendmentYesNoFormProvider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.amend.ConfirmCancelAmendmentYesNoView

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ConfirmCancelAmendmentYesNoViewSpec extends SpecBase {

  "ConfirmCancelAmendmentYesNoView" - {

    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title must include(messages("amend.confirmCancelAmendmentYesNo.title", monthYear))
      doc.title must include(monthYear)

      doc.select("h1").text must include(messages("amend.confirmCancelAmendmentYesNo.heading", monthYear))
      doc.select("h1").text must include(monthYear)

      doc.getElementsByClass("govuk-button").text must include(messages("site.continue"))
    }

    "must render radio buttons with correct values" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[value=true]").size() mustBe 1
      doc.select("input[value=false]").size() mustBe 1

      doc.select("label").text() must include(messages("site.yes"))
      doc.select("label").text() must include(messages("site.no"))
    }

    "must pre-populate the form when user has previously answered 'true'" in new Setup {
      val filledForm: Form[Boolean]         = form.fill(true)
      val filledHtml: HtmlFormat.Appendable = view(filledForm, monthYear)(request, messages)
      val doc: Document                     = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=true]").hasAttr("checked") mustBe true
      doc.select("input[value=false]").hasAttr("checked") mustBe false
    }

    "must pre-populate the form when user has previously answered 'false'" in new Setup {
      val filledForm: Form[Boolean]         = form.fill(false)
      val filledHtml: HtmlFormat.Appendable = view(filledForm, monthYear)(request, messages)
      val doc: Document                     = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=true]").hasAttr("checked") mustBe false
      doc.select("input[value=false]").hasAttr("checked") mustBe true
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: ConfirmCancelAmendmentYesNoView     = app.injector.instanceOf[ConfirmCancelAmendmentYesNoView]
    val formProvider                              = new ConfirmCancelAmendmentYesNoFormProvider()
    val form: Form[Boolean]                       = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()

    private val messagesApi = app.injector.instanceOf[play.api.i18n.MessagesApi]

    val messages: Messages = play.api.i18n.MessagesImpl(Lang("en"), messagesApi)

    private val monthYearDate: LocalDate = LocalDate.of(2026, 4, 1)

    val monthYear: String =
      monthYearDate.format(DateTimeFormatter.ofPattern("MMMM uuuu").withLocale(Locale.UK))

    val html: HtmlFormat.Appendable = view(form, monthYear)(request, messages)
  }
}
