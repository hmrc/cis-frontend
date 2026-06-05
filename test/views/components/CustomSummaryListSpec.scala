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
      first.select("span.govuk-visually-hidden").text.trim mustBe messages(
        "monthlyreturns.subcontractorDetailsAdded.change.hidden",
        "TyneWear Ltd"
      )

      val second = actionItems.get(1).select("a.govuk-link")
      second.attr("href") mustBe "http://example.com/remove"
      second.select("span.link-text").text mustBe messages("site.remove")
      second.select("span.govuk-visually-hidden").text.trim mustBe messages(
        "monthlyreturns.subcontractorDetailsAdded.remove.hidden",
        "TyneWear Ltd"
      )
    }

    "must render with hmrc-list-with-actions classes when no rowValues are provided" in new Setup {
      val html    = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc     = Jsoup.parse(html.body)
      val classes = doc.select("dl.govuk-summary-list").attr("class")

      classes must include("hmrc-list-with-actions")
      classes must include("hmrc-list-with-actions--short")
    }

    "must omit hmrc-list-with-actions classes when rowValues are provided" in new Setup {
      val html    = customSummaryListRows(rows = selectedRowsNoLinks.take(2), rowValues = Seq("1234455", "123"))
      val doc     = Jsoup.parse(html.body)
      val classes = doc.select("dl.govuk-summary-list").attr("class")

      classes mustNot include("hmrc-list-with-actions")
      classes mustNot include("hmrc-list-with-actions--short")
    }

    "must apply govuk-!-font-weight-regular to key elements by default" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list__key").forEach { key =>
        key.classNames must contain("govuk-!-font-weight-regular")
      }
    }

    "must apply custom keyClasses to key elements when provided" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks, keyClasses = "govuk-body govuk-!-font-weight-bold")
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list__key").forEach { key =>
        key.classNames must contain("govuk-body")
        key.classNames must contain("govuk-!-font-weight-bold")
        key.classNames mustNot contain("govuk-!-font-weight-regular")
      }
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

    "must render inline values when rowValues is provided" in new Setup {
      val html         = customSummaryListRows(
        rows = selectedRowsNoLinks.take(2),
        rowValues = Seq("1234455", "123")
      )
      val doc          = Jsoup.parse(html.body)
      val valueColumns = doc.select(".govuk-summary-list__value")

      valueColumns.size mustBe 2
      valueColumns.get(0).text mustBe "1234455"
      valueColumns.get(1).text mustBe "123"
    }

    "must not render value columns when rowValues is omitted" in new Setup {
      val html = customSummaryListRows(rows = selectedRowsNoLinks)
      val doc  = Jsoup.parse(html.body)

      doc.select(".govuk-summary-list__value").size mustBe 0
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

    val selectedRowsNoLinks: Seq[(String, Seq[(String, String, String)])] =
      Seq(
        "BuildRight Construction" -> Seq.empty,
        "Northern Trades Ltd"     -> Seq.empty,
        "TyneWear Ltd"            -> Seq.empty
      )

    val rowsWithLinks: Seq[(String, Seq[(String, String, String)])] =
      Seq(
        "TyneWear Ltd"            -> Seq(
          ("http://example.com/change", "site.change", "monthlyreturns.subcontractorDetailsAdded.change.hidden"),
          ("http://example.com/remove", "site.remove", "monthlyreturns.subcontractorDetailsAdded.remove.hidden")
        ),
        "Northern Trades Ltd"     -> Seq.empty,
        "BuildRight Construction" -> Seq.empty
      )
  }
}
