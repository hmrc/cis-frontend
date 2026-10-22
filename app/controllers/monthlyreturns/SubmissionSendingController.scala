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
import models.requests.{CisIdDataRequest, CisPath}
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
  resolveScheme: SchemeAction,
  reconcileFormPAndRds: ReconcileFormPAndRdsAction,
  getJourney: JourneyAction,
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

  // DTR-9431: Submit a UUID-scoped monthly return to ChRIS.
  def onPageLoadWithJourneyId(cisPath: CisPath, uuid: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen reconcileFormPAndRds andThen getJourney(uuid)).async {
      implicit request =>
        guardCompletedJourney(
          request.userAnswers,
          routes.AlreadySubmittedController.onPageLoadWithJourneyId(cisPath, uuid)
        ) {
          if (!request.userAnswers.isJourneyComplete) {
            logger.error("[SubmissionSendingController] UUID-scoped journey is incomplete")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          } else
            (for {
              (submissionId, updatedAnswers, isResubmission) <-
                submissionService.getOrCreateSubmissionForChris(request.cisId, request.userAnswers)
              submitted                                      <-
                submissionService.submitToChrisAndPersist(
                  submissionId,
                  request.cisId,
                  request.cisTaxpayer,
                  updatedAnswers,
                  request.identifier.isAgent,
                  isResubmission
                )
              _                                              <-
                submissionService.updateSubmissionFromChrisResponse(
                  request.cisId,
                  request.identifier.agentReference,
                  submissionId,
                  updatedAnswers,
                  submitted
                )
            } yield SubmissionStatus.fromString(submitted.status) match {
              case Started =>
                logger.info(
                  s"[SubmissionSendingController] UUID-scoped submitted.status=${submitted.status} correlationId=${submitted.correlationId
                      .getOrElse("unknown")}"
                )
                Redirect(routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithJourneyId(cisPath, uuid))

              case Pending | SubmissionStatus.Accepted =>
                Redirect(routes.SubmissionSendingController.onPollAndRedirectWithJourneyId(cisPath, uuid))

              case _ =>
                Redirect(routes.SubmissionUnsuccessfulController.onPageLoadWithJourneyId(cisPath, uuid))
            }).recover { case NonFatal(ex) =>
              logger.error("[SubmissionSendingController] UUID-scoped ChRIS submission failed", ex)
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
        }
    }

  // DTR-9431: Poll ChRIS submission status using UUID-scoped UserAnswers.
  def onPollAndRedirectWithJourneyId(cisPath: CisPath, uuid: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)).async { implicit request =>
      guardCompletedJourney(
        request.userAnswers,
        routes.AlreadySubmittedController.onPageLoadWithJourneyId(cisPath, uuid)
      ) {
        request.userAnswers.get(SubmissionDetailsPage) match {
          case None =>
            logger.error("[SubmissionSendingController] UUID-scoped SubmissionDetailsPage missing")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Some(_) =>
            val refreshInterval = submissionService.getRefreshInterval.toString
            val langCode        = messagesApi.preferred(request).lang.code

            submissionService
              .checkAndUpdateSubmissionStatusIfAllowed(
                request.cisId,
                request.identifier.agentReference,
                request.userAnswers
              )
              .flatMap {
                case Skip =>
                  Future.successful(Ok(view()).withHeaders("Refresh" -> refreshInterval))

                case Polled(status) =>
                  SubmissionStatus.fromString(status) match {
                    case Started =>
                      Future.successful(
                        Redirect(routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithJourneyId(cisPath, uuid))
                      )

                    case Pending | SubmissionStatus.Accepted =>
                      Future.successful(Ok(view()).withHeaders("Refresh" -> refreshInterval))

                    case TimedOut =>
                      Future.successful(
                        Redirect(routes.SubmissionAwaitingController.onPageLoadWithJourneyId(cisPath, uuid))
                      )

                    case Submitted =>
                      sendEmailAndRedirect(
                        request.userAnswers,
                        langCode,
                        routes.SubmissionSuccessController.onPageLoadWithJourneyId(cisPath, uuid)
                      )

                    case SubmittedNoReceipt =>
                      sendEmailAndRedirect(
                        request.userAnswers,
                        langCode,
                        routes.SubmittedNoReceiptController.onPageLoadWithJourneyId(cisPath, uuid)
                      )

                    case DepartmentalError =>
                      sendEmailAndRedirect(
                        request.userAnswers,
                        langCode,
                        routes.SubmissionUnsuccessfulController.onPageLoadWithJourneyId(cisPath, uuid)
                      )

                    case SubmissionStatus.FatalError =>
                      Future.successful(
                        Redirect(routes.SubmissionUnsuccessfulController.onPageLoadWithJourneyId(cisPath, uuid))
                      )

                    case _ =>
                      logger.error(s"[SubmissionSendingController] Unrecognised UUID-scoped submission status=$status")
                      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                  }
              }
              .recover { case NonFatal(ex) =>
                logger.error("[SubmissionSendingController] UUID-scoped submission polling failed", ex)
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
        }
      }
    }

  // DTR-9431: Guard completed journeys without depending on legacy CisIdDataRequest.
  private def guardCompletedJourney(
    ua: UserAnswers,
    alreadySubmittedRoute: Call
  )(block: => Future[Result]): Future[Result] =
    periodEndFromUserAnswers(ua) match {
      case None =>
        logger.error("[SubmissionSendingController] UUID-scoped periodEnd missing")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(periodEnd) =>
        val yearMonthPeriod = YearMonth.from(periodEnd).toString
        if (ua.get(SubmissionJourneyCompletedPage(yearMonthPeriod)).contains(true))
          Future.successful(Redirect(alreadySubmittedRoute))
        else
          block
    }
}
