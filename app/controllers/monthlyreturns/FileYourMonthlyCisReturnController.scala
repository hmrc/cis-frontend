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

import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{NormalMode, ReturnType, UserAnswers}
import models.agent.AgentClientData
import models.requests.OptionalDataRequest
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{CisIdPage, ReturnTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TypeUtils.toFuture
import views.html.monthlyreturns.FileYourMonthlyCisReturnView
import views.html.monthlyreturns.FileYourNilReturnView
import utils.UserAnswerUtils.*

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
  requireData: DataRequiredAction,
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

  def onSubmit(returnType: ReturnType): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      (for {
        cleanAnswers <- request.userAnswers.clearMonthlyReturnJourney.toFuture
        _            <- sessionRepository.set(cleanAnswers)
      } yield Redirect(routes.DateConfirmPaymentsController.onPageLoad(NormalMode, Some(returnType))))
        .recover(_ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def startReturn(
    returnType: ReturnType
  )(render: => Html)(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    val instanceIdOpt = request.getQueryString("instanceId")
    val userAnswer    = request.userAnswers.getOrElse(UserAnswers(request.userId))
    for {
      updatedAnswers <- Future.fromTry(userAnswer.set(ReturnTypePage, returnType))
      _              <- sessionRepository.set(updatedAnswers)
      agentData      <- getAgentClient(request)
      result         <- handleRequest(instanceIdOpt, agentData, updatedAnswers, render)
    } yield result
  }

  private def handleRequest(
    instanceIdOpt: Option[String],
    agentDataOpt: Option[AgentClientData],
    userAnswers: UserAnswers,
    render: => Html
  )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    if (!request.isAgent) {
      instanceIdOpt match {
        case Some(instanceId) => storeInstanceId(instanceId, userAnswers).map(_ => Ok(render))
        case None             =>
          monthlyReturnService
            .resolveAndStoreCisId(userAnswers, false)
            .map(_ => Ok(render))
            .recover { case NonFatal(ex) =>
              logger.error(
                s"[FileYourMonthlyCisReturnController] Failed to resolve CIS ID: ${ex.getMessage}",
                ex
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
      }
    } else {
      (instanceIdOpt, agentDataOpt) match {
        case (maybeInstanceId, Some(agentData)) =>
          val instanceId = maybeInstanceId.getOrElse(agentData.uniqueId)
          handleAgentFlow(instanceId, agentData, userAnswers, render)
        case (Some(_), None)                    =>
          logger.warn(s"[FileYourMonthlyCisReturnController] Missing AgentClientData")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case (None, None)                       =>
          logger.error(
            s"[FileYourMonthlyCisReturnController] Missing instanceId and AgentClientData"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def handleAgentFlow(
    instanceId: String,
    agentData: AgentClientData,
    userAnswers: UserAnswers,
    render: => Html
  )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    monthlyReturnService
      .hasClient(agentData.taxOfficeNumber, agentData.taxOfficeReference)
      .flatMap {
        case true  =>
          storeAgentClientData(agentData, userAnswers).flatMap(userAnswersWithAgentDate =>
            storeInstanceId(instanceId, userAnswersWithAgentDate).map(_ => Ok(render))
          )
        case false =>
          logger.warn(
            s"[FileYourMonthlyCisReturnController] hasClient = false for " +
              s"taxOfficeNumber: ${agentData.taxOfficeNumber}, taxOfficeReference: ${agentData.taxOfficeReference}"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
      .recover { case NonFatal(e) =>
        logger.error(s"[FileYourMonthlyCisReturnController] hasClient check failed ${e.getMessage}", e)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }

  private def getAgentClient(implicit
    request: OptionalDataRequest[_],
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Option[AgentClientData]] =
    if (request.isAgent) {
      monthlyReturnService.getAgentClient(request.userId)
    } else {
      Future.successful(None)
    }

  private def storeInstanceId(instanceId: String, userAnswers: UserAnswers): Future[Unit] =
    for {
      updated <- Future.fromTry(userAnswers.set(CisIdPage, instanceId))
      _       <- sessionRepository.set(updated)
    } yield ()

  private def storeAgentClientData(data: AgentClientData, ua: UserAnswers): Future[UserAnswers] =
    for {
      updatedUaWithCisId           <- Future.fromTry(ua.set(CisIdPage, data.uniqueId))
      updatedUaWithAgentClientData <- Future.fromTry(updatedUaWithCisId.set(AgentClientDataPage, data))
      _                            <- sessionRepository.set(updatedUaWithAgentClientData)
    } yield updatedUaWithAgentClientData

}
