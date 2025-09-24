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

import com.google.inject.Inject
import config.FrontendAppConfig
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.govuk.summarylist.*
import viewmodels.checkAnswers.monthlyreturns.{ConfirmEmailAddressSummary, DateConfirmNilPaymentsSummary, InactivityRequestSummary, PaymentsToSubcontractorsSummary, ReturnTypeSummary}
import views.html.monthlyreturns.CheckYourAnswersView

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  appConfig: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val returnDetailsList = SummaryListViewModel(
      rows = Seq(
        ReturnTypeSummary.row,
        DateConfirmNilPaymentsSummary.row(request.userAnswers),
        PaymentsToSubcontractorsSummary.row,
        InactivityRequestSummary.row(request.userAnswers)
      ).flatten
    )

    val emailList = SummaryListViewModel(
      rows = Seq(
        ConfirmEmailAddressSummary.row(request.userAnswers)
      ).flatten
    )

    Ok(view(returnDetailsList, emailList))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    if (appConfig.stubSendingEnabled)
      Redirect(stub.controllers.monthlyreturns.routes.StubSubmissionSendingController.onPageLoad())
    else
      Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoad())
  }
}
