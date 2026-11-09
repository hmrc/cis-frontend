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
import forms.monthlyreturns.PaymentDetailsConfirmationFormProvider

import javax.inject.Inject
import java.util.UUID
import models.{Mode, UserAnswers}
import models.ReturnType.*
import navigation.Navigator
import pages.monthlyreturns.{PaymentDetailsConfirmationPage, ReturnTypePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.PaymentDetailsConfirmationView

import scala.concurrent.{ExecutionContext, Future}

class PaymentDetailsConfirmationController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  multiTabAction: MultiTabAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  formProvider: PaymentDetailsConfirmationFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PaymentDetailsConfirmationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>

      val preparedForm = request.userAnswers.get(PaymentDetailsConfirmationPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode, isAmendment(request.userAnswers)))
  }

  def onPageLoadWithId(flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)) { implicit request =>
      val subJourneyId = flowId.toString
      val formAction   =
        Some(controllers.monthlyreturns.routes.PaymentDetailsConfirmationController.onSubmitWithId(flowId, mode))
      val cancelAction =
        Some(controllers.monthlyreturns.routes.PaymentDetailsConfirmationController.onCancelAmendmentWithId(flowId))
      val preparedForm = request.userAnswers.get(PaymentDetailsConfirmationPage(subJourneyId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(
        view(
          preparedForm,
          mode,
          isAmendment(request.userAnswers),
          formAction,
          cancelAction
        )
      )
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, isAmendment(request.userAnswers)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(PaymentDetailsConfirmationPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(PaymentDetailsConfirmationPage, mode, updatedAnswers))
        )
    }

  def onSubmitWithId(flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val subJourneyId = flowId.toString
      val formAction   =
        Some(controllers.monthlyreturns.routes.PaymentDetailsConfirmationController.onSubmitWithId(flowId, mode))
      val cancelAction =
        Some(controllers.monthlyreturns.routes.PaymentDetailsConfirmationController.onCancelAmendmentWithId(flowId))

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  mode,
                  isAmendment(request.userAnswers),
                  formAction,
                  cancelAction
                )
              )
            ),
          value =>
            for {
              updatedAnswers <-
                Future.fromTry(request.userAnswers.set(PaymentDetailsConfirmationPage(subJourneyId), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(PaymentDetailsConfirmationPage(subJourneyId), mode, updatedAnswers))
        )
    }

  def onCancelAmendment(): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId) { implicit request =>
      Redirect(controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoad())
    }

  def onCancelAmendmentWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)) { implicit request =>
      Redirect(controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoadWithId(flowId))
    }

  private def isAmendment(ua: UserAnswers): Boolean =
    ua
      .get(ReturnTypePage)
      .getOrElse(throw new RuntimeException("ReturnTypePage not found in user answers")) match {
      case MonthlyAmendedNilReturn | MonthlyAmendedStandardReturn => true
      case _                                                      => false
    }

  private def isAmendmentWithId(flowId: UUID, ua: UserAnswers): Boolean = {
    val subJourneyId = flowId.toString
    ua
      .get(ReturnTypePage(subJourneyId))
      .getOrElse(throw new RuntimeException("ReturnTypePage not found in user answers")) match {
      case MonthlyAmendedNilReturn | MonthlyAmendedStandardReturn => true
      case _                                                      => false
    }
  }
}
