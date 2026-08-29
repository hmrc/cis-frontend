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
import models.UserAnswers
import models.requests.CisIdDataRequest
import models.submission.PollDecision.{Polled, Skip}
import models.submission.SubmissionStatus.*
import models.submission.{PollDecision, SubmissionDetails, SubmissionStatus}
import pages.agent.AgentClientDataPage
import pages.submission.*
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, PRECONDITION_FAILED}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.FormpRdsReconcileService
import services.submission.SubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.UserAnswerUtils.isJourneyComplete
import views.html.monthlyreturns.SubmissionSendingView

import java.time.YearMonth
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  submissionService: SubmissionService,
  formpRdsReconcileService: FormpRdsReconcileService,
  view: SubmissionSendingView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging
    with SubmissionViewDataSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      guardCompletedJourney {
        implicit val hc: HeaderCarrier =
          HeaderCarrierConverter.fromRequestAndSession(request, request.session)

        if (!request.userAnswers.isJourneyComplete)
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        else
          reconcileFormpRdsBeforeChris.flatMap {
            case Some(redirect) => Future.successful(redirect)
            case None           =>
              (for {
                (submissionId, updatedAnswers, isResubmission) <-
                  submissionService.getOrCreateSubmissionForChris(request.userAnswers)
                submitted                                      <-
                  submissionService.submitToChrisAndPersist(
                    submissionId,
                    updatedAnswers,
                    request.isAgent,
                    isResubmission
                  )
                _                                              <-
                  submissionService.updateSubmissionFromChrisResponse(
                    submissionId,
                    updatedAnswers,
                    submitted
                  )
              } yield SubmissionStatus.fromString(submitted.status) match {
                case Started                             =>
                  logger.info(s"[SubmissionSendingController] submitted.status=${submitted.status}")
                  Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulResubmitController.onPageLoad())
                case Pending | SubmissionStatus.Accepted =>
                  Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect)
                case _                                   =>
                  Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad)
              }).recover { case ex =>
                logger.error("[SubmissionSendingController] Create/Submit/Update flow failed", ex)
                Redirect(controllers.routes.SystemErrorController.onPageLoad())
              }
          }
      }
    }

  private def reconcileFormpRdsBeforeChris(implicit
    request: CisIdDataRequest[_],
    hc: HeaderCarrier
  ): Future[Option[Result]] =
    resolveTaxOffice(request) match {
      case Some((taxOfficeNumber, taxOfficeReference)) =>
        formpRdsReconcileService
          .reconcile(request.cisId, taxOfficeNumber, taxOfficeReference)
          .map(_ => None)
          .recover {
            case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
              logger.warn(
                s"[SubmissionSendingController] Contractor known facts missing in RDS (status ${e.statusCode})"
              )
              Some(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()))
            case NonFatal(e)                                                                                  =>
              logger.error(
                s"[SubmissionSendingController] FormP/RDS reconciliation failed: ${e.getMessage}",
                e
              )
              Some(Redirect(controllers.routes.SystemErrorController.onPageLoad()))
          }
      case None                                        =>
        logger.warn("[SubmissionSendingController] Missing tax office reference for FormP/RDS reconciliation")
        Future.successful(Some(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad())))
    }

  private def resolveTaxOffice(request: CisIdDataRequest[_]): Option[(String, String)] =
    if (request.isAgent)
      request.userAnswers.get(AgentClientDataPage).map(a => (a.taxOfficeNumber, a.taxOfficeReference))
    else
      request.employerReference.map(ref => (ref.taxOfficeNumber, ref.taxOfficeReference))

  def onPollAndRedirect: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      guardCompletedJourney {
        request.userAnswers.get(SubmissionDetailsPage) match {
          case None =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Some(submissionStatus) =>
            val pollInterval = submissionService.getPollInterval(request.userAnswers).toString
            submissionService
              .checkAndUpdateSubmissionStatusIfAllowed(request.userAnswers)
              .flatMap(decision => pollDecisionResult(decision, pollInterval))
              .recover(_ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }

  private def guardCompletedJourney(block: => Future[Result])(implicit request: CisIdDataRequest[_]): Future[Result] =
    periodEndFromUserAnswers(request.userAnswers) match {
      case None            =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(periodEnd) =>
        val yearMonthPeriod = YearMonth.from(periodEnd).toString
        if (request.userAnswers.get(SubmissionJourneyCompletedPage(yearMonthPeriod)).contains(true)) {
          Future.successful(Redirect(controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad()))
        } else {
          block
        }
    }

  private def pollDecisionResult(decision: PollDecision, pollInterval: String)(implicit
    request: CisIdDataRequest[_]
  ): Future[Result] =
    decision match {
      case Skip           => sendingPage(pollInterval)
      case Polled(status) => polledStatusResult(status, pollInterval)
    }

  private def polledStatusResult(status: String, pollInterval: String)(implicit
    request: CisIdDataRequest[_]
  ): Future[Result] =
    val langCode = messagesApi.preferred(request).lang.code
    SubmissionStatus.fromString(status) match {
      case Started                             => Future.successful(Redirect(routes.SubmissionUnsuccessfulResubmitController.onPageLoad()))
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

  private def sendingPage(pollInterval: String)(implicit request: CisIdDataRequest[_]): Future[Result] =
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
