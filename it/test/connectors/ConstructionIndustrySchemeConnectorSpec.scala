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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, post, stubFor, urlPathEqualTo, urlPathMatching}
import itutil.ApplicationWithWiremock
import models.submission.{ChrisSubmissionRequest, CreateSubmissionRequest, UpdateSubmissionRequest}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues.convertOptionToValuable
import play.api.http.Status.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.Instant

class ConstructionIndustrySchemeConnectorSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: ConstructionIndustrySchemeConnector = app.injector.instanceOf[ConstructionIndustrySchemeConnector]

  private val cisId = "123"

  private val payload = ChrisSubmissionRequest(
    utr = "1234567890",
    aoReference = "123/AB456",
    informationCorrect = "yes",
    inactivity = "no",
    monthYear = "2025-10"
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

}