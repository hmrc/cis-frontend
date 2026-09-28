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

import com.google.inject.{Inject, Singleton}
import config.FrontendAppConfig
import models.requests.{IdentifierRequest, SchemeAccessRequest}
import models.{SimpleCisTaxpayer, UserAnswers}
import play.api.Logging
import play.api.http.Status
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionRefiner, Result, Results}
import repositories.SessionRepository
import services.{CisTaxpayerService, FormpRdsReconcileService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import views.html.PageNotFoundView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class AccessSchemeAction @Inject() (
  val messagesApi: MessagesApi,
  config: FrontendAppConfig,
  cisTaxpayerService: CisTaxpayerService,
  sessionRepository: SessionRepository,
  formpRdsReconcileService: FormpRdsReconcileService,
  notFoundView: PageNotFoundView
)(using ec: ExecutionContext)
    extends Results
    with Status
    with I18nSupport
    with Logging
    with FrontendHeaderCarrierProvider {
  import AccessSchemeAction.WILDCARD

  /** @param cisPath
    *   For Agents, this is equal to the CIS ID, allowing them work with multiple clients in different tabs. For
    *   Organisations, this takes a wildcard value to indicate that the CIS ID is deduced from the session. This
    *   wildcard pattern is taken from the Fitbit API.
    */
  def apply(cisPath: String): ActionRefiner[IdentifierRequest, SchemeAccessRequest] =
    new ActionRefiner[IdentifierRequest, SchemeAccessRequest] {
      protected val executionContext: ExecutionContext = ec

      protected def refine[A](req: IdentifierRequest[A]): Future[Either[Result, SchemeAccessRequest[A]]] =
        given IdentifierRequest[A] = req

        if req.isAgent then
          if cisPath == WILDCARD then
            logger.info(s"${req.agentInfo} tried to access wildcard URI ${req.uri}; redirecting to landing page.")
            Future.successful(Left(Redirect(config.constructionIndustryAgentAccountUrl)))
          else
            cisTaxpayerService
              .findByCisId(cisPath)
              .flatMap {
                case Some(cisTaxpayer) => buildRequest(cisTaxpayer, userAnswersId = s"${req.userId}/$cisPath")
                case None              =>
                  logger.info(s"${req.agentInfo} tried to access ${req.uri}.")
                  Future.successful(Left(NotFound(notFoundView())))
              }
        else if cisPath == WILDCARD then
          cisTaxpayerService.findBySession.flatMap(cisTaxpayer => buildRequest(cisTaxpayer, req.userId))
        else if cisPath.forall(_.isLetterOrDigit) then
          // The alphanumeric check above ensures we only do the string replacement below when no URL encoding is used
          logger.info(s"Organisation user tried to access ${req.uri}; replacing scheme ID with wildcard.")
          val wildcardUri = req.uri.replace(s"/$cisPath/", s"/$WILDCARD/")
          Future.successful(Left(Redirect(wildcardUri)))
        else
          logger.info(s"Organisation user tried to access ${req.uri} with invalid scheme ID; returning 404.")
          Future.successful(Left(NotFound(notFoundView())))

      /** @param userAnswersId
        *   For an Organisation user, this is just the user ID because they only have access to 1 scheme. For an Agent
        *   user, this is the user ID and scheme ID separated by a forward slash, allowing the Agent to manage multiple
        *   clients in parallel.
        */
      private def buildRequest[A](cisTaxpayer: SimpleCisTaxpayer, userAnswersId: String)(using IdentifierRequest[A]) =
        reconcileFormPAndRds(cisTaxpayer).flatMap {
          case Some(failureResult) => Future.successful(Left(failureResult))
          case None                =>
            for
              uaOpt <- sessionRepository.get(userAnswersId)
              ua     = uaOpt getOrElse UserAnswers(userAnswersId)
            yield Right(new SchemeAccessRequest(cisPath, cisTaxpayer, ua))
        }
    }

  private def reconcileFormPAndRds(cisTaxpayer: SimpleCisTaxpayer)(using HeaderCarrier) =
    formpRdsReconcileService
      .reconcile(cisTaxpayer.uniqueId, cisTaxpayer.taxOfficeNumber, cisTaxpayer.taxOfficeRef)
      .map(_ => None)
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
          logger.warn(
            s"[FileYourMonthlyCisReturnController] Contractor known facts missing in RDS (status ${e.statusCode})"
          )
          Some(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()))
        case NonFatal(e)                                                                                  =>
          logger.error(
            s"[FileYourMonthlyCisReturnController] FormP/RDS reconciliation failed: ${e.getMessage}",
            e
          )
          Some(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
}
object AccessSchemeAction {
  val WILDCARD = "-"
}
