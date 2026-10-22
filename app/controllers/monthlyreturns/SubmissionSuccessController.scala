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

import config.FrontendAppConfig
import controllers.actions.*
import controllers.helpers.SubmissionViewDataSupport
import models.{ReturnType, UserAnswers}
import models.monthlyreturns.{GetAllMonthlyReturnDetailsResponse, SubmissionConfirmationCache}
import models.ReturnType.reads
import models.requests.{CisPath, GetMonthlyReturnCompleteRequest, GetMonthlyReturnForEditRequest}
import pages.monthlyreturns.*
import pages.submission.{CorrelationIdPage, SubmissionDetailsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.MonthlyReturnService
import services.guard.SubmissionSuccessfulServiceGuard
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmissionSuccessView
import utils.{DateTimeFormats, IrMarkReferenceGenerator}
import viewmodels.checkAnswers.monthlyreturns.SubmissionSuccessViewModel

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  val controllerComponents: MessagesControllerComponents,
  view: SubmissionSuccessView,
  clock: Clock,
  monthlyReturnService: MonthlyReturnService,
  submissionSuccessGuard: SubmissionSuccessfulServiceGuard
)(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging
    with SubmissionViewDataSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      // DTR-9431: Preserve legacy exit navigation.
      val exitUrl = controllers.monthlyreturns.routes.ManageCisReturnController.onExit().url

      if (!submissionSuccessGuard.check) {
        logger.error(
          s"[SubmissionSuccessController][onPageLoad] - submissionSuccessGuard check failed correlationId=${request.userAnswers.get(CorrelationIdPage).getOrElse("unknown")}"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } else {
        val ua = request.userAnswers

        ua.get(SubmissionConfirmationCachePage) match {
          case Some(cache) =>
            Future.successful(
              Ok(
                view(
                  buildViewModelFromCache(
                    cache,
                    ua,
                    required(ua.get(CisIdPage), "[SubmissionSuccess] cisId missing from userAnswers"),
                    employerRefFrom(request)
                  ),
                  exitUrl
                )
              )
            )

          case None =>
            GetMonthlyReturnCompleteRequest.fromUserAnswers(ua) match {
              case Left(error) =>
                logger.error(
                  s"[SubmissionSuccessController] " +
                    s"Failed to build GetMonthlyReturnCompleteRequest: $error " +
                    s"correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}"
                )

                Future.successful(
                  Redirect(
                    controllers.routes.JourneyRecoveryController.onPageLoad()
                  )
                )

              case Right(req) =>
                logger.info(
                  s"[SubmissionSuccessController] Calling getMonthlyReturnComplete " +
                    s"instanceId=${req.instanceId} taxYear=${req.taxYear} taxMonth=${req.taxMonth} amendment=${req.amendment} " +
                    s"correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}"
                )

                (for {
                  monthlyReturn <-
                    monthlyReturnService.getMonthlyReturnComplete(req)

                  vm <-
                    buildViewModel(
                      ua = ua,
                      monthlyReturn = monthlyReturn,
                      cisId = required(ua.get(CisIdPage), "[SubmissionSuccess] cisId missing from userAnswers"),
                      empRef = employerRefFrom(request)
                    )

                  uaWithCache <-
                    Future.fromTry(
                      ua.set(
                        SubmissionConfirmationCachePage,
                        cacheFrom(vm)
                      )
                    )

                  _ <-
                    monthlyReturnService.completeSubmissionJourney(
                      uaWithCache
                    )
                } yield Ok(view(vm, exitUrl))).recoverWith { case ex: UpstreamErrorResponse =>
                  logger.error(
                    s"[SubmissionSuccessController] getMonthlyReturnComplete failed " +
                      s"with status ${ex.statusCode} for instanceId=${req.instanceId} " +
                      s"correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}",
                    ex
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                }
            }
        }
      }
    }

  // DTR-9431: Display confirmation for a UUID-scoped submission.
  def onPageLoadWithJourneyId(cisPath: CisPath, uuid: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)).async { implicit request =>

      val exitUrl =
        controllers.monthlyreturns.routes.ManageCisReturnController
          .onExitWithJourneyId(cisPath, uuid)
          .url

      if (!submissionSuccessGuard.check(request.userAnswers)) {
        logger.error(
          s"[SubmissionSuccessController][onPageLoadWithJourneyId] - submissionSuccessGuard check failed correlationId=${request.userAnswers.get(CorrelationIdPage).getOrElse("unknown")}"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } else {
        val ua = request.userAnswers

        ua.get(SubmissionConfirmationCachePage) match {
          case Some(cache) =>
            Future.successful(
              Ok(
                view(
                  buildViewModelFromCache(
                    cache,
                    ua,
                    request.cisId,
                    request.employerRef
                  ),
                  exitUrl
                )
              )
            )

          case None =>
            // DTR-9431: Build the request using the authorised CIS ID.
            GetMonthlyReturnForEditRequest.fromUserAnswers(request.cisId, ua) match {
              case Left(error) =>
                logger.error(
                  s"[SubmissionSuccessController] Failed to build GetMonthlyReturnForEditRequest: $error " +
                    s"correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}"
                )

                Future.successful(
                  Redirect(
                    controllers.routes.JourneyRecoveryController.onPageLoad()
                  )
                )

              case Right(req) =>
                logger.info(
                  s"[SubmissionSuccessController] Calling retrieveMonthlyReturnForEditDetails " +
                    s"instanceId=${req.instanceId} taxYear=${req.taxYear} taxMonth=${req.taxMonth} isAmendment=${req.isAmendment} " +
                    s"correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}"
                )

                (for {
                  monthlyReturn <-
                    monthlyReturnService.retrieveMonthlyReturnForEditDetails(req)

                  vm <-
                    buildViewModel(
                      ua = ua,
                      monthlyReturn = monthlyReturn,
                      cisId = request.cisId,
                      empRef = request.employerRef
                    )

                  uaWithCache <-
                    Future.fromTry(
                      ua.set(
                        SubmissionConfirmationCachePage,
                        cacheFrom(vm)
                      )
                    )

                  _ <-
                    monthlyReturnService.completeSubmissionJourney(
                      uaWithCache
                    )
                } yield Ok(view(vm, exitUrl))).recoverWith { case ex: UpstreamErrorResponse =>
                  logger.error(
                    s"[SubmissionSuccessController] UUID-scoped monthly return retrieval failed " +
                      s"with status ${ex.statusCode} for instanceId=${req.instanceId} " +
                      s"correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}",
                    ex
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                }
            }
        }
      }
    }

  private def cacheFrom(vm: SubmissionSuccessViewModel): SubmissionConfirmationCache =
    SubmissionConfirmationCache(
      periodEnd = vm.periodEnd,
      contractorName = vm.contractorName,
      email = vm.email,
      submittedTime = vm.submittedTime,
      submittedDate = vm.submittedDate,
      submittedDateTimeIso = vm.submittedDateTimeIso
    )

  // DTR-9431: Accept explicit CIS ID and employer reference for scoped journeys.
  private def buildViewModelFromCache(
    cache: SubmissionConfirmationCache,
    ua: UserAnswers,
    cisId: String,
    empRef: String
  )(implicit request: Request[_]): SubmissionSuccessViewModel = {
    val reference           = IrMarkReferenceGenerator.fromBase64(
      required(ua.get(SubmissionDetailsPage), "[SubmissionSuccess] submissionDetails missing from userAnswers").irMark
    )
    val submissionType      =
      required(ua.get(ReturnTypePage), "[SubmissionSuccess] ReturnTypePage missing from userAnswers")
    implicit val lang: Lang = messagesApi.preferred(request).lang

    val periodEnd = ua
      .get(DateConfirmPaymentsPage)
      .map(_.format(DateTimeFormats.dateTimeFormat()))
      .getOrElse(cache.periodEnd)

    val legacyDateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.UK)
    val submittedDate = cache.submittedDateTimeIso
      .flatMap(iso => scala.util.Try(ZonedDateTime.parse(iso)).toOption)
      .map(_.withZoneSameInstant(ZoneId.of("Europe/London")).format(DateTimeFormats.fullDateFormat()))
      .orElse(
        scala.util
          .Try(java.time.LocalDate.parse(cache.submittedDate, legacyDateFmt))
          .toOption
          .map(_.format(DateTimeFormats.fullDateFormat()))
      )
      .getOrElse(cache.submittedDate)

    SubmissionSuccessViewModel(
      reference = reference,
      periodEnd = periodEnd,
      submittedTime = cache.submittedTime,
      submittedDate = submittedDate,
      contractorName = cache.contractorName,
      empRef = empRef,
      email = cache.email,
      submissionType = submissionType,
      cisId = cisId
    )
  }

  // DTR-9431: Accept explicit CIS ID and employer reference for scoped journeys.
  private def buildViewModel(
    ua: UserAnswers,
    monthlyReturn: GetAllMonthlyReturnDetailsResponse,
    cisId: String,
    empRef: String
  )(implicit request: Request[_], hc: HeaderCarrier): Future[SubmissionSuccessViewModel] = {

    val submissionType =
      required(ua.get(ReturnTypePage), "[SubmissionSuccess] ReturnTypePage missing from userAnswers")

    val periodEnd = required(
      periodEndFromUserAnswers(ua),
      "[SubmissionSuccess] taxPeriodEnd missing from userAnswers"
    )

    val reference = IrMarkReferenceGenerator.fromBase64(
      required(ua.get(SubmissionDetailsPage), "[SubmissionSuccess] submissionDetails missing from userAnswers").irMark
    )

    val contractorName = monthlyReturn.scheme.headOption
      .flatMap(_.name)
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("[SubmissionSuccess] Scheme name is missing"))

    implicit val lang: Lang = messagesApi.preferred(request).lang
    val timeFmt             = DateTimeFormatter.ofPattern("h:mma")
    val nowUk               = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))

    resolveEmail(ua, cisId).map { email =>
      SubmissionSuccessViewModel(
        reference = reference,
        periodEnd = periodEnd.format(DateTimeFormats.dateTimeFormat()),
        submittedTime = nowUk.format(timeFmt).toLowerCase,
        submittedDate = nowUk.format(DateTimeFormats.fullDateFormat()),
        contractorName = contractorName,
        empRef = empRef,
        email = email,
        submissionType = submissionType,
        cisId = cisId,
        submittedDateTimeIso = Some(nowUk.toString)
      )
    }
  }

  private def resolveEmail(ua: UserAnswers, cisId: String)(implicit
    hc: HeaderCarrier
  ): Future[String] =
    if (ua.get(ConfirmationByEmailPage).contains(false)) {
      Future.successful("")
    } else {
      emailfromUserAnswers(ua)
        .map(Future.successful)
        .getOrElse(
          monthlyReturnService
            .getSchemeEmail(cisId)
            .map(_.getOrElse(""))
            .recover { case ex =>
              logger.warn(
                s"[SubmissionSuccess] getSchemeEmail failed for cisId=$cisId, defaulting to empty, correlationId=${ua.get(CorrelationIdPage).getOrElse("unknown")}",
                ex
              )
              ""
            }
        )
    }
}
