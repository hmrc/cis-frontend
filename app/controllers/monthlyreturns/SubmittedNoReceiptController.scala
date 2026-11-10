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
import models.UserAnswers
import models.monthlyreturns.{GetAllMonthlyReturnDetailsResponse, SubmissionConfirmationCache}
import models.requests.{CisIdDataRequest, DataRequest, GetMonthlyReturnForEditRequest}
import pages.monthlyreturns.*
import play.api.i18n.{I18nSupport, Lang, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.DateTimeFormats
import viewmodels.checkAnswers.monthlyreturns.SubmittedNoReceiptViewModel
import views.html.monthlyreturns.SubmittedNoReceiptView

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import javax.inject.Inject
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubmittedNoReceiptController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  multiTabAction: MultiTabAction,
  val controllerComponents: MessagesControllerComponents,
  view: SubmittedNoReceiptView,
  clock: Clock,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with SubmissionViewDataSupport {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val ua = request.userAnswers

      ua.get(SubmissionConfirmationCachePage) match {
        case Some(cache) =>
          Future.successful(Ok(view(buildViewModelFromCache(cache, ua))))

        case None =>
          val monthlyReturnForEditRequest = GetMonthlyReturnForEditRequest.fromUserAnswers(ua)

          monthlyReturnForEditRequest match {
            case Left(error) =>
              logger.error(s"[SubmittedNoReceiptController] Failed to build GetMonthlyReturnForEditRequest: $error")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

            case Right(req) =>
              (for {
                monthlyReturn <- monthlyReturnService.retrieveMonthlyReturnForEditDetails(req)
                vm            <- buildViewModel(ua, monthlyReturn)
                uaWithCache   <- Future.fromTry(ua.set(SubmissionConfirmationCachePage, cacheFrom(vm)))
                _             <- monthlyReturnService.completeSubmissionJourney(uaWithCache)
              } yield Ok(view(vm))).recoverWith { case ex: UpstreamErrorResponse =>
                logger.error(
                  s"[SubmittedNoReceiptController] onPageLoad failed " +
                    s"with status ${ex.statusCode} for instanceId=${req.instanceId}",
                  ex
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
          }
      }
    }

  private def cacheFrom(vm: SubmittedNoReceiptViewModel): SubmissionConfirmationCache =
    SubmissionConfirmationCache(
      periodEnd = vm.periodEnd,
      contractorName = vm.contractorName,
      email = vm.email,
      submittedTime = vm.submittedTime,
      submittedDate = vm.submittedDate,
      submittedDateTimeIso = vm.submittedDateTimeIso
    )

  private def buildViewModelFromCache(cache: SubmissionConfirmationCache, ua: UserAnswers)(implicit
    request: CisIdDataRequest[_]
  ): SubmittedNoReceiptViewModel = {
    val submissionType      =
      required(ua.get(ReturnTypePage), "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
    val cisId               = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val empRef              = employerRefFrom(request)
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

    SubmittedNoReceiptViewModel(
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

  private def buildViewModel(ua: UserAnswers, monthlyReturn: GetAllMonthlyReturnDetailsResponse)(implicit
    request: CisIdDataRequest[_],
    hc: HeaderCarrier
  ): Future[SubmittedNoReceiptViewModel] = {
    val cisId               = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val contractorName      = monthlyReturn.scheme.headOption
      .flatMap(_.name)
      .map(_.trim)
      .filter(_.nonEmpty)
      .orElse(ua.get(ContractorNamePage).map(_.trim).filter(_.nonEmpty))
    val employerRef         = employerRefFrom(request)
    val submissionType      =
      required(ua.get(ReturnTypePage), "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
    implicit val lang: Lang = messagesApi.preferred(request).lang
    val periodEnd           = required(
      periodEndFromUserAnswers(ua),
      "[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers"
    ).format(DateTimeFormats.dateTimeFormat())

    val ukNow = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))

    resolveEmail(ua, cisId).map { email =>
      val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase
      val submittedDate = ukNow.format(DateTimeFormats.fullDateFormat())

      SubmittedNoReceiptViewModel(
        periodEnd = periodEnd,
        submittedTime = submittedTime,
        submittedDate = submittedDate,
        contractorName = contractorName,
        empRef = employerRef,
        email = email,
        submissionType = submissionType,
        cisId = cisId,
        submittedDateTimeIso = Some(ukNow.toString)
      )
    }
  }

  private def resolveEmail(ua: UserAnswers, cisId: String)(implicit hc: HeaderCarrier): Future[String] =
    if (ua.get(ConfirmationByEmailPage).contains(false)) {
      Future.successful("")
    } else {
      emailfromUserAnswers(ua) match {
        case Some(email) =>
          Future.successful(email)
        case None        =>
          monthlyReturnService
            .getSchemeEmail(cisId)
            .map(_.getOrElse(""))
            .recover { case ex =>
              logger.warn(s"[SubmittedNoReceipt] getSchemeEmail failed for cisId=$cisId, defaulting to empty", ex)
              ""
            }
      }
    }

  // Multi-tab: Display the submitted no receipt page for the ID-scoped journey.
  def onPageLoadWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val ua      = request.userAnswers
      val id      = flowId.toString
      val exitUrl = controllers.monthlyreturns.routes.ManageCisReturnController.onExitWithId(flowId).url

      ua.get(SubmissionConfirmationCachePage(id)) match {
        case Some(cache) =>
          Future.successful(Ok(view(buildViewModelFromCache(cache, ua, id), exitUrl)))

        case None =>
          val monthlyReturnForEditRequest = GetMonthlyReturnForEditRequest.fromUserAnswers(ua, id)

          monthlyReturnForEditRequest match {
            case Left(error) =>
              logger.error(s"[SubmittedNoReceiptController] Failed to build GetMonthlyReturnForEditRequest: $error")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

            case Right(req) =>
              (for {
                monthlyReturn <- monthlyReturnService.retrieveMonthlyReturnForEditDetails(req)
                vm            <- buildViewModel(ua, monthlyReturn, id)
                uaWithCache   <- Future.fromTry(ua.set(SubmissionConfirmationCachePage(id), cacheFrom(vm)))
                _             <- monthlyReturnService.completeSubmissionJourney(uaWithCache, id)
              } yield Ok(view(vm, exitUrl))).recoverWith { case ex: UpstreamErrorResponse =>
                logger.error(
                  s"[SubmittedNoReceiptController] onPageLoadWithId failed " +
                    s"with status ${ex.statusCode} for instanceId=${req.instanceId}",
                  ex
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
          }
      }
    }

  // Multi-tab: Build the submitted no receipt view model from the ID-scoped cache.
  private def buildViewModelFromCache(cache: SubmissionConfirmationCache, ua: UserAnswers, id: String)(implicit
    request: DataRequest[AnyContent]
  ): SubmittedNoReceiptViewModel = {
    val submissionType      =
      required(ua.get(ReturnTypePage(id)), "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
    val cisId               = required(ua.get(CisIdPage(id)), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val empRef              = employerRefFrom(request, id)
    implicit val lang: Lang = messagesApi.preferred(request).lang

    val periodEnd = ua
      .get(DateConfirmPaymentsPage(id))
      .map(_.format(DateTimeFormats.dateTimeFormat()))
      .getOrElse(cache.periodEnd)

    val legacyDateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.UK)
    val submittedDate = cache.submittedDateTimeIso
      .flatMap(iso => Try(ZonedDateTime.parse(iso)).toOption)
      .map(_.withZoneSameInstant(ZoneId.of("Europe/London")).format(DateTimeFormats.fullDateFormat()))
      .orElse(
        Try(java.time.LocalDate.parse(cache.submittedDate, legacyDateFmt)).toOption
          .map(_.format(DateTimeFormats.fullDateFormat()))
      )
      .getOrElse(cache.submittedDate)

    SubmittedNoReceiptViewModel(
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

  // Multi-tab: Build the submitted no receipt view model from ID-scoped answers.
  private def buildViewModel(ua: UserAnswers, monthlyReturn: GetAllMonthlyReturnDetailsResponse, id: String)(implicit
    request: DataRequest[AnyContent],
    hc: HeaderCarrier
  ): Future[SubmittedNoReceiptViewModel] = {
    val cisId               = required(ua.get(CisIdPage(id)), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val contractorName      = monthlyReturn.scheme.headOption
      .flatMap(_.name)
      .map(_.trim)
      .filter(_.nonEmpty)
      .orElse(ua.get(ContractorNamePage(id)).map(_.trim).filter(_.nonEmpty))
    val employerRef         = employerRefFrom(request, id)
    val submissionType      =
      required(ua.get(ReturnTypePage(id)), "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
    implicit val lang: Lang = messagesApi.preferred(request).lang
    val periodEnd           = required(
      periodEndFromUserAnswers(ua, id),
      "[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers"
    ).format(DateTimeFormats.dateTimeFormat())

    val ukNow = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))

    resolveEmail(ua, cisId, id).map { email =>
      val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase
      val submittedDate = ukNow.format(DateTimeFormats.fullDateFormat())

      SubmittedNoReceiptViewModel(
        periodEnd = periodEnd,
        submittedTime = submittedTime,
        submittedDate = submittedDate,
        contractorName = contractorName,
        empRef = employerRef,
        email = email,
        submissionType = submissionType,
        cisId = cisId,
        submittedDateTimeIso = Some(ukNow.toString)
      )
    }
  }

  // Multi-tab: Resolve the email address from ID-scoped answers.
  private def resolveEmail(ua: UserAnswers, cisId: String, id: String)(implicit hc: HeaderCarrier): Future[String] =
    if (ua.get(ConfirmationByEmailPage(id)).contains(false)) {
      Future.successful("")
    } else {
      emailfromUserAnswers(ua, id) match {
        case Some(email) =>
          Future.successful(email)
        case None        =>
          monthlyReturnService
            .getSchemeEmail(cisId)
            .map(_.getOrElse(""))
            .recover { case ex =>
              logger.warn(s"[SubmittedNoReceipt] getSchemeEmail failed for cisId=$cisId, defaulting to empty", ex)
              ""
            }
      }
    }
}
