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
import forms.monthlyreturns.TotalTaxDeductedFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.TotalTaxDeductedView

class TotalTaxDeductedViewSpec extends SpecBase {

  "TotalTaxDeductedView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title                               must include(messages("totalTaxDeducted.title"))
      doc.select("label.govuk-label--l").text must include(messages("totalTaxDeducted.heading"))
      doc.select(".govuk-hint").text          must include(messages("totalTaxDeducted.hintText"))

      doc.select("input[name=value]").attr("type") mustBe "text"
      doc.select("input[name=value]").hasClass("govuk-input--width-10") mustBe true

      doc.text must include("£")

      doc.getElementsByClass("govuk-button").text must include(messages("site.saveAndContinue"))
    }

    "must display error summary when form has errors" in new Setup {
      val formWithErrors = form.bind(Map("value" -> ""))
      val htmlWithErrors = view(formWithErrors, NormalMode)
      val doc: Document  = Jsoup.parse(htmlWithErrors.toString)

      doc.select(".govuk-error-summary").size mustBe 1
      doc.select(".govuk-error-summary__title").text must include("There is a problem")
    }

    "must display field error when value is invalid" in new Setup {
      val formWithErrors = form.bind(Map("value" -> "invalid"))
      val htmlWithErrors = view(formWithErrors, NormalMode)
      val doc: Document  = Jsoup.parse(htmlWithErrors.toString)

      doc.select(".govuk-error-message").size mustBe 1
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[TotalTaxDeductedView]
    val formProvider                              = new TotalTaxDeductedFormProvider()
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view(form, NormalMode)
  }
}
