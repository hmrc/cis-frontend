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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.HeaderCarrier

class ConstructionIndustrySchemeConnectorSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: ConstructionIndustrySchemeConnector = app.injector.instanceOf[ConstructionIndustrySchemeConnector]

  private val cisId = "abc-123"

  "getCisTaxpayer" should {

    "return CisTaxpayer when BE returns 200 with valid JSON" in {
      stubFor(
        get(urlPathEqualTo("/cis/taxpayer"))
          .willReturn(
            aResponse()
              .withStatus(200)
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
              .withStatus(200)
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
          .willReturn(aResponse().withStatus(500).withBody("boom"))
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
              .withStatus(200)
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
              .withStatus(200)
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
              .withStatus(500)
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
}