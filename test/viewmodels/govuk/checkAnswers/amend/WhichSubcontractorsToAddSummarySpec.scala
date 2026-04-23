package viewmodels.govuk.checkAnswers.amend

import base.SpecBase
import models.CheckMode
import models.amend.WhichSubcontractorsToAdd
import org.scalatest.OptionValues
import pages.amend.WhichSubcontractorsToAddPage
import play.api.i18n.Messages
import play.api.test.Helpers
import viewmodels.checkAnswers.amend.WhichSubcontractorsToAddSummary

class WhichSubcontractorsToAddSummarySpec extends SpecBase with OptionValues {

  private implicit val messages: Messages = Helpers.stubMessages()

  private val subcontractors = WhichSubcontractorsToAdd.mockSubcontractors

  "WhichSubcontractorsToAddSummary" - {

    "when answer is present" - {

      "must return a SummaryListRow with selected subcontractor names" in {
        val selectedIds = Set(subcontractors.head.id, subcontractors(1).id)

        val answers = emptyUserAnswers
          .set(WhichSubcontractorsToAddPage, selectedIds)
          .success
          .value

        val result = WhichSubcontractorsToAddSummary.row(answers).value

        result.key.content.asHtml.toString must include(
          messages("amend.whichSubcontractorsToAdd.checkYourAnswersLabel")
        )

        result.value.content.asHtml.toString must include(subcontractors.head.name)
        result.value.content.asHtml.toString must include(subcontractors(1).name)

        result.actions.value.items.head.href mustBe
          controllers.amend.routes.WhichSubcontractorsToAddController
            .onPageLoad(CheckMode)
            .url

        result.actions.value.items.head.visuallyHiddenText.value mustBe
          messages("amend.whichSubcontractorsToAdd.change.hidden")

        result.actions.value.items.head.attributes must contain("id" -> "which-subcontractors-to-add")
      }

      "must fall back to showing id when subcontractor is not in the map" in {
        val unknownId = "unknown-id"

        val answers = emptyUserAnswers
          .set(WhichSubcontractorsToAddPage, Set(unknownId))
          .success
          .value

        val result = WhichSubcontractorsToAddSummary.row(answers).value

        result.value.content.asHtml.toString must include(unknownId)
      }
    }

    "when answer is not present" - {

      "must return None" in {
        val result = WhichSubcontractorsToAddSummary.row(emptyUserAnswers)

        result mustBe None
      }
    }
  }
}
