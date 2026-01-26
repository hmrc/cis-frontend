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
import forms.monthlyreturns.ConfirmSubcontractorRemovalFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.data.Form
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.ConfirmSubcontractorRemovalView

class ConfirmSubcontractorRemovalViewSpec extends SpecBase {

  "ConfirmSubcontractorRemovalView" - {
    "must render the content on the page" in new Setup {
      val html: HtmlFormat.Appendable = view(form, NormalMode, subcontractorName)
      val doc: Document               = Jsoup.parse(html.body)

      doc.title               must include(messages("monthlyreturns.confirmSubcontractorRemoval.title", subcontractorName))
      doc.select("h1").text() must include(
        messages("monthlyreturns.confirmSubcontractorRemoval.heading", subcontractorName)
      )
      doc.select("input[type=radio]").size() mustBe 2
    }
  }

  trait Setup {
    val app: Application                                      = applicationBuilder().build()
    val view: ConfirmSubcontractorRemovalView                 = app.injector.instanceOf[ConfirmSubcontractorRemovalView]
    val formProvider: ConfirmSubcontractorRemovalFormProvider =
      app.injector.instanceOf[ConfirmSubcontractorRemovalFormProvider]
    val form: Form[Boolean]                                   = formProvider()
    implicit val request: play.api.mvc.Request[_]             = FakeRequest()
    implicit val messages: Messages                           = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val subcontractorName = "TyneWear Ltd"
  }
}
