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

import controllers.actions.*
import models.Mode
import models.monthlyreturns.Declaration
import models.monthlyreturns.Declaration.Confirmed
import models.requests.CisPath
import navigation.Navigator
import pages.monthlyreturns.DeclarationPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.DeclarationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeclarationController @Inject() (
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  resolveScheme: SchemeAction,
  getJourney: MonthlyReturnAction,
  val controllerComponents: MessagesControllerComponents,
  view: DeclarationView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(cisPath: CisPath, mode: Mode): Action[AnyContent] = (identify andThen resolveScheme(cisPath)) {
    implicit request =>
      Ok(view(cisPath, mode))
  }

  def onSubmit(cisPath: CisPath, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney).async { implicit request =>
      for {
        updatedAnswers <- Future.fromTry(request.userAnswers.set(DeclarationPage, Set(Confirmed)))
        _              <- sessionRepository.set(updatedAnswers)
      } yield Redirect(navigator.nextPage(DeclarationPage, mode, updatedAnswers))
    }
}
