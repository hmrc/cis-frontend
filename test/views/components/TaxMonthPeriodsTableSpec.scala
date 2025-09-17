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

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import views.html.components.TaxMonthPeriodsTable
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup
import scala.jdk.CollectionConverters._

class TaxMonthPeriodsTableSpec extends SpecBase with Matchers {

  "TaxMonthPeriodsTable" - {
    "must render a table with correct caption" in new Setup {
      val html    = taxMonthPeriodsTable()
      val doc     = Jsoup.parse(html.body)
      val caption = doc.select("caption")

      caption.size mustBe 1
      caption.text mustBe "Tax month periods and what to enter"
      caption.hasClass("govuk-table__caption--m") mustBe true
    }

    "must render table headers correctly" in new Setup {
      val html    = taxMonthPeriodsTable()
      val doc     = Jsoup.parse(html.body)
      val headers = doc.select("thead th")

      headers.size mustBe 3
      headers.get(0).text mustBe "Time period"
      headers.get(1).text mustBe "Month ending"
      headers.get(2).text mustBe "Enter month and year"
    }

    "must render exactly 12 table rows" in new Setup {
      val html = taxMonthPeriodsTable()
      val doc  = Jsoup.parse(html.body)
      val rows = doc.select("tbody tr")

      rows.size mustBe 12
    }

    "must render correct data for first row" in new Setup {
      val html     = taxMonthPeriodsTable()
      val doc      = Jsoup.parse(html.body)
      val firstRow = doc.select("tbody tr").first()
      val cells    = firstRow.select("td")

      cells.size mustBe 3
      cells.get(0).text mustBe "6th April - 5th May"
      cells.get(1).text mustBe "May"
      cells.get(2).text mustBe "05 2025"
    }

    "must render correct data for last row" in new Setup {
      val html    = taxMonthPeriodsTable()
      val doc     = Jsoup.parse(html.body)
      val lastRow = doc.select("tbody tr").last()
      val cells   = lastRow.select("td")

      cells.size mustBe 3
      cells.get(0).text mustBe "6th March - 5th April"
      cells.get(1).text mustBe "April"
      cells.get(2).text mustBe "04 2025"
    }

    "must render correct data for middle row (6th row)" in new Setup {
      val html     = taxMonthPeriodsTable()
      val doc      = Jsoup.parse(html.body)
      val sixthRow = doc.select("tbody tr").get(5)
      val cells    = sixthRow.select("td")

      cells.size mustBe 3
      cells.get(0).text mustBe "6th September - 5th October"
      cells.get(1).text mustBe "October"
      cells.get(2).text mustBe "10 2025"
    }

    "must have correct table structure with govuk classes" in new Setup {
      val html  = taxMonthPeriodsTable()
      val doc   = Jsoup.parse(html.body)
      val table = doc.select("table")

      table.size mustBe 1
      table.hasClass("govuk-table") mustBe true
    }

    "must render all months in correct order" in new Setup {
      val html = taxMonthPeriodsTable()
      val doc  = Jsoup.parse(html.body)
      val rows = doc.select("tbody tr").asScala.toSeq

      val expectedMonths = Seq(
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December",
        "January",
        "February",
        "March",
        "April"
      )

      rows.zipWithIndex.foreach { case (row, index) =>
        val monthCell = row.select("td").get(1)
        monthCell.text mustBe expectedMonths(index)
      }
    }

    "must render all input values in correct order" in new Setup {
      val html = taxMonthPeriodsTable()
      val doc  = Jsoup.parse(html.body)
      val rows = doc.select("tbody tr").asScala.toSeq

      val expectedInputs = Seq(
        "05 2025",
        "06 2025",
        "07 2025",
        "08 2025",
        "09 2025",
        "10 2025",
        "11 2025",
        "12 2025",
        "01 2025",
        "02 2025",
        "03 2025",
        "04 2025"
      )

      rows.zipWithIndex.foreach { case (row, index) =>
        val inputCell = row.select("td").get(2)
        inputCell.text mustBe expectedInputs(index)
      }
    }

    "must render all period ranges in correct order" in new Setup {
      val html = taxMonthPeriodsTable()
      val doc  = Jsoup.parse(html.body)
      val rows = doc.select("tbody tr").asScala.toSeq

      val expectedPeriods = Seq(
        "6th April - 5th May",
        "6th May - 5th June",
        "6th June - 5th July",
        "6th July - 5th August",
        "6th August - 5th September",
        "6th September - 5th October",
        "6th October - 5th November",
        "6th November - 5th December",
        "6th December - 5th January",
        "6th January - 5th February",
        "6th February - 5th March",
        "6th March - 5th April"
      )

      rows.zipWithIndex.foreach { case (row, index) =>
        val periodCell = row.select("td").get(0)
        periodCell.text mustBe expectedPeriods(index)
      }
    }

    "must handle missing message keys gracefully" in new Setup {
      val html = taxMonthPeriodsTable()
      val doc  = Jsoup.parse(html.body)

      doc.select("table").size mustBe 1
      doc.select("tbody tr").size mustBe 12
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val taxMonthPeriodsTable                      = app.injector.instanceOf[TaxMonthPeriodsTable]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )
  }
}
