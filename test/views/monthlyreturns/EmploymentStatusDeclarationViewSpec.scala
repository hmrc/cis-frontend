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
import forms.monthlyreturns.EmploymentStatusDeclarationFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.EmploymentStatusDeclarationView

class EmploymentStatusDeclarationViewSpec extends SpecBase {

  "EmploymentStatusDeclarationView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.title             must include(messages("monthlyreturns.employmentStatusDeclaration.title"))
      doc.select("h1").text must include(messages("monthlyreturns.employmentStatusDeclaration.heading"))

      doc.getElementsByClass("govuk-button").text must include(
        messages("monthlyreturns.employmentStatusDeclaration.confirm")
      )
    }

    "must render radio buttons with correct values" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[type=radio][value=true]").size() mustBe 1
      doc.select("input[type=radio][value=false]").size() mustBe 1

      doc.select("label[for=value_0]").text() must include(messages("site.yes"))
      doc.select("label[for=value_1]").text() must include(messages("site.no"))
    }

    "must pre-populate the form when user has previously answered 'true'" in new Setup {
      val filledForm    = form.fill(true)
      val filledHtml    = view(filledForm, NormalMode)
      val doc: Document = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=true]").hasAttr("checked") mustBe true
      doc.select("input[value=false]").hasAttr("checked") mustBe false
    }

    "must pre-populate the form when user has previously answered 'false'" in new Setup {
      val filledForm    = form.fill(false)
      val filledHtml    = view(filledForm, NormalMode)
      val doc: Document = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=true]").hasAttr("checked") mustBe false
      doc.select("input[value=false]").hasAttr("checked") mustBe true
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[EmploymentStatusDeclarationView]
    val formProvider                              = new EmploymentStatusDeclarationFormProvider()
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view(form, NormalMode)
  }
}
