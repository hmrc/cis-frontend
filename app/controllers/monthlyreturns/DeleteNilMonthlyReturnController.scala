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
import forms.monthlyreturns.DeleteNilMonthlyReturnFormProvider

import javax.inject.Inject
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.monthlyreturns.{DateConfirmPaymentsPage, DeleteNilMonthlyReturnPage}
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.DeleteNilMonthlyReturnView

import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.concurrent.{ExecutionContext, Future}

class DeleteNilMonthlyReturnController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DeleteNilMonthlyReturnFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DeleteNilMonthlyReturnView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val preparedForm = request.userAnswers.get(DeleteNilMonthlyReturnPage) match {
      case None => form
      case Some(value) => form.fill(value)
    }
    val monthYear = getPeriodEnd
    Ok(view(preparedForm, monthYear, mode))
  }


  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val monthYear = getPeriodEnd
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, monthYear, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DeleteNilMonthlyReturnPage, value))
              _ <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(DeleteNilMonthlyReturnPage, mode, updatedAnswers))
        )
  }

  private def getPeriodEnd(implicit request: DataRequest[_]): String = {
    val locale: Locale = messages.lang.locale
    val dmyFmt = DateTimeFormatter
      .ofPattern("MMMM uuuu")
      .withLocale(locale)
    request.userAnswers
      .get(DateConfirmPaymentsPage)
      .map(_.format(dmyFmt))
      .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
  }
}