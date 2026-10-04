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
import models.ReturnType.reads
import models.monthlyreturns.{GetAllMonthlyReturnDetailsResponse, SubmissionConfirmationCache}
import models.requests.{CisPath, GetMonthlyReturnForEditRequest, SchemeRequest}
import models.{ReturnType, UserAnswers}
import pages.monthlyreturns.*
import pages.submission.SubmissionDetailsPage
import play.api.i18n.{I18nSupport, Lang}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import services.guard.SubmissionSuccessfulServiceGuard
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.{DateTimeFormats, IrMarkReferenceGenerator}
import viewmodels.checkAnswers.monthlyreturns.SubmissionSuccessViewModel
import views.html.monthlyreturns.SubmissionSuccessView

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionSuccessController @Inject() (
  identify: IdentifierAction,
  resolveScheme: SchemeAction,
  getJourney: MonthlyReturnAction,
  val controllerComponents: MessagesControllerComponents,
  view: SubmissionSuccessView,
  clock: Clock,
  monthlyReturnService: MonthlyReturnService,
  submissionSuccessGuard: SubmissionSuccessfulServiceGuard
)(implicit ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with SubmissionViewDataSupport {

  def onPageLoad(cisPath: CisPath): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney).async { implicit request =>
      if (!submissionSuccessGuard.check) {
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } else {
        val ua = request.userAnswers

        ua.get(SubmissionConfirmationCachePage) match {
          case Some(cache) =>
            Future.successful(Ok(view(buildViewModelFromCache(cache, ua))))

          case None =>
            val monthlyReturnForEditRequest = GetMonthlyReturnForEditRequest.fromUserAnswers(request.cisId, ua)

            monthlyReturnForEditRequest match {
              case Left(error) =>
                logger.error(s"[SubmissionSuccessController] Failed to build GetMonthlyReturnForEditRequest: $error")
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

              case Right(req) =>
                for {
                  monthlyReturn <- monthlyReturnService.retrieveMonthlyReturnForEditDetails(req)
                  vm            <- buildViewModel(ua, monthlyReturn)
                  uaWithCache   <- Future.fromTry(ua.set(SubmissionConfirmationCachePage, cacheFrom(vm)))
                  _             <- monthlyReturnService.completeSubmissionJourney(uaWithCache)
                } yield Ok(view(vm))
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

  private def buildViewModelFromCache(cache: SubmissionConfirmationCache, ua: UserAnswers)(implicit
    request: SchemeRequest[?]
  ): SubmissionSuccessViewModel = {
    val reference           = IrMarkReferenceGenerator.fromBase64(
      required(ua.get(SubmissionDetailsPage), "[SubmissionSuccess] submissionDetails missing from userAnswers").irMark
    )
    val submissionType      = required(ua.get(ReturnTypePage), "[SubmissionSuccess] ReturnTypePage missing from userAnswers")
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
      empRef = request.employerRef,
      email = cache.email,
      submissionType = submissionType,
      cisId = request.cisId
    )
  }

  private def buildViewModel(ua: UserAnswers, monthlyReturn: GetAllMonthlyReturnDetailsResponse)(using
    request: SchemeRequest[_]
  ): Future[SubmissionSuccessViewModel] = {
    val submissionType = required(ua.get(ReturnTypePage), "[SubmissionSuccess] ReturnTypePage missing from userAnswers")

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

    resolveEmail(ua, request.cisId).map { email =>
      SubmissionSuccessViewModel(
        reference = reference,
        periodEnd = periodEnd.format(DateTimeFormats.dateTimeFormat()),
        submittedTime = nowUk.format(timeFmt).toLowerCase,
        submittedDate = nowUk.format(DateTimeFormats.fullDateFormat()),
        contractorName = contractorName,
        empRef = request.employerRef,
        email = email,
        submissionType = submissionType,
        cisId = request.cisId,
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
              logger.warn(s"[SubmissionSuccess] getSchemeEmail failed for cisId=$cisId, defaulting to empty", ex)
              ""
            }
        )
    }
}
