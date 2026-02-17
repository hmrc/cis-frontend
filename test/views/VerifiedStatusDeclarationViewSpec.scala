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

package views

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.VerifiedStatusDeclarationView
import forms.VerifiedStatusDeclarationFormProvider
import models.NormalMode
import play.api.data.Form

class VerifiedStatusDeclarationViewSpec extends SpecBase {

  "VerifiedStatusDeclarationView" - {
    "must render the actual content on the page" in new Setup {
      val html: HtmlFormat.Appendable = view(form, NormalMode)
      val doc: Document               = Jsoup.parse(html.body)

      doc.title                 must include(messages("verifiedStatusDeclaration.title"))
      doc.select("h1").text     must include(messages("verifiedStatusDeclaration.heading"))
      doc.select("legend").text must include(messages("verifiedStatusDeclaration.legend"))
      doc.select("input[type=radio]").size() mustBe 2
      doc.select("button").text mustBe messages("site.confirm")
    }
  }

  trait Setup {
    val app: Application                                    = applicationBuilder().build()
    val view: VerifiedStatusDeclarationView                 = app.injector.instanceOf[VerifiedStatusDeclarationView]
    val formProvider: VerifiedStatusDeclarationFormProvider =
      app.injector.instanceOf[VerifiedStatusDeclarationFormProvider]
    val form: Form[Boolean]                                 = formProvider()
    implicit val request: play.api.mvc.Request[_]           = FakeRequest()
    implicit val messages: Messages                         = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
