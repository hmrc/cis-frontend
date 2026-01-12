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
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import views.html.components.CustomSummaryList

class CustomSummaryListSpec extends SpecBase {

  "CustomSummaryList" - {
    "must render the correct list items when provided" in new Setup {
      val html = customSummaryList(selectedSubcontractors, Seq.empty)
      val doc  = Jsoup.parse(html.body)

      val rows = doc.select(".govuk-summary-list__row")
      rows.size mustBe selectedSubcontractors.size
      rows.get(0).select(".govuk-summary-list__key").text mustBe "BuildRight Construction"
      rows.get(1).select(".govuk-summary-list__key").text mustBe "Northern Trades Ltd"
      rows.get(2).select(".govuk-summary-list__key").text mustBe "TyneWear Ltd"
    }

    "must render the correct links when provided" in new Setup {
      val links          = Seq(("http://example.com", "More Info"))
      val subcontractors = Seq("Mike Jordan")
      val html           = customSummaryList(subcontractors, links)
      val doc            = Jsoup.parse(html.body)

      val actions = doc.select(".govuk-summary-list__actions-list-item")
      actions.size mustBe 1
      actions.get(0).select("a.govuk-link").attr("href") mustBe "http://example.com"
      actions.get(0).select("span.govuk-visually-hidden").text mustBe "Link to More Info"
    }

    "must render no links when an empty list of links is provided" in new Setup {
      val html = customSummaryList(selectedSubcontractors, Seq.empty)
      val doc  = Jsoup.parse(html.body)

      val actions = doc.select(".govuk-summary-list__actions-list-item")
      actions.size mustBe 0
    }

    "must render with the correct CSS classes" in new Setup {
      val html = customSummaryList(selectedSubcontractors, Seq.empty)
      val doc  = Jsoup.parse(html.body)

      val list = doc.select(".govuk-summary-list")
      list.attr("class").trim mustBe "govuk-summary-list hmrc-list-with-actions hmrc-list-with-actions--short"
    }

    "must render the correct number of subcontractor rows in the list" in new Setup {
      val html = customSummaryList(selectedSubcontractors, Seq.empty)
      val doc  = Jsoup.parse(html.body)

      val rows = doc.select(".govuk-summary-list__row")
      rows.size mustBe selectedSubcontractors.size
    }

    "must handle empty string items in the list" in new Setup {
      val emptyItemList = Seq("")
      val html          = customSummaryList(emptyItemList, Seq.empty)
      val doc           = Jsoup.parse(html.body)

      val rows = doc.select(".govuk-summary-list__row")
      rows.size mustBe 1
      rows.get(0).select(".govuk-summary-list__key").text mustBe ""
    }

  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val customSummaryList: CustomSummaryList      = app.injector.instanceOf[CustomSummaryList]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val selectedSubcontractors: Seq[String] = Seq("BuildRight Construction", "Northern Trades Ltd", "TyneWear Ltd")
  }
}
