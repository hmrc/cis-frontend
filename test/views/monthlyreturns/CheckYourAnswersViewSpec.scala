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
import play.api.i18n.Messages
import play.api.test.FakeRequest
import viewmodels.govuk.SummaryListFluency
import viewmodels.govuk.summarylist.*
import viewmodels.implicits.*
import views.html.monthlyreturns.CheckYourAnswersView

class CheckYourAnswersViewSpec extends SpecBase with SummaryListFluency {

  "CheckYourAnswersView" - {

    "must render the page title, headings and submit button" in new Setup {
      doc.title                 must include(messages("monthlyreturns.checkYourAnswers.title"))
      doc.select("h1").text     must include(messages("monthlyreturns.checkYourAnswers.heading"))
      doc.select("h2").text     must include(messages("monthlyreturns.checkYourAnswers.returnDetails.heading"))
      doc.select("h2").text     must include(messages("monthlyreturns.checkYourAnswers.emailConfirmation.heading"))
      doc.select("button").text must include(messages("monthlyreturns.checkYourAnswers.submitSection.button"))
    }

    "must post to the correct form action" in new Setup {
      doc.select("form").attr("action") mustBe controllers.monthlyreturns.routes.CheckYourAnswersController
        .onSubmit()
        .url
    }

    "must render the return details summary list" in new Setup {
      val returnDetailsRow = SummaryListRowViewModel(
        key = "Return period",
        value = ValueViewModel("January 2025"),
        actions = Seq.empty
      )
      val listWithRow      = SummaryListViewModel(Seq(returnDetailsRow))
      val docWithRows      = Jsoup.parse(view(listWithRow, SummaryListViewModel(Seq.empty)).body)

      docWithRows.select(".govuk-summary-list__key").text   must include("Return period")
      docWithRows.select(".govuk-summary-list__value").text must include("January 2025")
    }

    "must render the email confirmation summary list" in new Setup {
      val emailRow    = SummaryListRowViewModel(
        key = "Email confirmation",
        value = ValueViewModel("Yes"),
        actions = Seq.empty
      )
      val listWithRow = SummaryListViewModel(Seq(emailRow))
      val docWithRows = Jsoup.parse(view(SummaryListViewModel(Seq.empty), listWithRow).body)

      docWithRows.select(".govuk-summary-list__key").text   must include("Email confirmation")
      docWithRows.select(".govuk-summary-list__value").text must include("Yes")
    }

    "must render two separate summary lists when both are populated" in new Setup {
      val returnRow   = SummaryListRowViewModel(
        key = "Return period",
        value = ValueViewModel("February 2025"),
        actions = Seq.empty
      )
      val emailRow    = SummaryListRowViewModel(
        key = "Email confirmation",
        value = ValueViewModel("No"),
        actions = Seq.empty
      )
      val docWithRows = Jsoup.parse(
        view(SummaryListViewModel(Seq(returnRow)), SummaryListViewModel(Seq(emailRow))).body
      )

      docWithRows.select(".govuk-summary-list").size() mustBe 2
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val view: CheckYourAnswersView                = app.injector.instanceOf[CheckYourAnswersView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val doc: Document = Jsoup.parse(
      view(SummaryListViewModel(Seq.empty), SummaryListViewModel(Seq.empty)).body
    )
  }
}
