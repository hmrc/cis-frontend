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
import models.ReturnType
import models.monthlyreturns.UpdateMonthlyReturnRequest
import pages.monthlyreturns.ReturnTypePage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import services.submission.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.*
import viewmodels.govuk.summarylist.*
import views.html.monthlyreturns.CheckYourAnswersView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  monthlyReturnService: MonthlyReturnService,
  submissionService: SubmissionService,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>
      val returnTypeRows = ReturnTypeSummary.returnType(request.userAnswers) match {
        case ReturnType.MonthlyStandardReturn =>
          Seq(
            DateConfirmPaymentsSummary.row(request.userAnswers),
            EmploymentStatusDeclarationSummary.row(request.userAnswers),
            VerifiedStatusDeclarationSummary.row(request.userAnswers),
            SubmitInactivityRequestSummary.row(request.userAnswers)
          )
        case ReturnType.MonthlyNilReturn      =>
          Seq(
            DateConfirmNilPaymentsSummary.row(request.userAnswers),
            PaymentsToSubcontractorsSummary.row,
            SubmitInactivityRequestSummary.row(request.userAnswers)
          )
      }

      val returnDetailsList = SummaryListViewModel(
        rows = (Seq(ReturnTypeSummary.row(request.userAnswers)) ++ returnTypeRows).flatten
      )

      val emailRows = Seq(
        ConfirmationByEmailSummary.row(request.userAnswers),
        EnterYourEmailAddressSummary.row(request.userAnswers)
      )

      val emailList = SummaryListViewModel(rows = emailRows.flatten)

      Ok(view(returnDetailsList, emailList))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      request.userAnswers.get(ReturnTypePage) match {
        case None =>
          logger.warn(
            "[CheckYourAnswersController] C6 submit without FormP record (missing ReturnTypePage); redirecting to journey recovery"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(returnType) =>
          if (submissionService.isAlreadySubmitted(request.userAnswers)) {
            logger.info(
              "[CheckYourAnswersController] Submission is already created; redirecting to journey recovery"
            )
            Future.successful(Redirect(controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad()))
          } else {
            val updateRequest = UpdateMonthlyReturnRequest.fromUserAnswers(request.userAnswers)

            updateRequest match {
              case Left(error) =>
                logger.error(s"[CheckYourAnswersController] Failed to build update request: $error")
                Future.successful(InternalServerError)

              case Right(req) =>
                monthlyReturnService
                  .updateMonthlyReturn(req)
                  .map { _ =>
                    logger.info(
                      s"[CheckYourAnswersController] Successfully updated monthly return ($returnType), redirecting to submission"
                    )
                    Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoad())
                  }
                  .recover { case t =>
                    logger.error("[CheckYourAnswersController] Failed to update monthly return ($returnType)", t)
                    Redirect(controllers.routes.SystemErrorController.onPageLoad())
                  }
            }
          }
      }
  }
}
