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
import forms.WhatDoYouWantToAmendStandardFormProvider
import models.{NormalMode, WhatDoYouWantToAmendStandard}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.amend.WhatDoYouWantToAmendStandardView

class WhatDoYouWantToAmendStandardViewSpec extends SpecBase {

  "WhatDoYouWantToAmendStandardView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.title             must include(messages("amend.whatDoYouWantToAmendStandard.title"))
      doc.select("h1").text must include(messages("amend.whatDoYouWantToAmendStandard.heading"))
    }

    "must render radio buttons with correct values" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.select("input[type=radio][value=amendToNilReturn]").size() mustBe 1
      doc.select("input[type=radio][value=amendPaymentOrSubcontractorDetails]").size() mustBe 1
    }

    "must render radio buttons with correct labels" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.text() must include(messages("amend.whatDoYouWantToAmendStandard.amendToNilReturn"))
      doc.text() must include(messages("amend.whatDoYouWantToAmendStandard.amendPaymentOrSubcontractorDetails"))
    }

    "must pre-populate the form when user has previously answered" in new Setup {
      val filledForm    = form.fill(WhatDoYouWantToAmendStandard.AmendToNilReturn)
      val filledHtml    = view(filledForm, NormalMode)
      val doc: Document = Jsoup.parse(filledHtml.toString)

      doc.select("input[value=amendToNilReturn]").hasAttr("checked") mustBe true
      doc.select("input[value=amendPaymentOrSubcontractorDetails]").hasAttr("checked") mustBe false
    }

    "must show error summary when form has errors" in new Setup {
      val formWithErrors = form.bind(Map("value" -> ""))
      val errorHtml      = view(formWithErrors, NormalMode)
      val doc: Document  = Jsoup.parse(errorHtml.toString)

      doc.title must startWith(messages("error.title.prefix"))
      doc.select(".govuk-error-summary").size() mustBe 1
      doc.text() must include(messages("amend.whatDoYouWantToAmendStandard.error.required"))
    }

    "must render error summary with correct link when form has errors" in new Setup {
      val formWithErrors = form.bind(Map("value" -> ""))
      val errorHtml      = view(formWithErrors, NormalMode)
      val doc: Document  = Jsoup.parse(errorHtml.toString)

      doc.select(".govuk-error-summary__list a").attr("href") mustBe "#value_0"
    }

    "must render the continue button" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.select("button[type=submit]").text mustBe messages("site.continue")
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[WhatDoYouWantToAmendStandardView]
    val formProvider                              = new WhatDoYouWantToAmendStandardFormProvider()
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view(form, NormalMode)
  }
}
