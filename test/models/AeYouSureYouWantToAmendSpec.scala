package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class AeYouSureYouWantToAmendSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "AeYouSureYouWantToAmend" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(AeYouSureYouWantToAmend.values.toSeq)

      forAll(gen) {
        aeYouSureYouWantToAmend =>

          JsString(aeYouSureYouWantToAmend.toString).validate[AeYouSureYouWantToAmend].asOpt.value mustEqual aeYouSureYouWantToAmend
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!AeYouSureYouWantToAmend.values.map(_.toString).contains(_))

      forAll(gen) {
        invalidValue =>

          JsString(invalidValue).validate[AeYouSureYouWantToAmend] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(AeYouSureYouWantToAmend.values.toSeq)

      forAll(gen) {
        aeYouSureYouWantToAmend =>

          Json.toJson(aeYouSureYouWantToAmend) mustEqual JsString(aeYouSureYouWantToAmend.toString)
      }
    }
  }
}
