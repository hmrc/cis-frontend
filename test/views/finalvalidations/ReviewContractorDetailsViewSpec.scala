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

package views.finalvalidations

import base.SpecBase
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.finalvalidations.ReviewContractorDetailsView

class ReviewContractorDetailsViewSpec extends SpecBase {

  "ReviewContractorDetailsView" - {

    "must render the correct page title, heading and paragraph" in new Setup {
      doc.title                       must include(messages("finalvalidations.reviewContractorDetails.title"))
      doc.select("h1").text mustEqual messages("finalvalidations.reviewContractorDetails.title")
      doc.select("p.govuk-body").text must include(messages("finalvalidations.reviewContractorDetails.p"))
    }

    "must render the contractor list item details" in new Setup {
      val taskListLinks = doc.select(".govuk-task-list__link").eachText()
      taskListLinks must contain(messages("finalvalidations.reviewContractorDetails.tasklist.schemeName"))
      taskListLinks must contain(messages("finalvalidations.reviewContractorDetails.tasklist.utr"))
      taskListLinks must contain(messages("finalvalidations.reviewContractorDetails.tasklist.emailAddress"))
    }

    "must render each Incomplete tag for contractor details" in new Setup {
      val tags = doc.select(".govuk-task-list__status .govuk-tag").eachText()

      tags.size mustEqual 3
      tags.forEach(_ mustEqual messages("finalValidations.reviewContractorDetails.taskList.status.incomplete"))
    }

    "must render Cannot start yet status for File a return" in new Setup {
      val taskListItems = doc.select(".govuk-task-list__item")
      val lastItem      = taskListItems.last()
      val lastStatus    = taskListItems.last().select(".govuk-task-list__status")

      lastItem.select(".govuk-task-list__name-and-hint").text mustEqual messages(
        "finalValidations.reviewContractorDetails.taskList.fileAReturn"
      )
      lastItem.select("a").isEmpty mustBe true
      lastStatus.text mustEqual messages(
        "finalValidations.reviewContractorDetails.taskList.status.cannotStartYet"
      )
      lastStatus.hasClass("govuk-task-list__status--cannot-start-yet") mustBe true

    }
    "must render the return to cis dashboard link" in new Setup {
      val backLink = doc.select("p.govuk-body > a.govuk-link").last()
      backLink.text mustEqual messages("finalValidations.reviewSubcontractorDetails.cisReturnDashboardLink")
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: ReviewContractorDetailsView         = app.injector.instanceOf[ReviewContractorDetailsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = MessagesImpl(Lang.defaultLang, app.injector.instanceOf[MessagesApi])

    def html: HtmlFormat.Appendable = view()
    def doc: Document               = Jsoup.parse(html.body)
  }
}
