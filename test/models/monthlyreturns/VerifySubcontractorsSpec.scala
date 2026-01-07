package models.monthlyreturns

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsError, JsString, Json}

class VerifySubcontractorsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "VerifySubcontractors" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(VerifySubcontractors.values.toSeq)

      forAll(gen) {
        verifySubcontractors =>

          JsString(verifySubcontractors.toString).validate[VerifySubcontractors].asOpt.value mustEqual verifySubcontractors
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!VerifySubcontractors.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[VerifySubcontractors] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(VerifySubcontractors.values.toSeq)

      forAll(gen) {
        verifySubcontractors =>

          Json.toJson(verifySubcontractors) mustEqual JsString(verifySubcontractors.toString)
      }
    }
  }
}
