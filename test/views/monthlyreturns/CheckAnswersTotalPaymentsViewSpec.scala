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
import controllers.monthlyreturns.CheckAnswersTotalPaymentsViewModel
import models.monthlyreturns.SelectedSubcontractor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import play.api.Application
import play.api.i18n.Messages
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.monthlyreturns.CheckAnswersTotalPaymentsView

class CheckAnswersTotalPaymentsViewSpec extends SpecBase {

  "CheckAnswersTotalPaymentsView" - {

    "must render the correct content on the page" in new Setup {
      val index                       = 1
      val html: HtmlFormat.Appendable = view(viewModel, index)
      val doc: Document               = Jsoup.parse(html.body)

      doc.title             must include(messages("monthlyreturns.checkAnswersTotalPayments.title", viewModel.name))
      doc.select("h1").text must include(
        messages("monthlyreturns.checkAnswersTotalPayments.heading", viewModel.name)
      )

      doc.select("dt").text must include(
        messages("monthlyreturns.checkAnswersTotalPayments.details.totalPaymentsMadeToSubcontractors")
      )
      doc.select("dd").text must include(messages(viewModel.totalPaymentsMade))

      doc.select("dt").text must include(
        messages("monthlyreturns.checkAnswersTotalPayments.details.totalCostOfMaterials")
      )
      doc.select("dd").text must include(messages(viewModel.costOfMaterials))

      doc.select("dt").text must include(
        messages("monthlyreturns.checkAnswersTotalPayments.details.totalCisDeductions")
      )
      doc.select("dd").text must include(messages(viewModel.totalTaxDeducted))

      val changeLinks: Elements = doc.select(".govuk-summary-list__actions a.govuk-link")
      changeLinks.size() mustBe 3

      val changeLinksWIthHiddenText = Seq(
        "Total payments to subcontractor",
        "Total cost of materials",
        "Total CIS deductions"
      )

      changeLinksWIthHiddenText.zipWithIndex.foreach { case (text, index) =>
        changeLinks.get(index).text() must include("Change")
        changeLinks.get(index).text() must include(text)
      }

      doc.getElementsByClass("govuk-button").text() must include(messages("site.continue"))
    }

  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: CheckAnswersTotalPaymentsView       = app.injector.instanceOf[CheckAnswersTotalPaymentsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    val subcontractorName             = "TyneWear Ltd"
    val totalPaymentsToSubcontractors = 1200
    val totalCostOfMaterials          = 500
    val totalCisDeductions            = 240

    val subcontractor = SelectedSubcontractor(
      id = 1,
      name = subcontractorName,
      totalPaymentsMade = Some(totalPaymentsToSubcontractors),
      costOfMaterials = Some(totalCostOfMaterials),
      totalTaxDeducted = Some(totalCisDeductions)
    )

    val viewModel: CheckAnswersTotalPaymentsViewModel = CheckAnswersTotalPaymentsViewModel.fromModel(subcontractor)
  }
}
