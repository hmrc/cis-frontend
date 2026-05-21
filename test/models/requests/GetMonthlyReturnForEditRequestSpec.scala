package models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class GetMonthlyReturnForEditRequestSpec extends AnyWordSpec with Matchers {

  "GetMonthlyReturnForEditRequest JSON format" should {

    "serialize and deserialize correctly" in {
      val model = GetMonthlyReturnForEditRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        isAmendment = Some(true)
      )

      val json = Json.toJson(model)
      json.as[GetMonthlyReturnForEditRequest] mustBe model
    }
  }
}
