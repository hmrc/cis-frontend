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
import models.requests.{CisIdDataRequest, DataRequest}
import models.submission.PollDecision.{Polled, Skip}
import models.submission.SubmissionStatus.*
import models.submission.{PollDecision, SubmissionDetails, SubmissionStatus}
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.*
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
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class SubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  multiTabAction: MultiTabAction,
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

        if (!request.userAnswers.isJourneyComplete) {
          logger.error("[SubmissionSendingController][onPageLoad] - isJourneyComplete check failed")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        } else if (alreadySentToChris(request.userAnswers)) {
          // A reload, back-button return or retry link after the submission was sent. Resume polling rather than
          // refreshing the items, creating a second submission or sending to ChRIS again.
          logger.info("[SubmissionSendingController][onPageLoad] - submission already sent to ChRIS, resuming polling")
          Future.successful(Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect))
        } else
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
                  logger.info(
                    s"[SubmissionSendingController] submitted.status=${submitted.status} correlationId=${submitted.correlationId
                        .getOrElse("unknown")}"
                  )
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

  /** A STARTED status means the attempt did not reach ChRIS, so a retry is allowed. */
  private def alreadySentToChris(ua: UserAnswers): Boolean =
    ua.get(SubmissionDetailsPage).exists(details => SubmissionStatus.fromString(details.status) != Started)

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
              Some(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
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
            logger.error(
              s"[SubmissionSendingController][onPollAndRedirect] - SubmissionDetailsPage missing from user answers correlationId=${request.userAnswers.get(CorrelationIdPage).getOrElse("unknown")}"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Some(submissionStatus) =>
            val refreshInterval = submissionService.getRefreshInterval.toString
            submissionService
              .checkAndUpdateSubmissionStatusIfAllowed(request.userAnswers)
              .flatMap(decision => pollDecisionResult(decision, refreshInterval))
              .recover { case ex =>
                logger.error(
                  s"[SubmissionSendingController][onPollAndRedirect] - submission status poll failed correlationId=${request.userAnswers.get(CorrelationIdPage).getOrElse("unknown")}",
                  ex
                )
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
        }
      }
    }

  private def guardCompletedJourney(block: => Future[Result])(implicit request: CisIdDataRequest[_]): Future[Result] =
    periodEndFromUserAnswers(request.userAnswers) match {
      case None            =>
        logger.error("[SubmissionSendingController][guardCompletedJourney] - periodEnd missing from user answers")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(periodEnd) =>
        val yearMonthPeriod = YearMonth.from(periodEnd).toString
        if (request.userAnswers.get(SubmissionJourneyCompletedPage(yearMonthPeriod)).contains(true)) {
          Future.successful(Redirect(controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad()))
        } else {
          block
        }
    }

  private def pollDecisionResult(decision: PollDecision, refreshInterval: String)(implicit
    request: CisIdDataRequest[_]
  ): Future[Result] =
    decision match {
      case Skip           => sendingPage(refreshInterval)
      case Polled(status) => polledStatusResult(status, refreshInterval)
    }

  private def polledStatusResult(status: String, refreshInterval: String)(implicit
    request: CisIdDataRequest[_]
  ): Future[Result] =
    val langCode = messagesApi.preferred(request).lang.code
    SubmissionStatus.fromString(status) match {
      case Started                             => Future.successful(Redirect(routes.SubmissionUnsuccessfulResubmitController.onPageLoad()))
      case Pending | SubmissionStatus.Accepted => sendingPage(refreshInterval)
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
      case _                                   =>
        logger.error(
          s"[SubmissionSendingController][polledStatusResult] - unrecognised status=$status correlationId=${request.userAnswers.get(CorrelationIdPage).getOrElse("unknown")}"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def sendingPage(refreshInterval: String)(implicit request: CisIdDataRequest[_]): Future[Result] =
    Future.successful(Ok(view()).withHeaders("Refresh" -> refreshInterval))

  private def sendEmailAndRedirect(
    userAnswers: UserAnswers,
    langCode: String,
    redirect: Call
  )(implicit hc: HeaderCarrier) =
    submissionService
      .sendSuccessEmail(userAnswers, langCode)
      .recover { case ex =>
        logger.warn(
          s"[SubmissionSendingController] Sending success email failed, continuing correlationId=${userAnswers.get(CorrelationIdPage).getOrElse("unknown")}",
          ex
        )()
      }
      .map(_ => Redirect(redirect))

  // Multi-tab: Submit the ID-scoped monthly return to ChRIS.
  def onPageLoadWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val userAnswers = request.userAnswers
      val id          = flowId.toString
      val cisId       = userAnswers
        .get(CisIdPage(id))
        .getOrElse(throw new IllegalStateException("CisId missing after MultiTabAction validation"))

      guardCompletedJourney(userAnswers, id) {
        if (!userAnswers.isJourneyComplete(id)) {
          logger.error("[SubmissionSendingController][onPageLoadWithId] - isJourneyComplete check failed")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        } else if (alreadySentToChris(userAnswers, id)) {
          // A reload, back-button return or retry link after the submission was sent. Resume polling rather than
          // refreshing the items, creating a second submission or sending to ChRIS again.
          logger.info(
            "[SubmissionSendingController][onPageLoadWithId] - Submission already sent to ChRIS, resuming polling"
          )
          Future.successful(
            Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId))
          )
        } else
          reconcileFormpRdsBeforeChris(cisId, id).flatMap {
            case Some(redirect) => Future.successful(redirect)
            case None           =>
              (for {
                (submissionId, updatedAnswers, isResubmission) <-
                  submissionService.getOrCreateSubmissionForChris(userAnswers, id)
                submitted                                      <-
                  submissionService.submitToChrisAndPersist(
                    submissionId,
                    updatedAnswers,
                    id,
                    request.isAgent,
                    isResubmission
                  )
                _                                              <-
                  submissionService.updateSubmissionFromChrisResponse(
                    submissionId,
                    updatedAnswers,
                    id,
                    submitted
                  )
              } yield SubmissionStatus.fromString(submitted.status) match {
                case Started                             =>
                  logger.info(
                    s"[SubmissionSendingController][onPageLoadWithId] submitted.status=${submitted.status} correlationId=${submitted.correlationId
                        .getOrElse("unknown")}"
                  )
                  Redirect(
                    controllers.monthlyreturns.routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithId(flowId)
                  )
                case Pending | SubmissionStatus.Accepted =>
                  Redirect(
                    controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId)
                  )
                case _                                   =>
                  Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoadWithId(flowId))
              }).recover { case NonFatal(ex) =>
                logger.error("[SubmissionSendingController][onPageLoadWithId] Create/Submit/Update flow failed", ex)
                Redirect(controllers.routes.SystemErrorController.onPageLoad())
              }
          }
      }
    }

  // Multi-tab: Poll ChRIS submission status for the ID-scoped journey.
  def onPollAndRedirectWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val userAnswers = request.userAnswers
      val id          = flowId.toString

      guardCompletedJourney(userAnswers, id) {
        userAnswers.get(SubmissionDetailsPage(id)) match {
          case None =>
            logger.error(
              s"[SubmissionSendingController][onPollAndRedirectWithId] - SubmissionDetailsPage missing from user answers correlationId=${userAnswers
                  .get(CorrelationIdPage(id))
                  .getOrElse("unknown")}"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Some(_) =>
            val refreshInterval = submissionService.getRefreshInterval.toString
            submissionService
              .checkAndUpdateSubmissionStatusIfAllowed(userAnswers, id)
              .flatMap(decision => pollDecisionResult(decision, refreshInterval, id))
              .recover { case NonFatal(ex) =>
                logger.error(
                  s"[SubmissionSendingController][onPollAndRedirectWithId] - submission status poll failed correlationId=${userAnswers
                      .get(CorrelationIdPage(id))
                      .getOrElse("unknown")}",
                  ex
                )
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
        }
      }
    }

  // Multi-tab: Check whether the ID-scoped submission has already been sent to ChRIS.
  private def alreadySentToChris(ua: UserAnswers, id: String): Boolean =
    ua.get(SubmissionDetailsPage(id)).exists(details => SubmissionStatus.fromString(details.status) != Started)

  // Multi-tab: Check whether the ID-scoped submission journey has already been completed.
  private def guardCompletedJourney(
    userAnswers: UserAnswers,
    id: String
  )(block: => Future[Result]): Future[Result] =
    userAnswers.get(DateConfirmPaymentsPage(id)) match {
      case None =>
        logger.error(
          "[SubmissionSendingController][guardCompletedJourneyWithId] - periodEnd missing from scoped user answers"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(periodEnd) =>
        val yearMonthPeriod = YearMonth.from(periodEnd).toString

        if (userAnswers.get(SubmissionJourneyCompletedForFlowPage(id, yearMonthPeriod)).contains(true)) {
          Future.successful(
            Redirect(controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoadWithId(UUID.fromString(id)))
          )
        } else {
          block
        }
    }

  // Multi-tab: Reconcile FormP/RDS before submitting the ID-scoped return to ChRIS.
  private def reconcileFormpRdsBeforeChris(cisId: String, id: String)(implicit
    request: DataRequest[_],
    hc: HeaderCarrier
  ): Future[Option[Result]] =
    resolveTaxOffice(request, id) match {
      case Some((taxOfficeNumber, taxOfficeReference)) =>
        formpRdsReconcileService
          .reconcile(cisId, taxOfficeNumber, taxOfficeReference)
          .map(_ => None)
          .recover {
            case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
              logger.warn(
                s"[SubmissionSendingController][reconcileFormpRdsBeforeChrisWithId] Contractor known facts missing in RDS (status ${e.statusCode})"
              )
              Some(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()))
            case NonFatal(e)                                                                                  =>
              logger.error(
                s"[SubmissionSendingController][reconcileFormpRdsBeforeChrisWithId] FormP/RDS reconciliation failed: ${e.getMessage}",
                e
              )
              Some(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
      case None                                        =>
        logger.warn(
          "[SubmissionSendingController][reconcileFormpRdsBeforeChrisWithId] Missing tax office reference for FormP/RDS reconciliation"
        )
        Future.successful(Some(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad())))
    }

  // Multi-tab: Resolve tax office details for the ID-scoped return journey.
  private def resolveTaxOffice(request: DataRequest[_], id: String): Option[(String, String)] =
    if (request.isAgent)
      request.userAnswers.get(AgentClientDataPage(id)).map(a => (a.taxOfficeNumber, a.taxOfficeReference))
    else
      request.employerReference.map(ref => (ref.taxOfficeNumber, ref.taxOfficeReference))

  // Multi-tab: Handle the polling decision for the ID-scoped journey.
  private def pollDecisionResult(
    decision: PollDecision,
    refreshInterval: String,
    id: String
  )(implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[Result] =
    decision match {
      case Skip           => sendingPageWithId(refreshInterval)
      case Polled(status) => polledStatusResult(status, refreshInterval, id)
    }

  // Multi-tab: Handle the polled ChRIS status for the ID-scoped journey.
  private def polledStatusResult(
    status: String,
    refreshInterval: String,
    id: String
  )(implicit request: DataRequest[AnyContent], hc: HeaderCarrier): Future[Result] = {
    val langCode = messagesApi.preferred(request).lang.code

    SubmissionStatus.fromString(status) match {
      case Started                             =>
        Future.successful(
          Redirect(routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithId(UUID.fromString(id)))
        )
      case Pending | SubmissionStatus.Accepted =>
        sendingPageWithId(refreshInterval)
      case TimedOut                            =>
        Future.successful(Redirect(routes.SubmissionAwaitingController.onPageLoadWithId(UUID.fromString(id))))
      case Submitted                           =>
        sendEmailAndRedirect(
          request.userAnswers,
          id,
          langCode,
          routes.SubmissionSuccessController.onPageLoadWithId(UUID.fromString(id))
        )
      case SubmittedNoReceipt                  =>
        sendEmailAndRedirect(
          request.userAnswers,
          id,
          langCode,
          routes.SubmittedNoReceiptController.onPageLoadWithId(UUID.fromString(id))
        )
      case DepartmentalError                   =>
        sendEmailAndRedirect(
          request.userAnswers,
          id,
          langCode,
          routes.SubmissionUnsuccessfulController.onPageLoadWithId(UUID.fromString(id))
        )
      case SubmissionStatus.FatalError         =>
        Future.successful(Redirect(routes.SubmissionUnsuccessfulController.onPageLoadWithId(UUID.fromString(id))))
      case _                                   =>
        logger.error(
          s"[SubmissionSendingController][polledStatusResultWithId] - unrecognised status=$status correlationId=${request.userAnswers.get(CorrelationIdPage(id)).getOrElse("unknown")}"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }

  // Multi-tab: Render the sending page for the ID-scoped journey.
  private def sendingPageWithId(refreshInterval: String)(implicit request: DataRequest[AnyContent]): Future[Result] =
    Future.successful(Ok(view()).withHeaders("Refresh" -> refreshInterval))

  // Multi-tab: Send the success email using ID-scoped answers and redirect.
  private def sendEmailAndRedirect(
    userAnswers: UserAnswers,
    id: String,
    langCode: String,
    redirect: Call
  )(implicit hc: HeaderCarrier): Future[Result] =
    submissionService
      .sendSuccessEmail(userAnswers, id, langCode)
      .recover { case NonFatal(ex) =>
        logger.warn(
          s"[SubmissionSendingController][sendEmailAndRedirectWithId] Sending success email failed, continuing correlationId=${userAnswers
              .get(CorrelationIdPage(id))
              .getOrElse("unknown")}",
          ex
        )
        userAnswers
      }
      .map(_ => Redirect(redirect))
}
