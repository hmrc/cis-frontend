package models.amend

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.{JsError, JsString, Json}

class WhatDoYouWantToAmendStandardSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with OptionValues {

  "WhatDoYouWantToAmendStandard" - {

    "must deserialise valid values" in {

      val gen = Gen.oneOf(WhatDoYouWantToAmendStandard.values.toSeq)

      forAll(gen) { whatDoYouWantToAmendStandard =>
        JsString(whatDoYouWantToAmendStandard.toString)
          .validate[WhatDoYouWantToAmendStandard]
          .asOpt
          .value mustEqual whatDoYouWantToAmendStandard
      }
    }

    "must fail to deserialise invalid values" in {

      val gen = arbitrary[String] suchThat (!WhatDoYouWantToAmendStandard.values.map(_.toString).contains(_))

      forAll(gen) { invalidValue =>
        JsString(invalidValue).validate[WhatDoYouWantToAmendStandard] mustEqual JsError("error.invalid")
      }
    }

    "must serialise" in {

      val gen = Gen.oneOf(WhatDoYouWantToAmendStandard.values.toSeq)

      forAll(gen) { whatDoYouWantToAmendStandard =>
        Json.toJson(whatDoYouWantToAmendStandard) mustEqual JsString(whatDoYouWantToAmendStandard.toString)
      }
    }
  }
}
