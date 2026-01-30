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
import forms.monthlyreturns.TotalTaxDeductedFormProvider
import models.Mode
import navigation.Navigator
import pages.monthlyreturns.TotalTaxDeductedPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.TotalTaxDeductedView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TotalTaxDeductedController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: TotalTaxDeductedFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TotalTaxDeductedView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(TotalTaxDeductedPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    val companyName = "TyneWear Ltd"

    Ok(view(preparedForm, mode, companyName))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val companyName = "TyneWear Ltd"
            Future.successful(BadRequest(view(formWithErrors, mode, companyName)))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(TotalTaxDeductedPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(TotalTaxDeductedPage, mode, updatedAnswers))
        )
  }
}
