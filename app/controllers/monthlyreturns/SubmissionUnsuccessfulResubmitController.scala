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
import controllers.helpers.SubmissionViewDataSupport
import models.requests.CisPath
import pages.monthlyreturns.CisIdPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmissionUnsuccessfulResubmitView

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubmissionUnsuccessfulResubmitController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  val controllerComponents: MessagesControllerComponents,
  view: SubmissionUnsuccessfulResubmitView,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with SubmissionViewDataSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    val cisId = required(
      request.userAnswers.get(CisIdPage),
      "[SubmissionUnsuccessfulResubmit] cisId missing from userAnswers"
    )

    monthlyReturnService
      .completeSubmissionJourney(request.userAnswers)
      .map(_ => Ok(view(cisId, routes.ManageCisReturnController.onExit().url)))
  }

  // DTR-9431: Complete the UUID-scoped submission journey.
  def onPageLoadWithJourneyId(cisPath: CisPath, uuid: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)).async { implicit request =>
      monthlyReturnService
        .completeSubmissionJourney(request.userAnswers)
        .map(_ => Ok(view(request.cisId, routes.ManageCisReturnController.onExitWithJourneyId(cisPath, uuid).url)))
    }
}
