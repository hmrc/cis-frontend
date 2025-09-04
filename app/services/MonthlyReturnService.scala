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

package services

import connectors.ConstructionIndustrySchemeConnector
import models.responses.MrResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MonthlyReturnService @Inject() (constructionIndustrySchemeConnector: ConstructionIndustrySchemeConnector)(implicit
  ec: ExecutionContext
) {
  def retrieveAllMonthlyReturns()(implicit hc: HeaderCarrier): Future[MrResponse] =
    constructionIndustrySchemeConnector.retrieveMonthlyReturns()

  def isDuplicate(year: Int, month: Int)(implicit hc: HeaderCarrier): Future[Boolean] =
    retrieveAllMonthlyReturns().map { res =>
      res.monthlyReturnList.exists(mr => mr.taxYear == year && mr.taxMonth == month)
    }
}
