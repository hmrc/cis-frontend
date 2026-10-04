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
import models.requests.CisPath
import models.{CheckMode, Mode, NormalMode}
import pages.monthlyreturns.SubmitInactivityRequestPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.InactivityRequestWarningView

import javax.inject.Inject

class InactivityRequestWarningController @Inject() (
  identify: IdentifierAction,
  resolveScheme: SchemeAction,
  getJourney: MonthlyReturnAction,
  val controllerComponents: MessagesControllerComponents,
  view: InactivityRequestWarningView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(cisPath: CisPath, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney) { implicit request =>
      val inactivityRequested = request.userAnswers.get(SubmitInactivityRequestPage).contains(true)
      if (!inactivityRequested) {
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      } else {
        val nextUrl = mode match {
          case CheckMode  => routes.CheckYourAnswersController.onPageLoad(cisPath).url
          case NormalMode => routes.ConfirmationByEmailController.onPageLoad(cisPath, NormalMode).url
        }
        Ok(view(nextUrl))
      }
    }
}
