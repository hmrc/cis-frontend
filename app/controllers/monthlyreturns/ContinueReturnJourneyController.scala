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

package controllers.monthlyreturns

import controllers.actions.*
import models.{NormalMode, UserAnswers}
import models.requests.{GetMonthlyReturnForEditRequest, IdentifierRequest}

import javax.inject.Inject
import navigation.Navigator
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.DateConfirmPaymentsPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class ContinueReturnJourneyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def continueReturnJourney: Action[AnyContent] =
    identify.async { implicit request =>
      buildEditRequest(request) match {
        case Left(error) =>
          logger.warn(s"[continueReturnJourney] Invalid edit request: $error")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Right(editRequest) =>
          monthlyReturnService
            .populateUserAnswersForContinueJourney(
              UserAnswers(request.userId),
              editRequest
            )
            .flatMap {
              case Left(error) =>
                logger.warn(
                  s"[continueReturnJourney] Failed to populate user answers: $error for request: $editRequest"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

              case Right(updatedUserAnswers) =>
                populateAgentClientDataIfRequired(updatedUserAnswers)
                  .flatMap { finalUserAnswers =>
                    sessionRepository.set(finalUserAnswers).map { _ =>
                      Redirect(navigator.nextPage(DateConfirmPaymentsPage, NormalMode, updatedUserAnswers))
                    }
                  }
                  .recover { case ex =>
                    logger.warn(
                      s"[continueReturnJourney] Error populating agent client data for request: $editRequest, error: ${ex.getMessage}"
                    )
                    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  }
            }
      }
    }

  private def buildEditRequest(request: Request[AnyContent]): Either[String, GetMonthlyReturnForEditRequest] =
    for {
      instanceId <- request.getQueryString("instanceId").filter(_.nonEmpty).toRight("Missing instance ID")
      taxYear    <- request.getQueryString("taxYear").filter(_.nonEmpty).toRight("Missing tax year")
      taxMonth   <- request.getQueryString("taxMonth").filter(_.nonEmpty).toRight("Missing tax month")
    } yield GetMonthlyReturnForEditRequest(
      instanceId = instanceId,
      taxYear = taxYear.toInt,
      taxMonth = taxMonth.toInt
    )

  private def populateAgentClientDataIfRequired(
    ua: UserAnswers
  )(implicit request: IdentifierRequest[AnyContent], hc: HeaderCarrier): Future[UserAnswers] =
    if (!request.isAgent) {
      Future.successful(ua)
    } else {
      monthlyReturnService.getAgentClient(request.userId).flatMap {
        case Some(agentData) =>
          monthlyReturnService
            .hasClient(agentData.taxOfficeNumber, agentData.taxOfficeReference)
            .flatMap {
              case true =>
                Future.fromTry(ua.set(AgentClientDataPage, agentData))

              case false =>
                logger.warn(
                  s"[ContinueReturnJourneyController] Agent ${request.userId} does not have a client with " +
                    s"taxOfficeNumber: ${agentData.taxOfficeNumber}, taxOfficeReference: ${agentData.taxOfficeReference}"
                )
                Future.failed(new RuntimeException("Agent no longer authorised for client"))
            }

        case None =>
          logger.warn(
            s"[ContinueReturnJourneyController] Missing AgentClientData for agent user ${request.userId}"
          )
          Future.failed(new RuntimeException("Agent data not found"))
      }
    }
}
