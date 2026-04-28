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
import forms.amend.AreYouSureYouWantToAmendFormProvider
import models.NormalMode
import models.amend.AreYouSureYouWantToAmend
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.amend.AreYouSureYouWantToAmendView

class AreYouSureYouWantToAmendViewSpec extends SpecBase {

  "AreYouSureYouWantToAmendView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.title                                   must include(messages("amend.areYouSureYouWantToAmend.title"))
      doc.select("h1").text                       must include(messages("amend.areYouSureYouWantToAmend.heading"))
      doc.getElementsByClass("govuk-button").text must include(messages("site.continue"))
    }

    "must render radio buttons with correct values" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[type=radio][value=yes]").size() mustBe 1
      doc.select("input[type=radio][value=no]").size() mustBe 1

      doc.select("label[for=value_0]").text() must include(messages("amend.areYouSureYouWantToAmend.yes"))
      doc.select("label[for=value_1]").text() must include(messages("amend.areYouSureYouWantToAmend.no"))
    }

    "must pre-populate the form when user has previously answered 'Yes'" in new Setup {
      val filledForm    = form.fill(AreYouSureYouWantToAmend.Yes)
      val filledHtml    = view(filledForm, NormalMode)
      val doc: Document = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=yes]").hasAttr("checked") mustBe true
      doc.select("input[value=no]").hasAttr("checked") mustBe false
    }

    "must pre-populate the form when user has previously answered 'No'" in new Setup {
      val filledForm    = form.fill(AreYouSureYouWantToAmend.No)
      val filledHtml    = view(filledForm, NormalMode)
      val doc: Document = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=yes]").hasAttr("checked") mustBe false
      doc.select("input[value=no]").hasAttr("checked") mustBe true
    }

    "must display error summary when form has errors" in new Setup {
      val errorForm     = form.bind(Map("value" -> ""))
      val errorHtml     = view(errorForm, NormalMode)
      val doc: Document = Jsoup.parse(errorHtml.toString)

      doc.select(".govuk-error-summary").size() mustBe 1
      doc.select(".govuk-error-summary__title").text() must include("There is a problem")
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[AreYouSureYouWantToAmendView]
    val formProvider                              = new AreYouSureYouWantToAmendFormProvider()
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view(form, NormalMode)
  }
}
