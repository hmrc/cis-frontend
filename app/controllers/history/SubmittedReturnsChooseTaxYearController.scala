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

package controllers.history

import controllers.actions.*
import forms.history.SubmittedReturnsChooseTaxYearFormProvider
import models.Mode
import navigation.Navigator
import pages.history.SubmittedReturnsChooseTaxYearPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.history.SubmittedReturnsChooseTaxYearView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmittedReturnsChooseTaxYearController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: SubmittedReturnsChooseTaxYearFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SubmittedReturnsChooseTaxYearView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val taxYears: Seq[String] =
    Seq("2021 to 2202", "2022 to 2023", "2023 to 2024", "2024 to 2025")
  val form: Form[String]    = formProvider(taxYears)

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(SubmittedReturnsChooseTaxYearPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode, taxYears))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, taxYears))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(SubmittedReturnsChooseTaxYearPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(SubmittedReturnsChooseTaxYearPage, mode, updatedAnswers))
        )
  }
}
