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
import forms.amend.AreYouSureYouWantToAmendYesNoFormProvider

import javax.inject.Inject
import models.Mode
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn}
import models.amend.AreYouSureYouWantToAmendYesNo.{No, Yes}
import models.amend.DeleteAllMonthlyReturnItemsRequest
import models.monthlyreturns.UpdateMonthlyReturnRequest
import pages.monthlyreturns.ReturnTypePage
import navigation.Navigator
import pages.amend.AreYouSureYouWantToAmendYesNoPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.AreYouSureYouWantToAmendYesNoView
import utils.TypeUtils.toFuture
import utils.UserAnswerUtils.*

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class AreYouSureYouWantToAmendYesNoController @Inject() (
  override val messagesApi: MessagesApi,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  monthlyReturnService: MonthlyReturnService,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  multiTabAction: MultiTabAction,
  formProvider: AreYouSureYouWantToAmendYesNoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: AreYouSureYouWantToAmendYesNoView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>

      val preparedForm = request.userAnswers.get(AreYouSureYouWantToAmendYesNoPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          {
            case Yes =>
              for {
                ua1            <- Future.fromTry(request.userAnswers.set(ReturnTypePage, MonthlyAmendedNilReturn))
                ua2            <- ua1.clearAmendedMonthlyStandardReturnJourney.toFuture
                updatedAnswers <- Future.fromTry(ua2.set(AreYouSureYouWantToAmendYesNoPage, Yes))
                _              <- sessionRepository.set(updatedAnswers)
                deleteRequest  <- toFuture(DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(updatedAnswers))
                _              <- amendMonthlyReturnService.deleteAllMonthlyReturnItems(deleteRequest)
                updateRequest  <- toFuture(UpdateMonthlyReturnRequest.fromUserAnswers(updatedAnswers))
                _              <- monthlyReturnService.updateMonthlyReturn(updateRequest)
              } yield Redirect(navigator.nextPage(AreYouSureYouWantToAmendYesNoPage, mode, updatedAnswers))

            case No =>
              for {
                ua1 <- Future.fromTry(request.userAnswers.set(AreYouSureYouWantToAmendYesNoPage, No))
                ua2 <- Future.fromTry(ua1.set(ReturnTypePage, MonthlyAmendedStandardReturn))
                _   <- sessionRepository.set(ua2)
              } yield Redirect(navigator.nextPage(AreYouSureYouWantToAmendYesNoPage, mode, ua2))
          }
        )
    }

  def onPageLoadWithId(flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)) { implicit request =>
      val id = flowId.toString

      val preparedForm = request.userAnswers.get(AreYouSureYouWantToAmendYesNoPage(id)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val formAction = Some(routes.AreYouSureYouWantToAmendYesNoController.onSubmitWithId(flowId))

      Ok(view(preparedForm, formAction))
    }

  def onSubmitWithId(flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val id                         = flowId.toString
      val formAction                 = Some(routes.AreYouSureYouWantToAmendYesNoController.onSubmitWithId(flowId))

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, formAction))),
          {
            case Yes =>
              for {
                ua1            <- Future.fromTry(request.userAnswers.set(ReturnTypePage(id), MonthlyAmendedNilReturn))
                ua2            <- ua1.clearAmendedMonthlyStandardReturnJourney(id).toFuture
                updatedAnswers <- Future.fromTry(ua2.set(AreYouSureYouWantToAmendYesNoPage(id), Yes))
                _              <- sessionRepository.set(updatedAnswers)
                deleteRequest  <- toFuture(DeleteAllMonthlyReturnItemsRequest.fromUserAnswers(updatedAnswers))
                _              <- amendMonthlyReturnService.deleteAllMonthlyReturnItems(deleteRequest)
                updateRequest  <- toFuture(UpdateMonthlyReturnRequest.fromUserAnswers(updatedAnswers))
                _              <- monthlyReturnService.updateMonthlyReturn(updateRequest)
              } yield Redirect(
                controllers.amend.routes.AreYouSureYouWantToAmendYesNoController.onPageLoadWithId(flowId)
              ) // TODO remove above redirect when next page is implemented
            // yield Redirect(navigator.nextPage(AreYouSureYouWantToAmendYesNoPage(id), mode, updatedAnswers))

            case No =>
              for {
                ua1 <- Future.fromTry(request.userAnswers.set(AreYouSureYouWantToAmendYesNoPage(id), No))
                ua2 <- Future.fromTry(ua1.set(ReturnTypePage(id), MonthlyAmendedStandardReturn))
                _   <- sessionRepository.set(ua2)
              } yield Redirect(
                controllers.amend.routes.AreYouSureYouWantToAmendYesNoController.onPageLoadWithId(flowId)
              ) // TODO remove above redirect when next page is implemented
            // yield Redirect(navigator.nextPage(AreYouSureYouWantToAmendYesNoPage(id), mode, ua2))
          }
        )
    }

  private def toFuture[A](either: Either[String, A]): Future[A] =
    either.fold(error => Future.failed(new RuntimeException(error)), Future.successful)
}
