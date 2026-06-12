package models.submission

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

class ResetGovTalkStatusRequestSpec extends AnyWordSpec with Matchers {
  "ResetGovTalkStatusRequest.format" should {

    "serialize and deserialize correctly" in {

      val model = ResetGovTalkStatusRequest(
        userIdentifier = "123",
        formResultID = "12890",
        oldProtocolStatus = "dataRequest",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      val json = Json.toJson(model)

      json.validate[ResetGovTalkStatusRequest] mustBe JsSuccess(model)
    }
  }
}
