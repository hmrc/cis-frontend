package models

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class AddSubcontractorDetailsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with OptionValues {

  "AddSubcontractorDetails" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(AddSubcontractorDetails.values.toSeq)

      forAll(gen) { addSubcontractorDetails =>
        JsString(addSubcontractorDetails.toString)
          .validate[AddSubcontractorDetails]
          .asOpt
          .value mustEqual addSubcontractorDetails
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!AddSubcontractorDetails.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>
        JsString(invalidValue).validate[AddSubcontractorDetails] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(AddSubcontractorDetails.values.toSeq)

      forAll(gen) { addSubcontractorDetails =>
        Json.toJson(addSubcontractorDetails) mustEqual JsString(addSubcontractorDetails.toString)
      }
    }
  }
}
