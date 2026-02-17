/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import itutil.ApplicationWithWiremock
import models.requests.SendSuccessEmailRequest
import models.monthlyreturns.MonthlyReturnRequest
import models.submission.{ChrisSubmissionRequest, CreateSubmissionRequest, UpdateSubmissionRequest}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext

class ConstructionIndustrySchemeConnectorSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  val connector: ConstructionIndustrySchemeConnector = app.injector.instanceOf[ConstructionIndustrySchemeConnector]

  private val cisId = "123"

  private val payload = ChrisSubmissionRequest(
    utr = "1234567890",
    aoReference = "123/AB456",
    informationCorrect = "yes",
    inactivity = "no",
    monthYear = "2025-10",
    email = "test@test.com",
    isAgent = false,
    clientTaxOfficeNumber = "",
    clientTaxOfficeRef = ""
  )

  "getCisTaxpayer" should {

    "return CisTaxpayer when BE returns 200 with valid JSON" in {
      stubFor(
        get(urlPathEqualTo("/cis/taxpayer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                """{
                  |  "uniqueId": "123",
                  |  "taxOfficeNumber": "111",
                  |  "taxOfficeRef": "test111",
                  |  "employerName1": "TEST LTD"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getCisTaxpayer().futureValue
      result.uniqueId mustBe "123"
      result.taxOfficeNumber mustBe "111"
      result.taxOfficeRef mustBe "test111"
      result.employerName1 mustBe Some("TEST LTD")
    }

    "fail when BE returns 200 with invalid JSON" in {
      stubFor(
        get(urlPathEqualTo("/cis/taxpayer"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{ "unexpectedField": true }""")
          )
      )

      val ex = intercept[Exception] {
        connector.getCisTaxpayer().futureValue
      }
      ex.getMessage.toLowerCase must include("uniqueid")
    }

    "propagate an upstream error when BE returns 500" in {
      stubFor(
        get(urlPathEqualTo("/cis/taxpayer"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Exception] {
        connector.getCisTaxpayer().futureValue
      }
      ex.getMessage must include("returned 500")
    }
  }

  "getAgentClient" should {

    val userId = "some-user-id"
    val validJson: JsValue = Json.obj(
      "uniqueId" -> "1",
      "taxOfficeNumber" -> "123",
      "taxOfficeReference" -> "AB001"
    )

    "returns Some(Json) if OK" in {
      stubFor(
        get(urlEqualTo(s"/cis/user-cache/agent-client/$userId"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(Json.stringify(validJson))
          )
      )

      val result = connector.getAgentClient(userId).futureValue

      result mustBe Some(validJson)
    }

    "returns None if NOT_FOUND" in {
      stubFor(
        get(urlEqualTo(s"/cis/user-cache/agent-client/$userId"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
          )
      )

      val result = connector.getAgentClient(userId).futureValue

      result mustBe None
    }

    "throws HttpException for other codes" in {
      stubFor(
        get(urlEqualTo(s"/cis/user-cache/agent-client/$userId"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Something broke")
          )

      )

      val result = connector
        .getAgentClient(userId)
        .failed
        .futureValue

      result mustBe a[HttpException]
      result.getMessage must include("Something broke")
    }

  }

  "getAgentClientTaxpayer" should {

    "return CisTaxpayer when BE returns 200 with valid JSON" in {
      val taxOfficeNumber = "111"
      val taxOfficeRef = "test111"

      stubFor(
        get(urlPathEqualTo(s"/cis/agent/client-taxpayer/$taxOfficeNumber/$taxOfficeRef"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                """{
                  |  "uniqueId": "123",
                  |  "taxOfficeNumber": "111",
                  |  "taxOfficeRef": "test111",
                  |  "employerName1": "TEST LTD"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getAgentClientTaxpayer(taxOfficeNumber, taxOfficeRef).futureValue

      result.uniqueId mustBe "123"
      result.taxOfficeNumber mustBe "111"
      result.taxOfficeRef mustBe "test111"
      result.employerName1 mustBe Some("TEST LTD")
    }

    "fail when BE returns 200 with invalid JSON" in {
      val taxOfficeNumber = "111"
      val taxOfficeRef = "test111"

      stubFor(
        get(urlPathEqualTo(s"/cis/agent/client-taxpayer/$taxOfficeNumber/$taxOfficeRef"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{ "unexpectedField": true }""")
          )
      )

      val ex = intercept[Exception] {
        connector.getAgentClientTaxpayer(taxOfficeNumber, taxOfficeRef).futureValue
      }

      ex.getMessage.toLowerCase must include("uniqueid")
    }

    "propagate an upstream error when BE returns 500" in {
      val taxOfficeNumber = "111"
      val taxOfficeRef = "test111"

      stubFor(
        get(urlPathEqualTo(s"/cis/agent/client-taxpayer/$taxOfficeNumber/$taxOfficeRef"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("boom")
          )
      )

      val ex = intercept[Exception] {
        connector.getAgentClientTaxpayer(taxOfficeNumber, taxOfficeRef).futureValue
      }

      ex.getMessage must include("returned 500")
    }
  }

  "retrieveMonthlyReturnForEditDetails" should {

    "return GetAllMonthlyReturnDetailsResponse when BE returns 200 with valid JSON" in {
      val instanceId = "CIS-123"
      val taxMonth   = 10
      val taxYear    = 2025

      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns-edit/"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(
                s"""{
                   |  "scheme": [{
                   |    "schemeId": 1,
                   |    "instanceId": "$instanceId",
                   |    "accountsOfficeReference": "123PA12345678",
                   |    "taxOfficeNumber": "123",
                   |    "taxOfficeReference": "AB456"
                   |  }],
                   |  "monthlyReturn": [{
                   |    "monthlyReturnId": 101,
                   |    "taxYear": $taxYear,
                   |    "taxMonth": $taxMonth
                   |  }],
                   |  "subcontractors": [{
                   |    "subcontractorId": 1001,
                   |    "tradingName": "Test Subcontractor Ltd"
                   |  }, {
                   |    "subcontractorId": 1002,
                   |    "firstName": "John",
                   |    "surname": "Doe"
                   |  }],
                   |  "monthlyReturnItems": [{
                   |    "monthlyReturnId": 101,
                   |    "monthlyReturnItemId": 2001,
                   |    "subcontractorId": 1001
                   |  }],
                   |  "submission": [{
                   |    "submissionId": 3001,
                   |    "submissionType": "MONTHLY_RETURN",
                   |    "schemeId": 1
                   |  }]
                   |}""".stripMargin
              )
          )
      )

      val result = connector.retrieveMonthlyReturnForEditDetails(instanceId, taxMonth, taxYear).futureValue

      result.scheme.length mustBe 1
      result.scheme.head.instanceId mustBe instanceId

      result.monthlyReturn.length mustBe 1
      result.monthlyReturn.head.taxYear mustBe taxYear
      result.monthlyReturn.head.taxMonth mustBe taxMonth

      result.subcontractors.length mustBe 2
      result.subcontractors.head.subcontractorId mustBe 1001
      result.subcontractors.head.tradingName mustBe Some("Test Subcontractor Ltd")
      result.subcontractors(1).subcontractorId mustBe 1002

      result.monthlyReturnItems.length mustBe 1
      result.monthlyReturnItems.head.subcontractorId mustBe Some(1001)

      result.submission.length mustBe 1
      result.submission.head.submissionType mustBe "MONTHLY_RETURN"
    }

    "return empty collections when BE returns 200 with empty arrays" in {
      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns-edit/"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{
                  |  "scheme": [],
                  |  "monthlyReturn": [],
                  |  "subcontractors": [],
                  |  "monthlyReturnItems": [],
                  |  "submission": []
                  |}""".stripMargin
              )
          )
      )

      val result = connector.retrieveMonthlyReturnForEditDetails("CIS-456", 5, 2024).futureValue

      result.scheme mustBe empty
      result.monthlyReturn mustBe empty
      result.subcontractors mustBe empty
      result.monthlyReturnItems mustBe empty
      result.submission mustBe empty
    }

    "propagate an upstream error when BE returns 500" in {
      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns-edit/"))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("Internal Server Error")
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturnForEditDetails("CIS-ERR", 1, 2025).futureValue
      }
      ex.getMessage must include("returned 500")
    }

    "propagate an upstream error when BE returns 404" in {
      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns-edit/"))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody("Not Found")
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturnForEditDetails("CIS-NOTFOUND", 3, 2025).futureValue
      }
      ex.getMessage must include("returned 404")
    }
  }

  "retrieveMonthlyReturns(cisId)" should {

    "return an MonthlyReturnResponse when BE returns 200 with valid JSON" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .withQueryParam("cisId", equalTo(cisId))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(
                """
                  |{
                  |  "monthlyReturnList": [
                  |    { "monthlyReturnId": 101, "taxYear": 2024, "taxMonth": 4 },
                  |    { "monthlyReturnId": 102, "taxYear": 2024, "taxMonth": 5 }
                  |  ]
                  |}
                  |""".stripMargin
              )
          )
      )

      val result = connector.retrieveMonthlyReturns("123").futureValue
      result.monthlyReturnList.map(_.monthlyReturnId) must contain allOf(101L, 102L)
      result.monthlyReturnList.map(_.taxMonth) must contain allOf(4, 5)
    }

    "fail when BE returns 200 with invalid JSON" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .withQueryParam("cisId", equalTo(cisId))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{ "notMonthlyReturnList": [] }""")
          )
      )

      val ex = intercept[Exception] {connector.retrieveMonthlyReturns("123").futureValue}
      ex.getMessage.toLowerCase must include("monthlyreturnlist")
    }

    "propagate an upstream error when BE returns 500" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .withQueryParam("cisId", equalTo(cisId))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("test error")
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturns("123").futureValue
      }
      ex.getMessage must include("returned 500")
    }

    "fail the future on connection issues" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .withQueryParam("cisId", equalTo(cisId))
          .willReturn(
            aResponse().withStatus(0)
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturns("123").futureValue
      }
      ex.getMessage.toLowerCase must include("exception")
    }
  }

  "createNilMonthlyReturn(payload)" should {

    "POST to /cis/monthly-returns/nil/create and return status on 200" in {
      val body =
        """
          |{ "status": "STARTED" }
          |""".stripMargin

      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns/nil/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(body)
          )
      )

      val req = models.monthlyreturns.NilMonthlyReturnRequest(
        instanceId = cisId,
        taxYear = 2024,
        taxMonth = 10,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )

      val out = connector.createNilMonthlyReturn(req).futureValue
      out.status mustBe "STARTED"
    }

    "propagate upstream error on non-2xx" in {
      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns/nil/create"))
          .willReturn(aResponse().withStatus(500).withBody("boom"))
      )

      val req = models.monthlyreturns.NilMonthlyReturnRequest(cisId, 2024, 10, "Y", "Y")
      val ex = intercept[Exception] { connector.createNilMonthlyReturn(req).futureValue }
      ex.getMessage must include("returned 500")
    }
  }

  "createSubmission" should {

    "POST to /cis/submissions/create and return submissionId on 201" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withStatus(CREATED)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "submissionId": "sub-123" }""")
          )
      )

      val req = CreateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        emailRecipient = Some("user@test.com")
      )

      val res = connector.createSubmission(req).futureValue
      res.submissionId mustBe "sub-123"
    }

    "fail the future on 400 (bad request)" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/create"))
          .willReturn(aResponse().withStatus(BAD_REQUEST).withBody("""{"message":"invalid"}"""))
      )

      val req = CreateSubmissionRequest("123", 2024, 4)
      val ex = intercept[Throwable] {
        connector.createSubmission(req).futureValue
      }
      ex.getMessage.toLowerCase must include("400")
    }

    "fail the future on 5xx (e.g. 502)" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/create"))
          .willReturn(aResponse().withStatus(BAD_GATEWAY).withBody("bad gateway"))
      )

      val req = CreateSubmissionRequest("123", 2024, 4)
      val ex = intercept[Throwable] {
        connector.createSubmission(req).futureValue
      }
      ex.getMessage.toLowerCase must include("502")
    }
  }

  "submitToChris" should {

    "return a ChrisSubmissionResponse when BE returns 200 SUBMITTED" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/sub-123/submit-to-chris"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{
                  |  "submissionId": "sub-123",
                  |  "status": "SUBMITTED",
                  |  "hmrcMarkGenerated": "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
                  |  "correlationId": "CID123",
                  |  "gatewayTimestamp": "2025-01-01T00:00:00Z"
                  |}""".stripMargin
              )
          )
      )

      val out = connector.submitToChris("sub-123", payload).futureValue
      out.submissionId mustBe "sub-123"
      out.status mustBe "SUBMITTED"
      out.hmrcMarkGenerated mustBe "Dj5TVJDyRYCn9zta5EdySeY4fyA="
    }

    "support 202 ACCEPTED with nextPollInSeconds" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/sub-123/submit-to-chris"))
          .willReturn(
            aResponse()
              .withStatus(ACCEPTED)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{
                  |  "submissionId": "sub-123",
                  |  "status": "ACCEPTED",
                  |  "hmrcMarkGenerated": "IRMARK999",
                  |  "correlationId": "CID999",
                  |  "gatewayTimestamp": "2025-01-01T00:00:00Z",
                  |  "responseEndPoint": {
                  |    "url": "https://chris.example/poll/CID-123",
                  |    "pollIntervalSeconds": 15
                  |  }
                  |}""".stripMargin
              )
          )
      )

      val out = connector.submitToChris("sub-123", payload).futureValue
      out.status mustBe "ACCEPTED"
      val ep = out.responseEndPoint.value
      ep.url mustBe "https://chris.example/poll/CID-123"
      ep.pollIntervalSeconds mustBe 15    }

    "fail the future when BE returns non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/sub-err/submit-to-chris"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[Throwable] {
        connector.submitToChris("sub-err", payload).futureValue
      }
      ex.getMessage.toLowerCase must include("500")
    }
  }

  "updateSubmission" should {

    "complete successfully on 204 No Content" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/sub-123/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        submittableStatus = "FATAL_ERROR"
      )

      connector.updateSubmission("sub-123", req).futureValue mustBe ()
    }

    "fail the future with UpstreamErrorResponse on non-2xx (e.g. 502)" in {
      stubFor(
        post(urlPathEqualTo("/cis/submissions/sub-123/update"))
          .willReturn(aResponse().withStatus(BAD_GATEWAY).withBody("bad gateway"))
      )

      val req = UpdateSubmissionRequest("123", 2024, 4, submittableStatus = "FATAL_ERROR")

      val err = connector.updateSubmission("sub-123", req).failed.futureValue
      err mustBe a[UpstreamErrorResponse]
      err.asInstanceOf[UpstreamErrorResponse].statusCode mustBe BAD_GATEWAY
    }
  }

  "getSubmissionStatus" should {

    val pollUrl = "https%3A%2F%2Fsomeurl.com%3Ftimestamp%3D2025-01-15T10%3A30%3A00Z"
    val correlationId = "CID-ABC-123"

    "return ChrisPollResponse with SUBMITTED status when BE returns 200 with valid JSON" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{
                  |  "status": "SUBMITTED",
                  |  "pollUrl": "https://chris.example/poll/next"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      result.status mustBe "SUBMITTED"
      result.pollUrl mustBe Some("https://chris.example/poll/next")
    }

    "return ChrisPollResponse with ACCEPTED status and no pollUrl when pollUrl is absent" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{
                  |  "status": "ACCEPTED"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      result.status mustBe "ACCEPTED"
      result.pollUrl mustBe None
    }

    "return ChrisPollResponse with FATAL_ERROR status" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody(
                """{
                  |  "status": "FATAL_ERROR"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      result.status mustBe "FATAL_ERROR"
      result.pollUrl mustBe None
    }

    "correctly encode the pollUrl query parameter" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .withQueryParam("pollUrl", equalTo("https%3A%2F%2Fsomeurl.com%3Ftimestamp%3D2025-01-15T10%3A30%3A00Z"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "status": "SUBMITTED" }""")
          )
      )

      val result = connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      result.status mustBe "SUBMITTED"
    }

    "fail when BE returns 200 with invalid JSON" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{ "unexpectedField": true }""")
          )
      )

      val ex = intercept[Exception] {
        connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      }
      ex.getMessage.toLowerCase must include("status")
    }

    "propagate an upstream error when BE returns 404" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(NOT_FOUND)
              .withBody("submission not found")
          )
      )

      val ex = intercept[Exception] {
        connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      }
      ex.getMessage must include("returned 404")
    }

    "propagate an upstream error when BE returns 500" in {
      stubFor(
        get(urlPathMatching("/cis/submissions/poll"))
          .withQueryParam("correlationId", equalTo(correlationId))
          .willReturn(
            aResponse()
              .withStatus(INTERNAL_SERVER_ERROR)
              .withBody("internal server error")
          )
      )

      val ex = intercept[Exception] {
        connector.getSubmissionStatus(pollUrl, correlationId).futureValue
      }
      ex.getMessage must include("returned 500")
    }
  }

  "createMonthlyReturn(payload)" should {

    "POST /cis/monthly-returns/standard/create and return Unit on 201" in {
      val req = MonthlyReturnRequest(
        instanceId = cisId,
        taxYear = 2025,
        taxMonth = 1
      )

      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns/standard/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(CREATED))
      )

      connector.createMonthlyReturn(req).futureValue mustBe ((): Unit)
    }

    "fail the future when BE returns non-201 (e.g. 500)" in {
      val req = MonthlyReturnRequest(
        instanceId = cisId,
        taxYear = 2025,
        taxMonth = 1
      )

      stubFor(
        post(urlPathEqualTo("/cis/monthly-returns/standard/create"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = connector.createMonthlyReturn(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe INTERNAL_SERVER_ERROR
    }
  }

  "hasClient(taxOfficeNumber, taxOfficeReference)" should {

    "GET /cis/agent/has-client/:ton/:tor and return true when BE returns 200" in {
      stubFor(
        get(urlPathEqualTo("/cis/agent/has-client/163/AB0063"))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "hasClient": true }""")
          )
      )

      connector.hasClient("163", "AB0063").futureValue mustBe true
    }

    "fail the future when BE returns non-200 (e.g. 403)" in {
      stubFor(
        get(urlPathEqualTo("/cis/agent/has-client/163/AB0063"))
          .willReturn(aResponse().withStatus(FORBIDDEN).withBody("""{"error":"nope"}"""))
      )

      val ex = connector.hasClient("163", "AB0063").failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe FORBIDDEN
    }
  }

  "sendSuccessfulEmail" should {

    "POST to /cis/submissions/:submissionId/send-success-email and succeed on 202" in {
      val submissionId = "sub-123"
      val req = SendSuccessEmailRequest(
        email = "test@test.com",
        month = "September",
        year = "2025"
      )

      stubFor(
        post(urlPathEqualTo(s"/cis/submissions/$submissionId/send-success-email"))
          .withHeader("Content-Type", equalTo("application/json"))
          .willReturn(aResponse().withStatus(ACCEPTED))
      )

      connector.sendSuccessfulEmail(submissionId, req).futureValue mustBe ()
    }

    "fail the future when BE returns non-202 (e.g. 500)" in {
      val submissionId = "sub-err"
      val req = SendSuccessEmailRequest("test@test.com", "September", "2025")

      stubFor(
        post(urlPathEqualTo(s"/cis/submissions/$submissionId/send-success-email"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val ex = intercept[RuntimeException] {
        connector.sendSuccessfulEmail(submissionId, req).futureValue
      }

      ex.getMessage must include("Send email failed")
      ex.getMessage must include("status: 500")
      ex.getMessage must include("boom")
    }
  }
}
