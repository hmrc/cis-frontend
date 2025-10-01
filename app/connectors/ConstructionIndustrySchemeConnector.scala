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

import models.ChrisSubmissionRequest
import models.monthlyreturns.{CisTaxpayer, MonthlyReturnResponse}
import play.api.Logging
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConstructionIndustrySchemeConnector @Inject() (config: ServicesConfig, http: HttpClientV2)(implicit
  ec: ExecutionContext
) extends HttpReadsInstances
    with Logging {

  private val cisBaseUrl: String = config.baseUrl("construction-industry-scheme") + "/cis"

  def getCisTaxpayer()(implicit hc: HeaderCarrier): Future[CisTaxpayer] =
    http
      .get(url"$cisBaseUrl/taxpayer")
      .execute[CisTaxpayer]

  def retrieveMonthlyReturns(cisId: String)(implicit hc: HeaderCarrier): Future[MonthlyReturnResponse] =
    http
      .get(url"$cisBaseUrl/monthly-returns?cisId=$cisId")
      .execute[MonthlyReturnResponse]

  def submitChris(request: ChrisSubmissionRequest)(implicit hc: HeaderCarrier): Future[Boolean] =
    http
      .post(url"$cisBaseUrl/chris")
      .setHeader("Content-Type" -> "application/json", "Accept" -> "application/json")
      .withBody(Json.toJson(request))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(response) if response.status / 100 == 2 =>
          true

        case Right(response) =>
          logger.warn(s"[submitChris] Unexpected non-2xx status=${response.status}")
          throw new RuntimeException(s"Unexpected CHRIS status: ${response.status}")

        case Left(errorResponse) =>
          if (errorResponse.statusCode / 100 == 5) {
            logger.error(
              s"[submitChris] Upstream 5xx status=${errorResponse.statusCode} message=${errorResponse.message}"
            )
          } else {
            logger.warn(
              s"[submitChris] Upstream 4xx status=${errorResponse.statusCode} message=${errorResponse.message}"
            )
          }
          throw new RuntimeException(s"CHRIS submission failed: ${errorResponse.statusCode} ${errorResponse.message}")
      }
}
