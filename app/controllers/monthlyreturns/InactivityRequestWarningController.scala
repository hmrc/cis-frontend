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
import models.{CheckMode, JourneyScope, Mode, NormalMode}
import pages.monthlyreturns.SubmitInactivityRequestPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.InactivityRequestWarningView

import java.util.UUID
import javax.inject.Inject

class InactivityRequestWarningController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  val controllerComponents: MessagesControllerComponents,
  view: InactivityRequestWarningView
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>
      val inactivityRequested = request.userAnswers.get(SubmitInactivityRequestPage).contains(true)
      if (!inactivityRequested) {
        logger.error(
          s"[InactivityRequestWarningController][onPageLoad] - SubmitInactivityRequestPage not true, mode=$mode"
        )
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      } else {
        val nextUrl = mode match {
          case CheckMode  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad().url
          case NormalMode => controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode).url
        }
        Ok(view(nextUrl))
      }
  }

  // Multi-tab: UUID-scoped inactivity request warning
  def onPageLoadWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)) { implicit request =>
      val id = JourneyScope.subJourneyId(cisPath, flowId)

      val inactivityRequested = request.userAnswers.get(SubmitInactivityRequestPage(id)).contains(true)

      if (!inactivityRequested) {
        logger.error(
          s"[InactivityRequestWarningController][onPageLoadWithId] - SubmitInactivityRequestPage not true, mode=$mode"
        )
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      } else {
        val nextUrl = mode match {
          case CheckMode  =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(cisPath, flowId).url
          case NormalMode =>
            controllers.monthlyreturns.routes.ConfirmationByEmailController
              .onPageLoadWithId(cisPath, flowId, NormalMode)
              .url
        }
        Ok(view(nextUrl))
      }
    }
}
