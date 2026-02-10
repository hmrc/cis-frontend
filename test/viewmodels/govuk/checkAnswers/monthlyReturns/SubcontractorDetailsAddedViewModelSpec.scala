package viewmodels.govuk.checkAnswers.monthlyReturns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.Call
import viewmodels.checkAnswers.monthlyreturns.{SubcontractorDetailsAddedRow, SubcontractorDetailsAddedViewModel}

class SubcontractorDetailsAddedViewModelSpec extends AnyFreeSpec with Matchers {

  "SubcontractorDetailsAddedViewModel.addedCount" - {

    "must return the first headingArg parsed as an Int when present (multiple heading)" in {
      val vm = SubcontractorDetailsAddedViewModel(
        headingKey = "subcontractorDetailsAdded.heading.multiple",
        headingArgs = Seq(Integer.valueOf(3)), // matches {0} in messages.en
        rows = Nil,
        hasIncomplete = false
      )

      vm.addedCount mustBe 3
    }

    "must return 1 when headingArgs is empty (single heading)" in {
      val vm = SubcontractorDetailsAddedViewModel(
        headingKey = "subcontractorDetailsAdded.heading.single",
        headingArgs = Seq.empty,
        rows = Nil,
        hasIncomplete = false
      )

      vm.addedCount mustBe 1
    }
  }

  "SubcontractorDetailsAddedViewModel fields" - {

    "must keep rows and hasIncomplete" in {
      val rows = Seq(
        SubcontractorDetailsAddedRow(
          index = 0,
          subcontractorId = 1001L,
          name = "TyneWear Ltd",
          detailsAdded = true,
          changeCall = Call("GET", "/change-0"),
          removeCall = Call("GET", "/remove-0")
        )
      )

      val vm = SubcontractorDetailsAddedViewModel(
        headingKey = "subcontractorDetailsAdded.heading.multiple",
        headingArgs = Seq(Integer.valueOf(1)),
        rows = rows,
        hasIncomplete = true
      )

      vm.rows mustBe rows
      vm.hasIncomplete mustBe true
    }
  }
}
