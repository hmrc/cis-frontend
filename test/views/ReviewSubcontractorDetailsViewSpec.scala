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

package views

import base.SpecBase
import controllers.monthlyreturns.routes.SelectSubcontractorsController
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.ReviewSubcontractorDetailsView

class ReviewSubcontractorDetailsViewSpec extends SpecBase {

  "ReviewSubcontractorDetailsView" - {

    "must render the correct page title" in new Setup {
      doc.title must include(messages("reviewSubcontractorDetails.title"))
    }

    "must render the correct heading" in new Setup {
      doc.select("h1").text mustEqual messages("reviewSubcontractorDetails.heading")
    }

    "must render the intro paragraph" in new Setup {
      doc.select("p.govuk-body").text must include(messages("reviewSubcontractorDetails.intro"))
    }

    "must render each subcontractor as a task list item link" in new Setup {
      val taskListLinks = doc.select(".govuk-task-list__link").eachText()

      subcontractors.foreach { name =>
        taskListLinks must contain(name)
      }
    }

    "must render each subcontractor link pointing to a dead link" in new Setup {
      doc.select(".govuk-task-list__link").forEach { link =>
        link.attr("href") mustEqual "#"
      }
    }

    "must render the Incomplete tag for each subcontractor" in new Setup {
      val tags = doc.select(".govuk-task-list__status .govuk-tag").eachText()

      tags.size mustEqual subcontractors.size
      tags.forEach(_ mustEqual messages("reviewSubcontractorDetails.taskList.status.incomplete"))
    }

    "must render File a return as the last task list item without a link" in new Setup {
      val taskListItems = doc.select(".govuk-task-list__item")
      val lastItem      = taskListItems.last()

      lastItem.select(".govuk-task-list__name-and-hint").text mustEqual messages(
        "reviewSubcontractorDetails.taskList.fileAReturn"
      )
      lastItem.select("a").isEmpty mustBe true
    }

    "must render Cannot continue yet status for File a return" in new Setup {
      val taskListItems = doc.select(".govuk-task-list__item")
      val lastStatus    = taskListItems.last().select(".govuk-task-list__status")

      lastStatus.text mustEqual messages("reviewSubcontractorDetails.taskList.status.cannotContinueYet")
      lastStatus.hasClass("govuk-task-list__status--cannot-start-yet") mustBe true
    }

    "must render the back link with the correct URL" in new Setup {
      val backLink = doc.select("p.govuk-body > a.govuk-link").last()

      backLink.text mustEqual messages("reviewSubcontractorDetails.backLink")
      backLink.attr("href") mustEqual SelectSubcontractorsController.onPageLoad(None).url
    }

    "must render only File a return when subcontractors list is empty" in new Setup {
      override val subcontractors: Seq[String] = Seq.empty
      val taskListItems                        = doc.select(".govuk-task-list__item")

      taskListItems.size mustEqual 1
      taskListItems.first().select(".govuk-task-list__name-and-hint").text mustEqual messages(
        "reviewSubcontractorDetails.taskList.fileAReturn"
      )
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: ReviewSubcontractorDetailsView      = app.injector.instanceOf[ReviewSubcontractorDetailsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = MessagesImpl(Lang.defaultLang, app.injector.instanceOf[MessagesApi])

    val subcontractors: Seq[String] = Seq("Hooper And Associates", "Quint Transportation", "The Kintner Group")

    def html: HtmlFormat.Appendable = view(subcontractors)
    def doc: Document               = Jsoup.parse(html.body)
  }
}
