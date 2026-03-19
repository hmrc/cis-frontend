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
import forms.monthlyreturns.DeleteAmendedNilMonthlyReturnFormProvider
import models.Mode
import models.requests.DataRequest
import navigation.Navigator
import pages.monthlyreturns.{DateConfirmPaymentsPage, DeleteAmendedNilMonthlyReturnPage}
import play.api.data.Form
import play.api.i18n.Lang.logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.DeleteAmendedNilMonthlyReturnView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeleteAmendedNilMonthlyReturnController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DeleteAmendedNilMonthlyReturnFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DeleteAmendedNilMonthlyReturnView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(DeleteAmendedNilMonthlyReturnPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }
    val taxPeriodEnd = getPeriodEnd
    Ok(view(preparedForm, taxPeriodEnd, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val taxPeriodEnd = getPeriodEnd
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxPeriodEnd, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DeleteAmendedNilMonthlyReturnPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(DeleteAmendedNilMonthlyReturnPage, mode, updatedAnswers))
        )
  }

  private def getPeriodEnd(implicit request: DataRequest[_]): String = {
    val dmyFmt = DateTimeFormatter.ofPattern("MMMM uuuu")
    request.userAnswers
      .get(DateConfirmPaymentsPage)
      .map(_.format(dmyFmt))
      .getOrElse {
        logger.error("[DeleteAmendedNilMonthlyReturn] taxPeriodEnd missing")
        throw new IllegalStateException("taxPeriodEnd missing")
      }
  }
}
