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

import models.monthlyreturns.{CisTaxpayer, MonthlyReturn, MonthlyReturnResponse, NilMonthlyReturnRequest}
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.given
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ConstructionIndustrySchemeConnector @Inject() (config: ServicesConfig, http: HttpClientV2)(implicit
  ec: ExecutionContext
) extends HttpReadsInstances {

  private val cisBaseUrl: String = config.baseUrl("construction-industry-scheme") + "/cis"

  def getCisTaxpayer()(implicit hc: HeaderCarrier): Future[CisTaxpayer] =
    http
      .get(url"$cisBaseUrl/taxpayer")
      .execute[CisTaxpayer]

  def retrieveMonthlyReturns(cisId: String)(implicit hc: HeaderCarrier): Future[MonthlyReturnResponse] =
    http
      .get(url"$cisBaseUrl/monthly-returns?cisId=$cisId")
      .execute[MonthlyReturnResponse]

  def createNilMonthlyReturn(payload: NilMonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[MonthlyReturn] =
    http
      .post(url"$cisBaseUrl/monthly-returns/nil/create")
      .withBody(Json.toJson(payload))
      .execute[MonthlyReturn]
}
