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
import models.ChrisSubmissionRequest
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import uk.gov.hmrc.http.HeaderCarrier

import org.scalatest.EitherValues.*

class ConstructionIndustrySchemeConnectorSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: ConstructionIndustrySchemeConnector = app.injector.instanceOf[ConstructionIndustrySchemeConnector]

  private val cisId = "abc-123"

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
                  |  "uniqueId": "abc-123",
                  |  "taxOfficeNumber": "111",
                  |  "taxOfficeRef": "test111",
                  |  "employerName1": "TEST LTD"
                  |}""".stripMargin
              )
          )
      )

      val result = connector.getCisTaxpayer().futureValue
      result.uniqueId mustBe "abc-123"
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

      val result = connector.retrieveMonthlyReturns("abc-123").futureValue
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

      val ex = intercept[Exception] {connector.retrieveMonthlyReturns("abc-123").futureValue}
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
        connector.retrieveMonthlyReturns("abc-123").futureValue
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
        connector.retrieveMonthlyReturns("abc-123").futureValue
      }
      ex.getMessage.toLowerCase must include("exception")
    }
  }

  "submitChris" should {

    "return Right(HttpResponse) when BE returns 200" in  {
      stubFor(
        post(urlPathEqualTo("/cis/chris"))
          .willReturn(aResponse().withStatus(OK))
      )
      val result = connector.submitChris(payload).futureValue
      result mustBe a[Right[?, ?]]
      result.value.status mustBe 200

    }

    "return Right(HttpResponse) for other 2xx (e.g. 201)" in {
      stubFor(
        post(urlPathEqualTo("/cis/chris"))
          .willReturn(aResponse().withStatus(CREATED))
      )
      val result = connector.submitChris(payload).futureValue
      result mustBe a[Right[?, ?]]
      result.value.status mustBe 201    }

    "return Right(HttpResponse) for unexpected non-2xx (e.g. 304)" in {
      stubFor(
        post(urlPathEqualTo("/cis/chris"))
          .willReturn(aResponse().withStatus(NOT_MODIFIED).withBody("not modified"))      )

      val result = connector.submitChris(payload).futureValue
      result mustBe a[Right[?, ?]]
      val response = result.value
      response.status mustBe 304
    }

    "return Left(UpstreamErrorResponse) for 4xx (e.g. 404)" in {
      stubFor(
        post(urlPathEqualTo("/cis/chris"))
          .willReturn(aResponse().withStatus(NOT_FOUND).withBody("""{"message":"not found"}"""))
      )

      val result = connector.submitChris(payload).futureValue
      result mustBe a[Left[?, ?]]
      val response = result.left.value
      response.statusCode mustBe 404
      response.message.toLowerCase must include ("not found")
    }

    "return Left(UpstreamErrorResponse) for 5xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/cis/chris"))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("boom"))
      )

      val result = connector.submitChris(payload).futureValue
      result mustBe a[Left[?, ?]]
      val response = result.left.value
      response.statusCode mustBe 500
      response.message.toLowerCase must include ("boom")
    }
  }
}