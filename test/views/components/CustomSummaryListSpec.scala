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

package views.components

import base.SpecBase
import org.jsoup.Jsoup
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import views.html.components.CustomSummaryList

class CustomSummaryListSpec extends SpecBase {

  "CustomSummaryList" - {

    "must render the correct list items when provided" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc  = Jsoup.parse(html.body)

      val renderedRows = doc.select(".govuk-summary-list__row")
      renderedRows.size mustBe 3

      renderedRows.get(0).select(".govuk-summary-list__key").text mustBe "BuildRight Construction"
      renderedRows.get(1).select(".govuk-summary-list__key").text mustBe "Northern Trades Ltd"
      renderedRows.get(2).select(".govuk-summary-list__key").text mustBe "TyneWear Ltd"
    }

    "must render the correct links per row when provided" in new Setup {
      val html = customSummaryListRows(rows = rowsWithLinks)
      val doc  = Jsoup.parse(html.body)

      val actionItems = doc.select(".govuk-summary-list__actions-list-item")
      actionItems.size mustBe 2

      val first = actionItems.get(0).select("a.govuk-link")
      first.attr("href") mustBe "http://example.com/change"
      first.select("span.link-text").text mustBe messages("site.change")
      first.select("span.govuk-visually-hidden").text.trim mustBe messages("subcontractorDetailsAdded.change.hidden", "TyneWear Ltd")

      val second = actionItems.get(1).select("a.govuk-link")
      second.attr("href") mustBe "http://example.com/remove"
      second.select("span.link-text").text mustBe messages("site.remove")
      second.select("span.govuk-visually-hidden").text.trim mustBe messages("subcontractorDetailsAdded.remove.hidden", "TyneWear Ltd")
    }

    "must render with the correct CSS classes" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc  = Jsoup.parse(html.body)

      val list = doc.select("dl.govuk-summary-list")
      list.attr("class").trim mustBe "govuk-summary-list hmrc-list-with-actions hmrc-list-with-actions--short"
    }

    "must render the correct number of subcontractor rows in the list" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list__row").size mustBe 3
    }

    "must handle empty string items in the list" in new Setup {
      val html = customSummaryListRows(rows = Seq("" -> Seq.empty))
      val doc  = Jsoup.parse(html.body)

      val renderedRows = doc.select(".govuk-summary-list__row")
      renderedRows.size mustBe 1
      renderedRows.get(0).select(".govuk-summary-list__key").text mustBe ""
    }

    "must not render actions list markup when a row has no links" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list__actions-list").size mustBe 0
      doc.select(".govuk-summary-list__actions-list-item").size mustBe 0
    }
  }

  trait Setup {
    val app: Application = applicationBuilder().build()

    val customSummaryListRows: CustomSummaryList =
      app.injector.instanceOf[CustomSummaryList]

    implicit val request: play.api.mvc.Request[_] = FakeRequest()

    implicit val messages: Messages = MessagesImpl(
      Lang.defaultLang,
      app.injector.instanceOf[MessagesApi]
    )

    // Rows with no links (per row)
    val selectedRowsNoLinks: Seq[(String, Seq[(String, String, String)])] =
      Seq(
        "BuildRight Construction"     -> Seq.empty,
        "Northern Trades Ltd"         -> Seq.empty,
        "TyneWear Ltd"                -> Seq.empty
      )

    // Only TyneWear Ltd row has two links
    val rowsWithLinks: Seq[(String, Seq[(String, String, String)])] =
      Seq(
        "TyneWear Ltd" -> Seq(
          ("http://example.com/change", "site.change", "subcontractorDetailsAdded.change.hidden"),
          ("http://example.com/remove", "site.remove", "subcontractorDetailsAdded.remove.hidden")
        ),
        "Northern Trades Ltd" -> Seq.empty,
        "BuildRight Construction" -> Seq.empty
      )
  }
}
