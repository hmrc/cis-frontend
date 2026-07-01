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

package controllers.amend

import controllers.actions.*
import models.{NormalMode, UserAnswers}
import models.monthlyreturns.ContinueReturnJourneyQueryParams
import models.requests.GetMonthlyReturnForEditRequest

import javax.inject.Inject
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}

class ContinueAmendReturnJourneyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def continueAmendReturnJourney(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] =
    identify.async { implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth,
        isAmendment = true
      )

      monthlyReturnService
        .populateUserAnswersForContinueAmendJourney(
          UserAnswers(request.userId),
          editRequest
        )
        .flatMap {
          case Left(error) =>
            logger.warn(
              s"[continueAmendReturnJourney] Failed to populate user answers: $error for request: $editRequest"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Right(result) =>
            sessionRepository.set(result.userAnswers).map { _ =>
              (result.hasSubcontractors, queryParams.isOriginalNilReturn, result.isNilReturn) match {
                case (true, _, _)        =>
                  Redirect(
                    controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
                  )
                case (false, true, true) =>
                  Redirect(controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad())
                case _                   =>
                  Redirect(controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad())
              }
            }
        }
    }
}
