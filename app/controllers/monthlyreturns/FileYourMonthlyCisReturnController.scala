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

import controllers.actions.{AccessSchemeAction, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.{EmployerReference, NormalMode, ReturnType, SimpleCisTaxpayer, UserAnswers}
import models.agent.AgentClientData
import models.requests.{DataRequest, OptionalDataRequest, SchemeAccessRequest}
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{CisIdPage, ContractorNamePage, ReturnTypePage}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, PRECONDITION_FAILED}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import services.{FormpRdsReconcileService, MonthlyReturnService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TypeUtils.toFuture
import views.html.monthlyreturns.FileYourMonthlyCisReturnView
import views.html.monthlyreturns.FileYourNilReturnView
import utils.UserAnswerUtils.*

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class FileYourMonthlyCisReturnController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  monthlyReturnView: FileYourMonthlyCisReturnView,
  nilReturnView: FileYourNilReturnView,
  identify: IdentifierAction,
  accessScheme: AccessSchemeAction,
  sessionRepository: SessionRepository,
  monthlyReturnService: MonthlyReturnService,
  formpRdsReconcileService: FormpRdsReconcileService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def startMonthlyReturn(cisId: String): Action[AnyContent] =
    (identify andThen accessScheme(cisId)).async { implicit request =>
      startReturn(ReturnType.MonthlyStandardReturn)(monthlyReturnView())
    }

  def startNilReturn(cisId: String): Action[AnyContent] =
    (identify andThen accessScheme(cisId)).async { implicit request =>
      startReturn(ReturnType.MonthlyNilReturn)(nilReturnView())
    }

  def onSubmit(cisId: String, returnType: ReturnType): Action[AnyContent] =
    (identify andThen accessScheme(cisId)).async { implicit request =>
      (for {
        cleanAnswers <- request.userAnswers.clearMonthlyReturnJourney.toFuture
        _            <- sessionRepository.set(cleanAnswers)
      } yield Redirect(routes.DateConfirmPaymentsController.onPageLoad(NormalMode, Some(returnType))))
        .recover(_ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def startReturn(
    returnType: ReturnType
  )(render: => Html)(using request: SchemeAccessRequest[AnyContent]): Future[Result] =
    for
      updatedAnswers <- Future.fromTry(request.userAnswers.set(ReturnTypePage, returnType))
      _              <- sessionRepository.set(updatedAnswers)
    yield Ok(render)
}
