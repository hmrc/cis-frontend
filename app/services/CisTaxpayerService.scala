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

package services

import com.google.inject.{Inject, Singleton}
import connectors.ConstructionIndustrySchemeConnector
import models.SimpleCisTaxpayer
import models.requests.IdentifierRequest
import play.api.Logging
import repositories.CisTaxpayerCache
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@Singleton
class CisTaxpayerService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  cisTaxpayerCache: CisTaxpayerCache
)(using ExecutionContext)
    extends Logging
    with FrontendHeaderCarrierProvider {

  /** @return the CIS taxpayer deduced from the Organisation user's CIS enrolment. */
  def findBySession(using HeaderCarrier): Future[SimpleCisTaxpayer] =
    cisConnector.getCisTaxpayer() map SimpleCisTaxpayer.from

  /** @return None if either the taxpayer can't be found OR they are NOT a client of the agent. */
  def findByCisId(cisId: String)(using IdentifierRequest[?]): Future[Option[SimpleCisTaxpayer]] =
    cisTaxpayerCache
      .find(cisId)
      .flatMap {
        case Some(cisTaxpayer) =>
          cisConnector
            .hasClient(cisTaxpayer.taxOfficeNumber, cisTaxpayer.taxOfficeRef)
            .map(isClient => if isClient then Some(cisTaxpayer) else None)
        case None              =>
          logger.info(s"Cache miss for scheme <$cisId>; using client list.")
          findInClientList(cisId)
      }
      .recoverWith { ex =>
        logger.warn(s"Falling back to client list; failed to fetch scheme <$cisId> from cache:", ex)
        findInClientList(cisId)
      }

  private def findInClientList(cisId: String)(using req: IdentifierRequest[?]) =
    for clientList <- cisConnector.getAllClients yield
      cisTaxpayerCache
        .insert(clientList) // Run this asynchronously to avoid blocking user flow if cache fails
        .andThen { case Failure(ex) =>
          logger.warn(s"Fetched client list for ${req.agentInfo} but failed to cache:", ex)
        }

      clientList.find(_.id == cisId)
}
