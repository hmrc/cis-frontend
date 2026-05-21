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
import forms.amend.ConfirmCancelAmendmentYesNoFormProvider
import models.{NormalMode, UserAnswers}
import models.amend.DeleteUnsubmittedMonthlyReturnRequest
import models.requests.DataRequest
import navigation.Navigator
import pages.amend.ConfirmCancelAmendmentYesNoPage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.AmendMonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.ConfirmCancelAmendmentYesNoView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmCancelAmendmentYesNoController @Inject() (
  override val messagesApi: MessagesApi,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: ConfirmCancelAmendmentYesNoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmCancelAmendmentYesNoView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getMonthYear match {
      case Some(monthYear) =>
        val preparedForm = request.userAnswers.get(ConfirmCancelAmendmentYesNoPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }

        Ok(view(preparedForm, monthYear))
      case None            =>
        logger.error("[ConfirmCancelAmendmentYesNoController] monthYear missing")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    getMonthYear match {
      case Some(monthYear) =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, monthYear))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmCancelAmendmentYesNoPage, value))
                _              <- sessionRepository.set(updatedAnswers)
                result         <- value match {
                                    case true  => handleYes(updatedAnswers)
                                    case false => handleNo(updatedAnswers)
                                  }
              } yield Redirect(navigator.nextPage(ConfirmCancelAmendmentYesNoPage, NormalMode, updatedAnswers))
          )
      case None            =>
        logger.error("[ConfirmCancelAmendmentYesNoController] monthYear missing")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  private def handleYes(ua: UserAnswers)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val deleteRequest = DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers(ua)

    amendMonthlyReturnService
      .deleteUnsubmittedMonthlyReturn(deleteRequest)
      .map { _ =>
        Redirect(navigator.nextPage(ConfirmCancelAmendmentYesNoPage, NormalMode, ua))
      }
  }

  private def handleNo(ua: UserAnswers): Future[Result] =
    Future.successful(Redirect(navigator.nextPage(ConfirmCancelAmendmentYesNoPage, NormalMode, ua)))

  private def getMonthYear: Option[String] =
    Some("April 2026")
}
