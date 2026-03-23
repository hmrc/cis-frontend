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
import forms.monthlyreturns.DeleteMonthlyReturnFormProvider
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.monthlyreturns.{DateConfirmPaymentsPage, DeleteMonthlyReturnPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.DeleteMonthlyReturnView

import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class DeleteMonthlyReturnController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DeleteMonthlyReturnFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DeleteMonthlyReturnView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    getPeriodEnd match {
      case Some(monthYear) =>
        val preparedForm = request.userAnswers.get(DeleteMonthlyReturnPage) match {
          case None        => form
          case Some(value) => form.fill(value)
        }
        Ok(view(preparedForm, monthYear, mode))

      case None =>
        logger.error("[DeleteNilMonthlyReturn] dateConfirmPayments missing")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      getPeriodEnd match {
        case Some(monthYear) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, monthYear, mode))),
              value =>
                for {
                  updatedAnswers <- Future.fromTry(request.userAnswers.set(DeleteMonthlyReturnPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                } yield Redirect(navigator.nextPage(DeleteMonthlyReturnPage, mode, updatedAnswers))
            )

        case None =>
          logger.error("[DeleteNilMonthlyReturn] dateConfirmPayments missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def getPeriodEnd(implicit request: DataRequest[_]): Option[String] = {
    val messages = messagesApi.preferred(request)
    val fmt      = DateTimeFormatter
      .ofPattern("MMMM uuuu")
      .withLocale(messages.lang.locale)

    request.userAnswers
      .get(DateConfirmPaymentsPage)
      .map(_.format(fmt))
  }
}
