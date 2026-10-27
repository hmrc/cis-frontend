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
import forms.monthlyreturns.SubmitInactivityRequestFormProvider
import models.{JourneyScope, Mode}
import models.requests.CisPath
import navigation.Navigator
import pages.monthlyreturns.SubmitInactivityRequestPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmitInactivityRequestView

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmitInactivityRequestController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  formProvider: SubmitInactivityRequestFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SubmitInactivityRequestView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>

      val preparedForm = request.userAnswers.get(SubmitInactivityRequestPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmitInactivityRequestPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(SubmitInactivityRequestPage, mode, updatedAnswers))
        )
    }

  // DTR-9431: GET for UUID-scoped nil return journey
  def onPageLoadWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)) { implicit request =>

      val id = JourneyScope.subJourneyId(cisPath, flowId)

      val preparedForm = request.userAnswers.get(SubmitInactivityRequestPage(id)) match {
        case Some(value) => form.fill(value)
        case None        => form
      }

      Ok(
        view(
          preparedForm,
          mode,
          Some(routes.SubmitInactivityRequestController.onSubmitWithId(cisPath, flowId, mode))
        )
      )
    }

  // DTR-9431: POST for UUID-scoped nil return journey
  def onSubmitWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>

      val id = JourneyScope.subJourneyId(cisPath, flowId)

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  mode,
                  Some(routes.SubmitInactivityRequestController.onSubmitWithId(cisPath, flowId, mode))
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                                  request.userAnswers.set(SubmitInactivityRequestPage(id), value)
                                )
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(cisPath, flowId, SubmitInactivityRequestPage, mode, updatedAnswers)
            )
        )
    }
}
