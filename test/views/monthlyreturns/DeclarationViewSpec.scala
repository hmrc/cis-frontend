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
import forms.monthlyreturns.DeclarationFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.scalatest.matchers.must.Matchers
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.DeclarationView

class DeclarationViewSpec extends SpecBase with Matchers {

  "DeclarationView" - {

    "must render the page with heading, paragraph, checkbox and button" in new Setup {
      val html = view(form, NormalMode, "5 April 2024")
      val doc  = Jsoup.parse(html.body)

      doc.title must include(messages("declaration.title"))
      doc.select("legend").text must include(messages("declaration.heading"))

      doc.select("p").text must include(messages("declaration.paragraph", "5 April 2024"))

      doc.select("input[type=checkbox]").size() mustBe 1

      doc.select("button[type=submit]").text mustBe messages("declaration.submit")
    }

    "must show error summary and messages when form has errors" in new Setup {
      val boundWithError = form.bind(Map("value" -> ""))
      val html           = view(boundWithError, NormalMode, "5 April 2024")
      val doc            = Jsoup.parse(html.body)

      doc.title must startWith(messages("error.title.prefix"))

      doc.select(".govuk-error-summary").size() mustBe 1

      val expected = messages("declaration.error.required")
      doc.text() must include(expected)
    }

    "must render with empty date when no date is provided" in new Setup {
      val html = view(form, NormalMode, "")
      val doc  = Jsoup.parse(html.body)

      doc.select("p").text must include(messages("declaration.paragraph", ""))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[DeclarationView]
    val formProvider                              = app.injector.instanceOf[DeclarationFormProvider]
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
