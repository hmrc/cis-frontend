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

package controllers.actions

import config.FrontendAppConfig
import models.requests.{DataRequest, IdentifierRequest}
import pages.monthlyreturns.CisIdPage
import play.api.Logging
import play.api.mvc.{ActionFilter, Result, Results}
import services.CisTaxpayerService

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MultiTabAction @Inject() (
  appConfig: FrontendAppConfig,
  cisTaxpayerService: CisTaxpayerService
)(using ec: ExecutionContext)
    extends Logging {

  def forStart(cisId: String): ActionFilter[IdentifierRequest] =
    new ActionFilter[IdentifierRequest] {
      override protected def executionContext: ExecutionContext = ec

      override protected def filter[A](request: IdentifierRequest[A]): Future[Option[Result]] = {
        given IdentifierRequest[A] = request
        checkClientAndFeatureFlag(cisId)
      }
    }

  def forJourney(flowId: UUID): ActionFilter[DataRequest] =
    new ActionFilter[DataRequest] {
      override protected def executionContext: ExecutionContext                         = ec
      override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
        request.userAnswers.get(CisIdPage(flowId.toString)) match {
          case Some(cisId) =>
            given IdentifierRequest[A] = IdentifierRequest(
              request.request,
              request.userId,
              request.employerReference,
              request.agentReference,
              request.isAgent,
              request.agentCode
            )
            checkClientAndFeatureFlag(cisId)

          case None =>
            Future.successful(Some(journeyRecovery))
        }
    }

  private def checkClientAndFeatureFlag(cisId: String)(using IdentifierRequest[?]): Future[Option[Result]] =
    isAuthorisedForCisId(cisId)
      .map {
        case false =>
          Some(journeyRecovery)

        case true if !appConfig.multiTabEnabled =>
          Some(Results.Redirect(appConfig.returnsLandingPageUrl(cisId, None)))

        case true =>
          None
      }
      .recover { case NonFatal(ex) =>
        logger.error(s"[MultiTabAction] Failed to validate CIS client: ${ex.getMessage}", ex)
        Some(journeyRecovery)
      }

  private def isAuthorisedForCisId(cisId: String)(using request: IdentifierRequest[?]): Future[Boolean] =
    if cisId.trim.isEmpty then Future.successful(false)
    else if request.isAgent then
      cisTaxpayerService
        .findForAgent(cisId)
        .map(_.exists(_.uniqueId == cisId))
    else if request.employerReference.isEmpty then Future.successful(false)
    else
      cisTaxpayerService.findForOrganisation
        .map(_.uniqueId == cisId)

  private def journeyRecovery: Result =
    Results.Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
}
