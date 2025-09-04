/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions._
import forms.monthlyreturns.DeclarationFormProvider
import javax.inject.Inject
import models.Mode
import models.monthlyreturns.Declaration
import navigation.Navigator
import pages.monthlyreturns.{DateConfirmNilPaymentsPage, DeclarationPage}
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeFormats.dateTimeFormat
import views.html.monthlyreturns.DeclarationView

import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DeclarationFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: DeclarationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(DeclarationPage) match {
      case None        => form
      case Some(value) => form.fill(value.head)
    }

    val formattedDate = request.userAnswers
      .get(DateConfirmNilPaymentsPage)
      .map { date =>
        implicit val lang: Lang = messagesApi.preferred(request).lang
        date.format(dateTimeFormat())
      }
      .getOrElse("")

    Ok(view(preparedForm, mode, formattedDate))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            val formattedDate = request.userAnswers
              .get(DateConfirmNilPaymentsPage)
              .map { date =>
                implicit val lang: Lang = messagesApi.preferred(request).lang
                date.format(dateTimeFormat())
              }
              .getOrElse("")
            Future.successful(BadRequest(view(formWithErrors, mode, formattedDate)))
          },
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(DeclarationPage, Set(value)))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(DeclarationPage, mode, updatedAnswers))
        )
  }
}
