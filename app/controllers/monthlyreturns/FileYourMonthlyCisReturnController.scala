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

package controllers.monthlyreturns

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import models.UserAnswers
import models.agent.AgentClientData
import models.requests.OptionalDataRequest
import pages.monthlyreturns.CisIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.FileYourMonthlyCisReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class FileYourMonthlyCisReturnController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  view: FileYourMonthlyCisReturnView,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val instanceIdOpt = request.getQueryString("instanceId")
    getAgentClient(request).flatMap(clientTaxOfficeNumberTaxOfficeReference =>
      handleRequest(instanceIdOpt, clientTaxOfficeNumberTaxOfficeReference)
    )
  }

  private def handleRequest(
    instanceIdOpt: Option[String],
    clientTaxOfficeNumberTaxOfficeReference: Option[(String, String)]
  )(implicit
    request: OptionalDataRequest[AnyContent]
  ): Future[Result] =
    if (!request.isAgent) {
      instanceIdOpt match {
        case Some(instanceId) => storeInstanceId(instanceId).map(_ => Ok(view()))
        case None             => ensureUserAnswersExists().map(_ => Ok(view()))
      }
    } else {
      (instanceIdOpt, clientTaxOfficeNumberTaxOfficeReference) match {
        case (None, _) =>
          logger.warn(s"[FileYourMonthlyCisReturnController] Missing instanceId for agent request")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case (Some(_), None) =>
          logger.warn(s"[FileYourMonthlyCisReturnController] Missing client tax office number tax office reference")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case (Some(instanceId), Some((taxOfficeNumber, taxOfficeReference))) =>
          monthlyReturnService
            .hasClient(taxOfficeNumber, taxOfficeReference)
            .flatMap {
              case true  => storeInstanceId(instanceId).map(_ => Ok(view()))
              case false =>
                logger.warn(
                  s"[FileYourMonthlyCisReturnController] hasClient = false for " +
                    s"taxOfficeNumber: $taxOfficeNumber, taxOfficeReference: $taxOfficeReference"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
            .recover { case NonFatal(e) =>
              logger.error(s"[FileYourMonthlyCisReturnController] hasClient check failed ${e.getMessage}", e)
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
      }
    }

  private def getAgentClient(implicit
    request: OptionalDataRequest[_],
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Option[(String, String)]] =
    if (request.isAgent) {
      monthlyReturnService.getAgentClient(request.userId).map {
        case Some(data) => Some((data.taxOfficeNumber, data.taxOfficeReference))
        case _          => None
      }
    } else Future.successful(None)

  private def ensureUserAnswersExists()(implicit request: OptionalDataRequest[_]): Future[Unit] =
    request.userAnswers match {
      case Some(_) => Future.successful(())
      case None    =>
        val ua = UserAnswers(request.userId)
        sessionRepository.set(ua).map(_ => ())
    }

  private def storeInstanceId(instanceId: String)(implicit request: OptionalDataRequest[_]): Future[Unit] =
    val ua0 = request.userAnswers.getOrElse(UserAnswers(request.userId))
    for {
      updated <- Future.fromTry(ua0.set(CisIdPage, instanceId))
      _       <- sessionRepository.set(updated)
    } yield ()
}
