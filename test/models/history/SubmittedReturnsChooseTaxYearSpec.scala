package models.history

import models.history.SubmittedReturnsChooseTaxYear
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class SubmittedReturnsChooseTaxYearSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "SubmittedReturnsChooseTaxYear" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(SubmittedReturnsChooseTaxYear.values.toSeq)

      forAll(gen) {
        submittedReturnsChooseTaxYear =>

          JsString(submittedReturnsChooseTaxYear.toString).validate[SubmittedReturnsChooseTaxYear].asOpt.value mustEqual submittedReturnsChooseTaxYear
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!SubmittedReturnsChooseTaxYear.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[SubmittedReturnsChooseTaxYear] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(SubmittedReturnsChooseTaxYear.values.toSeq)

      forAll(gen) {
        submittedReturnsChooseTaxYear =>

          Json.toJson(submittedReturnsChooseTaxYear) mustEqual JsString(submittedReturnsChooseTaxYear.toString)
      }
    }
  }
}
