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
import controllers.helpers.SubmissionViewDataSupport
import models.{CheckMode, ReturnType, UserAnswers}
import models.monthlyreturns.UpdateMonthlyReturnRequest
import models.requests.{CisIdDataRequest, CisPath}
import pages.monthlyreturns.ReturnTypePage
import pages.submission.SubmissionJourneyCompletedPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.MonthlyReturnService
import services.submission.SubmissionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.*
import viewmodels.govuk.summarylist.*
import views.html.monthlyreturns.CheckYourAnswersView
import utils.UserAnswerUtils.isJourneyComplete

import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  monthlyReturnService: MonthlyReturnService,
  submissionService: SubmissionService,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging
    with SubmissionViewDataSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      guardCompletedJourney {
        ReturnTypeSummary.returnType(request.userAnswers) match {
          case Some(returnType) =>
            val returnTypeRows = returnType match {
              case ReturnType.MonthlyStandardReturn | ReturnType.MonthlyAmendedStandardReturn =>
                Seq(
                  DateConfirmPaymentsSummary.row(request.userAnswers),
                  NumberOfSubcontractorPaymentsMadeSummary.row(request.userAnswers),
                  EmploymentStatusDeclarationSummary.row(request.userAnswers),
                  VerifiedStatusDeclarationSummary.row(request.userAnswers),
                  SubmitInactivityRequestSummary.row(request.userAnswers)
                )
              case ReturnType.MonthlyNilReturn | ReturnType.MonthlyAmendedNilReturn           =>
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

            Future.successful(Ok(view(returnDetailsList, emailList, routes.CheckYourAnswersController.onSubmit())))

          case None =>
            logger.error("[CheckYourAnswersController][onPageLoad] - Missing ReturnTypePage")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      guardCompletedJourney {
        request.userAnswers.get(ReturnTypePage) match {
          case None =>
            logger.error(
              "[CheckYourAnswersController] C6 submit without FormP record (missing ReturnTypePage); redirecting to journey recovery"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Some(returnType) if !request.userAnswers.isJourneyComplete =>
            logger.error(
              "[CheckYourAnswersController][onSubmit] - journey not complete (isJourneyComplete check failed)"
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
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

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
                      logger.error(s"[CheckYourAnswersController] Failed to update monthly return ($returnType)", t)
                      Redirect(controllers.routes.SystemErrorController.onPageLoad())
                    }
              }
            }
        }
      }
  }

  private def guardCompletedJourney(block: => Future[Result])(implicit request: CisIdDataRequest[_]): Future[Result] =
    guardCompletedJourney(
      request.userAnswers,
      routes.AlreadySubmittedController.onPageLoad()
    )(block)

  // DTR-9431: Check journey completion without depending on the legacy request type.
  private def guardCompletedJourney(
    ua: UserAnswers,
    alreadySubmittedRoute: Call
  )(block: => Future[Result]): Future[Result] =
    periodEndFromUserAnswers(ua) match {
      case None =>
        logger.error("[CheckYourAnswersController] Unable to determine period end")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(periodEnd) =>
        val period = YearMonth.from(periodEnd).toString
        if ua.get(SubmissionJourneyCompletedPage(period)).contains(true) then
          Future.successful(Redirect(alreadySubmittedRoute))
        else block
    }

  // DTR-9431: Display answers from the UUID-scoped nil-return journey.
  def onPageLoadWithJourneyId(cisPath: CisPath, uuid: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)).async { implicit request =>
      val ua = request.userAnswers

      // temporarily redirect to journey recovery, once ChRIS submission is implemented for this journey, this will be
      // replaced by AlreadySubmittedController.onPageLoad(cisPath, uuid)
      guardCompletedJourney(ua, controllers.routes.JourneyRecoveryController.onPageLoad()) {
        ReturnTypeSummary.returnType(ua) match {
          case Some(ReturnType.MonthlyNilReturn | ReturnType.MonthlyAmendedNilReturn) =>
            val returnDetailsList = SummaryListViewModel(rows =
              Seq(
                ReturnTypeSummary.row(ua),
                DateConfirmNilPaymentsSummary.row(ua),
                PaymentsToSubcontractorsSummary.row,
                SubmitInactivityRequestSummary.row(
                  ua,
                  routes.SubmitInactivityRequestController.onPageLoadWithJourneyId(cisPath, uuid, CheckMode)
                )
              ).flatten
            )

            val emailList = SummaryListViewModel(rows =
              Seq(
                ConfirmationByEmailSummary.row(
                  ua,
                  routes.ConfirmationByEmailController.onPageLoadWithJourneyId(cisPath, uuid, CheckMode)
                ),
                EnterYourEmailAddressSummary.row(
                  ua,
                  routes.EnterYourEmailAddressController.onPageLoadWithJourneyId(cisPath, uuid, CheckMode)
                )
              ).flatten
            )

            Future.successful(
              Ok(
                view(
                  returnDetailsList,
                  emailList,
                  routes.CheckYourAnswersController.onSubmitWithJourneyId(cisPath, uuid)
                )
              )
            )

          // TODO DTR-9431: Add scoped standard-return summary rows.
          case _                                                                      =>
            logger.error("[CheckYourAnswersController] Missing or unsupported return type")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  // DTR-9431: Submit the UUID-scoped nil return.
  def onSubmitWithJourneyId(cisPath: CisPath, uuid: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)).async { implicit request =>
      val ua = request.userAnswers

      // temporarily redirect to journey recovery, once ChRIS submission is implemented for this journey, this will be
      // replaced by AlreadySubmittedController.onPageLoad(cisPath, uuid)
      guardCompletedJourney(ua, controllers.routes.JourneyRecoveryController.onPageLoad()) {
        ua.get(ReturnTypePage) match {
          case Some(ReturnType.MonthlyNilReturn | ReturnType.MonthlyAmendedNilReturn) if !ua.isJourneyComplete =>
            logger.error("[CheckYourAnswersController] UUID-scoped journey is incomplete")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Some(returnType @ (ReturnType.MonthlyNilReturn | ReturnType.MonthlyAmendedNilReturn)) =>
            if submissionService.isAlreadySubmitted(ua) then
              // Future.successful(Redirect(routes.AlreadySubmittedController.onPageLoad(cisPath, uuid)))
              Future.successful(NotImplemented("ChRIS submission is not implemented for this journey yet"))
            else
              UpdateMonthlyReturnRequest.fromUserAnswers(request.cisId, ua) match {
                case Left(error) =>
                  logger.error(s"[CheckYourAnswersController] Failed to build update request: $error")
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

                case Right(updateRequest) =>
                  monthlyReturnService
                    .updateMonthlyReturn(updateRequest)
                    .map { _ =>
                      logger.info(s"[CheckYourAnswersController] Updated UUID-scoped return ($returnType)")
                      // Redirect(routes.SubmissionSendingController.onPageLoad(cisPath, uuid))
                      NotImplemented("ChRIS submission is not implemented for this journey yet")
                    }
                    .recover { case NonFatal(ex) =>
                      logger.error("[CheckYourAnswersController] Failed to update monthly return", ex)
                      Redirect(controllers.routes.SystemErrorController.onPageLoad())
                    }
              }

          // TODO DTR-9431: Add scoped standard-return submission.
          case _                                                                                     =>
            logger.error("[CheckYourAnswersController] Missing or unsupported return type")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }
}
