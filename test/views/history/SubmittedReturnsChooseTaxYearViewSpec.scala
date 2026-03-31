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

package views.history

import base.SpecBase
import forms.history.SubmittedReturnsChooseTaxYearFormProvider
import models.NormalMode
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.freespec.AnyFreeSpec
import play.api.Application
import play.api.data.Form
import play.api.i18n.{Messages, MessagesApi, MessagesImpl}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.history.SubmittedReturnsChooseTaxYearView

class SubmittedReturnsChooseTaxYearViewSpec extends SpecBase {

  "SubmittedReturnsChooseTaxYearView" - {

    "must render the page with the correct html elements" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      doc.title                           must include(messages("history.submittedReturnsChooseTaxYear.title"))
      doc.select("h1").text               must include(messages("history.submittedReturnsChooseTaxYear.heading"))
      doc.select(".govuk-caption-l").text must include(messages("history.submittedReturnsChooseTaxYear.caption"))

      doc.getElementsByClass("govuk-button").text must include(messages("site.continue"))
    }

    "must render radio buttons with correct values" in new Setup {
      val doc: Document = Jsoup.parse(html.toString)

      taxYears.zipWithIndex.foreach { case (year, index) =>
        doc.select(s"input[type=radio][value='$year']").size() mustBe 1
        doc.select(s"label[for=value_$index]").text must include(year)
      }

      doc.select("div.govuk-radios__divider").text mustBe messages("site.or")
      doc.select("input[type=radio][value=all]").size() mustBe 1
      doc.select("label[for=value_all]").text must include(messages("history.submittedReturnsChooseTaxYear.viewAll"))
    }
  }

  trait Setup {
    val app: Application                        = applicationBuilder().build()
    val view: SubmittedReturnsChooseTaxYearView = app.injector.instanceOf[SubmittedReturnsChooseTaxYearView]

    val taxYears: Seq[String] =
      Seq("2021 to 2202", "2022 to 2023", "2023 to 2024", "2024 to 2025")

    val formProvider       = new SubmittedReturnsChooseTaxYearFormProvider()
    val form: Form[String] = formProvider()

    implicit val request: Request[_] = FakeRequest()
    implicit val messages: Messages  = MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[MessagesApi]
    )

    val html: HtmlFormat.Appendable = view(form, NormalMode, taxYears)
  }
}
