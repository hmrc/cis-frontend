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
import forms.monthlyreturns.ConfirmSubcontractorRemovalFormProvider
import models.{Mode, NormalMode}
import navigation.Navigator
import pages.monthlyreturns.{ConfirmSubcontractorRemovalPage, SelectedSubcontractorPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.ConfirmSubcontractorRemovalView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmSubcontractorRemovalController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ConfirmSubcontractorRemovalFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmSubcontractorRemovalView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(index: Int): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(SelectedSubcontractorPage(index)) match {
      case None                => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(subcontractor) => Ok(view(form, index, subcontractor.name))
    }
  }

  def onSubmit(index: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None                => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(subcontractor) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, index, subcontractor.name))),
              value =>
                if (value) {
                  val updatedSubcontractors = request.userAnswers
                    .get(SelectedSubcontractorPage.all)
                    .getOrElse(Map())
                    .filter(_._1 != index)
                    .values
                    .zipWithIndex
                    .map((sub, idx) => idx -> sub)
                    .toMap

                  for {
                    updatedAnswers <-
                      Future.fromTry(request.userAnswers.set(SelectedSubcontractorPage.all, updatedSubcontractors))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))
                } else {
                  Future.successful(
                    Redirect(routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))
                  )
                }
            )
      }
  }
}
