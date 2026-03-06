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
import models.{ReturnType, UserAnswers}
import models.agent.AgentClientData
import models.requests.OptionalDataRequest
import pages.monthlyreturns.{CisIdPage, ReturnTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.FileYourMonthlyCisReturnView
import views.html.monthlyreturns.FileYourNilReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class FileYourMonthlyCisReturnController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  monthlyReturnView: FileYourMonthlyCisReturnView,
  nilReturnView: FileYourNilReturnView,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def startMonthlyReturn(): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      startReturn(ReturnType.MonthlyStandardReturn)(monthlyReturnView())
    }

  def startNilReturn(): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      startReturn(ReturnType.MonthlyNilReturn)(nilReturnView())
    }

  private def startReturn(
    returnType: ReturnType
  )(render: => Html)(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    val instanceIdOpt = request.getQueryString("instanceId")
    val userAnswer    = request.userAnswers.getOrElse(UserAnswers(request.userId))
    for {
      updatedAnswers <- Future.fromTry(userAnswer.set(ReturnTypePage, returnType))
      _              <- sessionRepository.set(updatedAnswers)
      clientInfoOpt  <- getAgentClient(request)
      result         <- handleRequest(instanceIdOpt, clientInfoOpt, updatedAnswers, render)
    } yield result
  }

  private def handleRequest(
    instanceIdOpt: Option[String],
    clientTaxOfficeNumberTaxOfficeReference: Option[(String, String)],
    userAnswers: UserAnswers,
    render: => Html
  )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    if (!request.isAgent) {
      instanceIdOpt match {
        case Some(instanceId) => storeInstanceId(instanceId, userAnswers).map(_ => Ok(render))
        case None             => Future.successful(Ok(render))
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
              case true  => storeInstanceId(instanceId, userAnswers).map(_ => Ok(render))
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
    } else { Future.successful(None) }

  private def storeInstanceId(instanceId: String, userAnswers: UserAnswers): Future[Unit] =
    for {
      updated <- Future.fromTry(userAnswers.set(CisIdPage, instanceId))
      _       <- sessionRepository.set(updated)
    } yield ()
}
