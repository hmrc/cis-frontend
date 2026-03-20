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
import forms.monthlyreturns.CostOfMaterialsFormProvider
import models.Mode
import navigation.Navigator
import pages.monthlyreturns.{SelectedSubcontractorMaterialCostsPage, SelectedSubcontractorPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.CostOfMaterialsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CostOfMaterialsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: CostOfMaterialsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: CostOfMaterialsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Option[BigDecimal]] = formProvider()

  def onPageLoad(mode: Mode, index: Int, returnTo: Option[String]): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None                => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(subcontractor) =>
          val preparedForm = request.userAnswers.get(SelectedSubcontractorMaterialCostsPage(index)) match {
            case None        => form
            case Some(value) => form.fill(Some(value))
          }

          Ok(view(preparedForm, mode, subcontractor.name, index, returnTo))
      }
    }

  def onSubmit(mode: Mode, index: Int, returnTo: Option[String]): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      def redirect(updatedAnswers: models.UserAnswers): Result =
        returnTo match {
          case Some("changeAnswers") =>
            Redirect(controllers.monthlyreturns.routes.ChangeAnswersTotalPaymentsController.onPageLoad(index))
          case _                     =>
            Redirect(navigator.nextPage(SelectedSubcontractorMaterialCostsPage(index), mode, updatedAnswers))
        }

      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(subcontractor) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, subcontractor.name, index, returnTo))),
              valueOpt => {
                val valueToPersist = valueOpt.getOrElse(BigDecimal(0))

                for {
                  updatedAnswers <-
                    Future.fromTry(
                      request.userAnswers.set(SelectedSubcontractorMaterialCostsPage(index), valueToPersist)
                    )
                  _              <- sessionRepository.set(updatedAnswers)
                } yield redirect(updatedAnswers)
              }
            )
      }
    }
}
