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
import models.monthlyreturns.DeleteMonthlyReturnItemRequest
import models.{Mode, UserAnswers}
import pages.monthlyreturns.{CisIdPage, ConfirmSubcontractorRemovalPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.ConfirmSubcontractorRemovalView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmSubcontractorRemovalController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ConfirmSubcontractorRemovalFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmSubcontractorRemovalView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode, index: Int): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

        case Some(subcontractor) =>
          val preparedForm =
            request.userAnswers.get(ConfirmSubcontractorRemovalPage(index)) match {
              case None        => form
              case Some(value) => form.fill(value)
            }

          Ok(view(preparedForm, mode, subcontractor.name, index))
      }
  }

  def onSubmit(mode: Mode, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(subcontractor) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, subcontractor.name, index))),
              confirmRemove =>
                for {
                  updatedAnswers1 <-
                    Future.fromTry(request.userAnswers.set(ConfirmSubcontractorRemovalPage(index), confirmRemove))

                  result <-
                    if (!confirmRemove) {
                      sessionRepository.set(updatedAnswers1).map { _ =>
                        Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(mode))
                      }
                    } else {
                      buildDeletePayload(updatedAnswers1, index) match {
                        case None =>
                          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

                        case Some(payload) =>
                          monthlyReturnService
                            .deleteMonthlyReturnItem(payload)
                            .flatMap { _ =>
                              for {
                                updatedAnswers2 <-
                                  Future.fromTry(updatedAnswers1.remove(SelectedSubcontractorPage(index)))
                                _               <- sessionRepository.set(updatedAnswers2)
                              } yield Redirect(
                                controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
                              )
                            }
                            .recover { case e =>
                              Redirect(controllers.routes.SystemErrorController.onPageLoad())
                            }
                      }
                    }
                } yield result
            )
      }
    }

  private def buildDeletePayload(ua: UserAnswers, index: Int): Option[DeleteMonthlyReturnItemRequest] =
    for {
      cisId         <- ua.get(CisIdPage)
      monthYear     <- ua.get(DateConfirmPaymentsPage)
      subcontractor <- ua.get(SelectedSubcontractorPage(index))
    } yield DeleteMonthlyReturnItemRequest(
      instanceId = cisId,
      taxYear = monthYear.getYear,
      taxMonth = monthYear.getMonthValue,
      subcontractorId = subcontractor.id
    )

}
