package models.submission

import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec

class SubmissionStatusSpec extends AnyWordSpec with Matchers {

  "SubmissionStatus.fromString" should {

    "return correct status for known values" in {
      SubmissionStatus.fromString("STARTED")              shouldBe SubmissionStatus.Started
      SubmissionStatus.fromString("PENDING")              shouldBe SubmissionStatus.Pending
      SubmissionStatus.fromString("ACCEPTED")             shouldBe SubmissionStatus.Accepted
      SubmissionStatus.fromString("TIMED_OUT")            shouldBe SubmissionStatus.TimedOut
      SubmissionStatus.fromString("SUBMITTED")            shouldBe SubmissionStatus.Submitted
      SubmissionStatus.fromString("SUBMITTED_NO_RECEIPT") shouldBe SubmissionStatus.SubmittedNoReceipt
      SubmissionStatus.fromString("DEPARTMENTAL_ERROR")   shouldBe SubmissionStatus.DepartmentalError
      SubmissionStatus.fromString("FATAL_ERROR")          shouldBe SubmissionStatus.FatalError
    }

    "return Unknown for unrecognised values" in {
      SubmissionStatus.fromString("UNKNOWN_STATUS") shouldBe SubmissionStatus.Unknown("UNKNOWN_STATUS")
    }

  }
}
