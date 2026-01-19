package models.monthlyreturns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class MonthlyReturnRequestSpec extends AnyFreeSpec with Matchers {

  "MonthlyReturnRequest JSON format" - {

    "should write to JSON and read back (round-trip)" in {
      val model = MonthlyReturnRequest(
        instanceId = "CIS-123",
        taxYear = 2025,
        taxMonth = 1
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "instanceId" -> "CIS-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 1
      )

      json.as[MonthlyReturnRequest] mustBe model
    }
  }
}
