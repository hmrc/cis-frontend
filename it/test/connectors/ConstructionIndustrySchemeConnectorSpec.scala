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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlPathMatching}
import itutil.ApplicationWithWiremock
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.http.HeaderCarrier

class ConstructionIndustrySchemeConnectorSpec extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with IntegrationPatience
  with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: ConstructionIndustrySchemeConnector = app.injector.instanceOf[ConstructionIndustrySchemeConnector]

  "retrieveMonthlyReturns" should {

    "return an MrResponse when BE returns 200 with valid JSON" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
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

      val result = connector.retrieveMonthlyReturns().futureValue

      result.monthlyReturnList.map(_.monthlyReturnId) should contain allOf(101L, 102L)
      result.monthlyReturnList.map(_.taxMonth) should contain allOf(4, 5)
    }

    "fail when BE returns 200 with invalid JSON" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "notMonthlyReturnList": [] }""") // missing required field
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturns().futureValue
      }
      ex.getMessage.toLowerCase should include("monthlyreturnlist")
    }

    "propagate an upstream error when BE returns 500" in {
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("test error")
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturns().futureValue
      }
      ex.getMessage should include("returned 500")
    }

    "fail the future on connection issues" in {
      // Using an invalid status in WireMock to simulate a connection problem
      stubFor(
        get(urlPathMatching("/cis/monthly-returns"))
          .willReturn(
            aResponse().withStatus(0)
          )
      )

      val ex = intercept[Exception] {
        connector.retrieveMonthlyReturns().futureValue
      }
      ex.getMessage.toLowerCase should include("exception")
    }
  }
}