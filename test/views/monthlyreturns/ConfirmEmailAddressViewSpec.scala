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

import base.SpecBase
import forms.monthlyreturns.ConfirmEmailAddressFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.ConfirmEmailAddressView

class ConfirmEmailAddressViewSpec extends SpecBase with Matchers {

  "ConfirmEmailAddressView" - {

    "must render the page with heading, paragraph, input and button" in new Setup {
      val html = view(form, NormalMode)
      val doc = Jsoup.parse(html.body)

      doc.title must include(messages("confirmEmailAddress.title"))
      doc.select("h1").text mustBe messages("confirmEmailAddress.heading")

      doc.select("p").text must include(messages("confirmEmailAddress.paragraph"))

      doc.select("label").text must include(messages("confirmEmailAddress.input.label"))

      doc.select("button[type=submit]").text mustBe messages("site.continue")
    }

    "must show error summary and messages when form has errors" in new Setup {
      val boundWithError = form.bind(Map("value" -> ""))
      val html = view(boundWithError, NormalMode)
      val doc = Jsoup.parse(html.body)

      doc.title must startWith(messages("error.title.prefix"))

      doc.select(".govuk-error-summary").size() mustBe 1

      val expected = messages("confirmEmailAddress.error.required")
      doc.text() must include(expected)
    }
  }

  trait Setup {
    val app = applicationBuilder().build()
    val view = app.injector.instanceOf[ConfirmEmailAddressView]
    val formProvider = app.injector.instanceOf[ConfirmEmailAddressFormProvider]
    val form = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}


