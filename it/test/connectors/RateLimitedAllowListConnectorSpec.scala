/*
 * Copyright 2026 HM Revenue & Customs
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
import itutil.{ApplicationWithWiremock, WireMockConstants}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

class RateLimitedAllowListConnectorSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with ApplicationWithWiremock {

  implicit val hc: HeaderCarrier    = HeaderCarrier()
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  override val extraConfig: Map[String, Any] = super.extraConfig ++ Map(
    "microservice.services.rate-limited-allow-list.host" -> WireMockConstants.stubHost,
    "microservice.services.rate-limited-allow-list.port" -> WireMockConstants.stubPort
  )

  lazy val connector: RateLimitedAllowListConnector = app.injector.instanceOf[RateLimitedAllowListConnector]

  private val serviceName  = "cis-management-frontend"
  private val featureName  = "cis-private-beta-2026"
  private val identifier   = "754/EZ00047"
  private val expectedPath = s"/rate-limited-allow-list/services/$serviceName/features/$featureName"

  "checkAllowList" should {

    "return true when the service responds with included: true" in {
      stubFor(
        post(urlPathEqualTo(expectedPath))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"included": true}""")
          )
      )

      connector.checkAllowList(serviceName, featureName, identifier).futureValue mustBe true
    }

    "return false when the service responds with included: false" in {
      stubFor(
        post(urlPathEqualTo(expectedPath))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody("""{"included": false}""")
          )
      )

      connector.checkAllowList(serviceName, featureName, identifier).futureValue mustBe false
    }

    "propagate an upstream error when the service returns 500" in {
      stubFor(
        post(urlPathEqualTo(expectedPath))
          .willReturn(aResponse().withStatus(INTERNAL_SERVER_ERROR).withBody("error"))
      )

      val ex = intercept[Exception] {
        connector.checkAllowList(serviceName, featureName, identifier).futureValue
      }
      ex.getMessage must include("returned 500")
    }
  }
}
