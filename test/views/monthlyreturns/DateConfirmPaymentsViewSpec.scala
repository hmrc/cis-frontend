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
import forms.monthlyreturns.DateConfirmPaymentsFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.monthlyreturns.DateConfirmPaymentsView

class DateConfirmPaymentsViewSpec extends SpecBase {

  "DateConfirmPaymentsView" - {
    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)
      doc.title                                    must include(messages("dateConfirmPayments.title"))
      doc.select("h1").text                        must include(messages("dateConfirmPayments.heading"))
      doc.select("p").text                         must include(messages("dateConfirmPayments.p1"))
      doc.select(".govuk-warning-text__text").text must include(messages("dateConfirmPayments.warningText"))
      doc.select("h2").text                        must include(messages("dateConfirmPayments.legendText"))

      doc.select("label").text must include(messages("dateConfirmPayments.labelMonth"))
      doc.select("label").text must include(messages("dateConfirmPayments.labelYear"))

      doc.getElementsByClass("govuk-button").text must include(messages("site.continue"))
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view                                      = app.injector.instanceOf[DateConfirmPaymentsView]
    val formProvider                              = new DateConfirmPaymentsFormProvider()
    val form                                      = formProvider()
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val html = view(form, NormalMode)
  }
}
