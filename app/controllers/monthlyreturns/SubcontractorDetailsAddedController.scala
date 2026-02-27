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
import forms.monthlyreturns.SubcontractorDetailsAddedFormProvider
import models.{Mode, NormalMode}
import pages.monthlyreturns.{SelectedSubcontractorPage, SubcontractorDetailsAddedPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.SubcontractorDetailsAddedBuilder
import views.html.monthlyreturns.SubcontractorDetailsAddedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubcontractorDetailsAddedController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SubcontractorDetailsAddedFormProvider,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: SubcontractorDetailsAddedView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val ua = request.userAnswers
    SubcontractorDetailsAddedBuilder.build(ua) match {
      case Some(viewModel) =>
        val preparedForm =
          request.userAnswers.get(SubcontractorDetailsAddedPage) match {
            case Some(value) => form.fill(value)
            case None        => form
          }

        Ok(view(preparedForm, mode, viewModel))

      case None =>
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      SubcontractorDetailsAddedBuilder.build(request.userAnswers) match {
        case None =>
          Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))

        case Some(viewModel) =>
          val subcontractors = request.userAnswers.get(SelectedSubcontractorPage.all).getOrElse(Map())
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, viewModel))),
              answer =>
                val updatedUa =
                  request.userAnswers.set(SubcontractorDetailsAddedPage, answer).getOrElse(request.userAnswers)

                sessionRepository.set(updatedUa).map { _ =>
                  if (answer && viewModel.hasIncomplete) {
                    val withError =
                      form.withError("value", "monthlyreturns.subcontractorDetailsAdded.error.incomplete")
                    BadRequest(view(withError, mode, viewModel))
                  } else if (answer) {
                    Redirect(controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController.onPageLoad())
                  } else if (!answer && subcontractors.values.exists(!_.isComplete)) {
                    Redirect(
                      controllers.monthlyreturns.routes.AddSubcontractorDetailsController.onPageLoad(NormalMode)
                    )
                  } else {
                    Redirect(
                      controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
                    )
                  }
                }
            )
      }
  }
}
