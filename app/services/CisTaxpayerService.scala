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
import models.EmployerReference
import models.monthlyreturns.CisTaxpayer
import models.requests.IdentifierRequest
import play.api.Logging
import repositories.CisTaxpayerCache
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

@Singleton
class CisTaxpayerService @Inject() (
  connector: ConstructionIndustrySchemeConnector,
  cache: CisTaxpayerCache
)(using ExecutionContext)
    extends Logging
    with FrontendHeaderCarrierProvider {

  def findForOrganisation(using req: IdentifierRequest[?]): Future[CisTaxpayer] =
    val EmployerReference(ton, tor) = req.employerReference.get

    cache
      .findBy(ton, tor)
      .flatMap {
        case Some(cisTaxpayer) => Future.successful(cisTaxpayer)
        case None              =>
          logger.info(s"Cache miss for scheme <$ton/$tor>; fetching from backend.")
          fetchFromBackend
      }
      .recoverWith { ex =>
        logger.warn(s"Falling back to connector; failed to fetch scheme <$ton/$tor> from cache.")
        fetchFromBackend
      }

  def findForAgent(cisId: String)(using IdentifierRequest[?]): Future[Option[CisTaxpayer]] =
    cache
      .findBy(cisId)
      .flatMap {
        case Some(cisTaxpayer) =>
          connector
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

  private def fetchFromBackend(using req: IdentifierRequest[?]) =
    for cisTaxpayer <- connector.getCisTaxpayer() yield
      cache
        .insert(List(cisTaxpayer))
        .andThen { case Failure(ex) => logger.warn(s"Fetched scheme for ${req.user} but failed to cache:", ex) }

      cisTaxpayer

  private def findInClientList(cisId: String)(using req: IdentifierRequest[?]) =
    for clientList <- connector.getAllClients yield
      cache
        .insert(clientList) // Run this asynchronously to avoid blocking user flow if cache fails
        .andThen { case Failure(ex) =>
          logger.warn(s"Fetched client list for ${req.user} but failed to cache:", ex)
        }

      clientList.find(_.uniqueId == cisId)
}
