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

import controllers.actions.{IdentifierAction, MonthlyReturnAction, ReconcileFormPAndRdsAction, SchemeAction}
import models.requests.{CisPath, JourneyRequest}
import models.{NormalMode, ReturnType}
import pages.monthlyreturns.ReturnTypePage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TypeUtils.toFuture
import utils.UserAnswerUtils.*
import views.html.monthlyreturns.{FileYourMonthlyCisReturnView, FileYourNilReturnView}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FileYourMonthlyCisReturnController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  monthlyReturnView: FileYourMonthlyCisReturnView,
  nilReturnView: FileYourNilReturnView,
  identify: IdentifierAction,
  resolveScheme: SchemeAction,
  reconcileFormPAndRDS: ReconcileFormPAndRdsAction,
  getJourney: MonthlyReturnAction,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def startMonthlyReturn(cisPath: CisPath): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen reconcileFormPAndRDS andThen getJourney).async {
      implicit request =>
        startReturn(ReturnType.MonthlyStandardReturn)(monthlyReturnView(cisPath))
    }

  def startNilReturn(cisPath: CisPath): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen reconcileFormPAndRDS andThen getJourney).async {
      implicit request =>
        startReturn(ReturnType.MonthlyNilReturn)(nilReturnView(cisPath))
    }

  def onSubmit(cisPath: CisPath, returnType: ReturnType): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney).async { implicit request =>
      (for {
        cleanAnswers <- request.userAnswers.clearMonthlyReturnJourney.toFuture
        _            <- sessionRepository.set(cleanAnswers)
      } yield Redirect(routes.DateConfirmPaymentsController.onPageLoad(cisPath, NormalMode, Some(returnType))))
        .recover(_ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def startReturn(returnType: ReturnType)(render: => Html)(using request: JourneyRequest[?]): Future[Result] =
    for
      updatedAnswers <- Future.fromTry(request.userAnswers.set(ReturnTypePage, returnType))
      _              <- sessionRepository.set(updatedAnswers)
    yield Ok(render)
}
