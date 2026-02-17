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

package views.monthlyreturns

import base.SpecBase
import forms.PaymentDetailsFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.PaymentDetailsView

class PaymentDetailsViewSpec extends SpecBase with Matchers {

  "PaymentDetailsView" - {

    "must render the page with the correct html elements" in new Setup {
      val companyName = "Test Company Ltd"
      val html        = view(form, NormalMode, companyName, 1)
      val doc         = Jsoup.parse(html.body)

      doc.title             must include(messages("paymentDetails.title"))
      doc.select("h1").text must include(messages("paymentDetails.heading", companyName))

      doc.select("input[type=text]").size() mustBe 1
      doc.select(".govuk-input__prefix").text mustBe "£"

      doc.select(".govuk-hint").text must include(messages("paymentDetails.hint"))

      doc.select("button[type=submit]").text mustBe messages("site.continue")
    }

    "must show error summary and messages when form has errors" in new Setup {
      val companyName    = "Test Company Ltd"
      val boundWithError = form.bind(Map("value" -> ""))
      val html           = view(boundWithError, NormalMode, companyName, 1)
      val doc            = Jsoup.parse(html.body)

      doc.title must startWith(messages("error.title.prefix"))

      doc.select(".govuk-error-summary").size() mustBe 1

      val expected = messages("paymentDetails.error.required")
      doc.text() must include(expected)
    }

    "must render with different company names" in new Setup {
      val companyName1 = "Company A"
      val html1        = view(form, NormalMode, companyName1, 1)
      val doc1         = Jsoup.parse(html1.body)

      doc1.select("h1").text must include(companyName1)

      val companyName2 = "Company B"
      val html2        = view(form, NormalMode, companyName2, 1)
      val doc2         = Jsoup.parse(html2.body)

      doc2.select("h1").text must include(companyName2)
    }

    "must have the correct form action" in new Setup {
      val companyName = "Test Company Ltd"
      val html        = view(form, NormalMode, companyName, 1)
      val doc         = Jsoup.parse(html.body)

      val formElement = doc.select("form").first()
      formElement.attr("action") must include("/payment-details")
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[PaymentDetailsView]
    val formProvider                              = app.injector.instanceOf[PaymentDetailsFormProvider]
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
