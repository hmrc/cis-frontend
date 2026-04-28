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
import models.monthlyreturns.ContinueReturnJourneyQueryParams
import models.{NormalMode, UserAnswers}
import models.requests.GetMonthlyReturnForEditRequest

import javax.inject.Inject
import navigation.Navigator
import pages.monthlyreturns.DateConfirmPaymentsPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
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

  def continueReturnJourney(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] =
    identify.async { implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth
      )

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
            monthlyReturnService
              .populateAgentClientDataIfRequired(
                ua = updatedUserAnswers,
                userId = request.userId,
                isAgent = request.isAgent
              )
              .flatMap { finalUserAnswers =>
                sessionRepository.set(finalUserAnswers).map { _ =>
                  Redirect(navigator.nextPage(DateConfirmPaymentsPage, NormalMode, finalUserAnswers))
                }
              }
              .recover { case ex =>
                logger.warn(
                  s"[continueReturnJourney] Failed to populate agent client data for request: $editRequest, error: ${ex.getMessage}"
                )
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
        }
    }
}
