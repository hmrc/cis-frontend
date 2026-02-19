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
import pages.monthlyreturns.NilReturnStatusPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.govuk.summarylist.*
import viewmodels.checkAnswers.monthlyreturns.*
import views.html.nilreturns.CheckYourAnswersView
import play.api.Logging

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  monthlyReturnService: MonthlyReturnService,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>

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

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    request.userAnswers.get(NilReturnStatusPage) match {
      case None =>
        logger.warn(
          "[CheckYourAnswersController] C6 submit without FormP record (missing NilReturnStatusPage); redirecting to journey recovery"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(_) =>
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        monthlyReturnService
          .updateNilMonthlyReturn(request.userAnswers)
          .map { _ =>
            logger.info(
              "[CheckYourAnswersController] Updated FormP monthly nil return confirmation/nil flags; redirecting to submission"
            )
            Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoad())
          }
          .recover { case ex =>
            logger.error(
              s"[CheckYourAnswersController] Failed to update FormP monthly nil return: ${ex.getMessage}",
              ex
            )
            Redirect(controllers.routes.SystemErrorController.onPageLoad())
          }
    }
  }
}
