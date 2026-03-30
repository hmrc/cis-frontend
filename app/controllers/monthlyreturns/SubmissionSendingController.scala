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
import models.UserAnswers
import models.requests.DataRequest
import models.submission.PollDecision.{Polled, Skip}
import models.submission.SubmissionStatus.*
import models.submission.{PollDecision, SubmissionDetails, SubmissionStatus}
import pages.submission.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.submission.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.SubmissionSendingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  submissionService: SubmissionService,
  view: SubmissionSendingView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      (for {
        created   <- submissionService.create(request.userAnswers)
        submitted <-
          submissionService.submitToChrisAndPersist(created.submissionId, request.userAnswers, request.isAgent)
        _         <- submissionService.updateSubmission(created.submissionId, request.userAnswers, submitted)
      } yield SubmissionStatus.fromString(submitted.status) match {
        // TODO - recoverable error for resubmit: case "STARTED" will be updated to a new page MR-05-b controller when ready
        case Started                             =>
          logger.info(s"[SubmissionSendingController] submitted.status=${submitted.status}")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Pending | SubmissionStatus.Accepted =>
          Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect)
        case _                                   => Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad)
      }).recover { case ex =>
        logger.error("[SubmissionSendingController] Create/Submit/Update flow failed", ex)
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
    }

  def onPollAndRedirect: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      request.userAnswers.get(SubmissionDetailsPage) match {
        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(submissionStatus) =>
          val pollInterval = submissionService.getPollInterval(request.userAnswers).toString
          submissionService
            .checkAndUpdateSubmissionStatusIfAllowed(request.userAnswers)
            .flatMap(decision => pollDecisionResult(decision, pollInterval))
      }
    }

  private def pollDecisionResult(decision: PollDecision, pollInterval: String)(implicit
    request: DataRequest[_]
  ): Future[Result] =
    decision match {
      case Skip           => sendingPage(pollInterval)
      case Polled(status) => polledStatusResult(status, pollInterval)
    }

  private def polledStatusResult(status: String, pollInterval: String)(implicit
    request: DataRequest[_]
  ): Future[Result] =
    val langCode = messagesApi.preferred(request).lang.code
    SubmissionStatus.fromString(status) match {
      case Pending | SubmissionStatus.Accepted => sendingPage(pollInterval)
      case TimedOut                            => Future.successful(Redirect(routes.SubmissionAwaitingController.onPageLoad))
      case Submitted                           =>
        sendEmailAndRedirect(
          request.userAnswers,
          langCode,
          routes.SubmissionSuccessController.onPageLoad
        )
      case SubmittedNoReceipt                  =>
        sendEmailAndRedirect(
          request.userAnswers,
          langCode,
          routes.SubmittedNoReceiptController.onPageLoad
        )
      case DepartmentalError                   =>
        sendEmailAndRedirect(
          request.userAnswers,
          langCode,
          routes.SubmissionUnsuccessfulController.onPageLoad
        )
      case SubmissionStatus.FatalError         =>
        Future.successful(Redirect(routes.SubmissionUnsuccessfulController.onPageLoad))
      case _                                   => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def sendingPage(pollInterval: String)(implicit request: DataRequest[_]): Future[Result] =
    Future.successful(Ok(view()).withHeaders("Refresh" -> pollInterval))

  private def sendEmailAndRedirect(
    userAnswers: UserAnswers,
    langCode: String,
    redirect: Call
  )(implicit hc: HeaderCarrier) =
    submissionService
      .sendSuccessEmail(userAnswers, langCode)
      .recover { case ex =>
        logger.warn("[SubmissionSendingController] Sending success email failed, continuing", ex)()
      }
      .map(_ => Redirect(redirect))
}
