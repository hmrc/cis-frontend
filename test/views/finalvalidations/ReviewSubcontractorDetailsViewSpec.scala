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
import controllers.monthlyreturns.routes.SelectSubcontractorsController
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi, MessagesImpl}
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import views.html.finalvalidations.ReviewSubcontractorDetailsView
import models.finalvalidation.{ReviewSubcontractorDetailsPageModel, ReviewSubcontractorDetailsRow}

class ReviewSubcontractorDetailsViewSpec extends SpecBase {

  "ReviewSubcontractorDetailsView" - {

    "must render the correct page title" in new Setup {
      doc.title must include(messages("finalValidations.reviewSubcontractorDetails.title"))
    }

    "must render the correct heading" in new Setup {
      doc.select("h1").text mustEqual messages("finalValidations.reviewSubcontractorDetails.heading")
    }

    "must render the intro paragraph" in new Setup {
      doc.select("p.govuk-body").text must include(messages("finalValidations.reviewSubcontractorDetails.intro"))
    }

    "must render each subcontractor name as a task list item link" in new Setup {
      val subcontractorNames =
        doc
          .select(".govuk-task-list__link > span:not(.govuk-visually-hidden)")
          .eachText()

      subcontractors.foreach { subcontractor =>
        subcontractorNames must contain(subcontractor.name)
      }
    }

    "must render Review as visually hidden text for each subcontractor link" in new Setup {
      val hiddenText =
        doc
          .select(".govuk-task-list__link .govuk-visually-hidden")
          .eachText()

      hiddenText.size mustEqual subcontractors.size

      hiddenText.forEach(
        _ mustEqual messages(
          "finalValidations.reviewSubcontractorDetails.taskList.review"
        )
      )
    }

    "must render each subcontractor link pointing to the update subcontractor details page" in new Setup {
      val taskListLinks = doc.select(".govuk-task-list__link")

      subcontractors.zipWithIndex.foreach { case (subcontractor, index) =>
        taskListLinks.get(index).attr("href") mustEqual
          controllers.finalvalidations.routes.UpdateSubcontractorDetailsController
            .onPageLoad(subcontractor.subcontractorId)
            .url
      }
    }

    "must render the Incomplete tag for each subcontractor" in new Setup {
      val tags = doc.select(".govuk-task-list__status .govuk-tag").eachText()

      tags.size mustEqual subcontractors.size
      tags.forEach(_ mustEqual messages("finalValidations.reviewSubcontractorDetails.taskList.status.incomplete"))
    }

    "must render File a return as the last task list item without a link" in new Setup {
      val taskListItems = doc.select(".govuk-task-list__item")
      val lastItem      = taskListItems.last()

      lastItem.select(".govuk-task-list__name-and-hint").text mustEqual messages(
        "finalValidations.reviewSubcontractorDetails.taskList.fileAReturn"
      )
      lastItem.select("a").isEmpty mustBe true
    }

    "must render Cannot continue yet status for File a return" in new Setup {
      val taskListItems = doc.select(".govuk-task-list__item")
      val lastStatus    = taskListItems.last().select(".govuk-task-list__status")

      lastStatus.text mustEqual messages(
        "finalValidations.reviewSubcontractorDetails.taskList.status.cannotContinueYet"
      )
      lastStatus.hasClass("govuk-task-list__status--cannot-start-yet") mustBe true
    }

    "must render the back link with the correct URL" in new Setup {
      val backLink = doc.select("p.govuk-body > a.govuk-link").last()

      backLink.text mustEqual messages("finalValidations.reviewSubcontractorDetails.backLink")
      backLink.attr("href") mustEqual SelectSubcontractorsController.onPageLoad(None).url
    }

    "must render only File a return when subcontractors list is empty" in new Setup {
      override val subcontractors: Seq[ReviewSubcontractorDetailsRow] = Seq.empty
      val taskListItems                                               = doc.select(".govuk-task-list__item")

      taskListItems.size mustEqual 1
      taskListItems.first().select(".govuk-task-list__name-and-hint").text mustEqual messages(
        "finalValidations.reviewSubcontractorDetails.taskList.fileAReturn"
      )
    }
  }

  trait Setup {
    val app: Application                          = applicationBuilder().build()
    val view: ReviewSubcontractorDetailsView      = app.injector.instanceOf[ReviewSubcontractorDetailsView]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = MessagesImpl(Lang.defaultLang, app.injector.instanceOf[MessagesApi])

    val subcontractors: Seq[ReviewSubcontractorDetailsRow] = Seq(
      ReviewSubcontractorDetailsRow(
        subcontractorId = 1L,
        name = "Hooper And Associates",
        hasErrors = true
      ),
      ReviewSubcontractorDetailsRow(
        subcontractorId = 2L,
        name = "Quint Transportation",
        hasErrors = true
      ),
      ReviewSubcontractorDetailsRow(
        subcontractorId = 3L,
        name = "The Kintner Group",
        hasErrors = true
      )
    )

    def model: ReviewSubcontractorDetailsPageModel =
      ReviewSubcontractorDetailsPageModel(
        subcontractors = subcontractors,
        canContinue = false,
        backUrl = SelectSubcontractorsController.onPageLoad(None).url
      )

    def html: HtmlFormat.Appendable = view(model)
    def doc: Document               = Jsoup.parse(html.body)
  }
}
