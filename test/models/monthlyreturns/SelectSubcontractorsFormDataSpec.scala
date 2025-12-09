package models.monthlyreturns

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class SelectSubcontractorsFormDataSpec extends AnyWordSpec with Matchers {

  "SelectSubcontractorsFormData" should {
    "serialise and deserialise correctly via Json" in {
      val model = new SelectSubcontractorsFormData(
        confirmation = true,
        monthsToInclude = List(true, true, true, true)
      )

      val json = Json.toJson(model)
      json.as[SelectSubcontractorsFormData] mustBe model
    }

    "produce the correct Json structure" in {
      val model = new SelectSubcontractorsFormData(
        confirmation = true,
        monthsToInclude = List(true, false)
      )

      val json = Json.toJson(model)
      (json \ "confirmation").as[Boolean] mustBe true
      (json \ "monthsToInclude").as[List[Boolean]] mustBe (List(true, false))
    }

    "handle an empty monthsToInclude list" in {
      val model = SelectSubcontractorsFormData(confirmation = true, monthsToInclude = Nil)

      val json = Json.toJson(model)
      json.as[SelectSubcontractorsFormData] mustBe model
    }

    "support equal comparison" in {
      val model1 = SelectSubcontractorsFormData(true, List(true, false))
      val model2 = SelectSubcontractorsFormData(true, List(true, false))

      model1 mustBe model2
    }
  }
}
