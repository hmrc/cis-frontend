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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import viewmodels.checkAnswers.monthlyreturns.SummarySubcontractorPaymentsViewModel
import views.html.monthlyreturns.SummarySubcontractorPaymentsView

import scala.math.BigDecimal

class SummarySubcontractorPaymentsViewSpec extends SpecBase {

  "SummarySubcontractorPaymentsView" - {

    "must render the page with correct heading" in new Setup {
      val html: HtmlFormat.Appendable =
        view(viewModel(subcontractorCount, totalPayments, totalMaterialsCost, totalCisDeductions))
      val doc: Document               = Jsoup.parse(html.body)

      doc.title             must include(messages("monthlyreturns.summarySubcontractorPayments.title"))
      doc.select("h1").text must include(messages("monthlyreturns.summarySubcontractorPayments.heading"))
    }

    "must not render intro paragraph when count is 1" in new Setup {
      val html: HtmlFormat.Appendable = view(viewModel(1, totalPayments, totalMaterialsCost, totalCisDeductions))
      val doc: Document               = Jsoup.parse(html.body)

      doc.body().text must not include messages("monthlyreturns.summarySubcontractorPayments.intro", 1)
    }

    "must render intro paragraph when count is greater than 1" in new Setup {
      val html: HtmlFormat.Appendable =
        view(viewModel(subcontractorCount, totalPayments, totalMaterialsCost, totalCisDeductions))
      val doc: Document               = Jsoup.parse(html.body)

      doc.select("p").text must include(
        messages("monthlyreturns.summarySubcontractorPayments.intro", subcontractorCount)
      )
    }

    "must render intro paragraph when count is 0" in new Setup {
      val html: HtmlFormat.Appendable = view(viewModel(0, totalPayments, totalMaterialsCost, totalCisDeductions))
      val doc: Document               = Jsoup.parse(html.body)

      doc.select("p").text must include(
        messages("monthlyreturns.summarySubcontractorPayments.intro", 0)
      )
    }

    "must render summary list with correct totals" in new Setup {
      val html: HtmlFormat.Appendable =
        view(viewModel(subcontractorCount, totalPayments, totalMaterialsCost, totalCisDeductions))
      val doc: Document               = Jsoup.parse(html.body)

      val summaryRows = doc.select(".govuk-summary-list__row")
      summaryRows.size mustBe 3

      val keys = summaryRows.select(".govuk-summary-list__key").eachText()
      keys.get(0) mustEqual messages("monthlyreturns.summarySubcontractorPayments.totalPayments.label")
      keys.get(1) mustEqual messages("monthlyreturns.summarySubcontractorPayments.totalMaterialsCost.label")
      keys.get(2) mustEqual messages("monthlyreturns.summarySubcontractorPayments.totalCisDeductions.label")

      val values = summaryRows.select(".govuk-summary-list__value").eachText()
      values.get(0) mustEqual "£3600"
      values.get(1) mustEqual "£900"
      values.get(2) mustEqual "£540"
    }

    "must format currency values correctly" in new Setup {
      val html: HtmlFormat.Appendable = view(
        viewModel(
          subcontractorCount,
          BigDecimal(1234.56),
          BigDecimal(789.01),
          BigDecimal(234.56)
        )
      )
      val doc: Document               = Jsoup.parse(html.body)

      val values = doc.select(".govuk-summary-list__value").eachText()
      values.get(0) mustEqual "£1,234.56"
      values.get(1) mustEqual "£789.01"
      values.get(2) mustEqual "£234.56"
    }

    "must handle zero values correctly" in new Setup {
      val html: HtmlFormat.Appendable = view(viewModel(0, BigDecimal(0), BigDecimal(0), BigDecimal(0)))
      val doc: Document               = Jsoup.parse(html.body)

      val values = doc.select(".govuk-summary-list__value").eachText()
      values.get(0) mustEqual "£0"
      values.get(1) mustEqual "£0"
      values.get(2) mustEqual "£0"
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: SummarySubcontractorPaymentsView    = app.injector.instanceOf[SummarySubcontractorPaymentsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val subcontractorCount = 4
    val totalPayments      = BigDecimal(3600)
    val totalMaterialsCost = BigDecimal(900)
    val totalCisDeductions = BigDecimal(540)

    def viewModel(count: Int, payments: BigDecimal, materials: BigDecimal, deductions: BigDecimal) =
      SummarySubcontractorPaymentsViewModel(count, payments, materials, deductions)
  }
}
