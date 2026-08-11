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
import models.requests.{CisIdDataRequest, GetMonthlyReturnForEditRequest}
import pages.monthlyreturns.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import viewmodels.checkAnswers.monthlyreturns.SubmittedNoReceiptViewModel
import views.html.monthlyreturns.SubmittedNoReceiptView

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmittedNoReceiptController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
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
              for {
                monthlyReturn <- monthlyReturnService.retrieveMonthlyReturnForEditDetails(req)
                vm            <- buildViewModel(ua, monthlyReturn)
                uaWithCache   <- Future.fromTry(ua.set(SubmissionConfirmationCachePage, cacheFrom(vm)))
                _             <- monthlyReturnService.completeSubmissionJourney(uaWithCache)
              } yield Ok(view(vm))
          }
      }
    }

  private def cacheFrom(vm: SubmittedNoReceiptViewModel): SubmissionConfirmationCache =
    SubmissionConfirmationCache(
      periodEnd = vm.periodEnd,
      contractorName = vm.contractorName,
      email = vm.email,
      submittedTime = vm.submittedTime,
      submittedDate = vm.submittedDate
    )

  private def buildViewModelFromCache(cache: SubmissionConfirmationCache, ua: UserAnswers)(implicit
    request: CisIdDataRequest[_]
  ): SubmittedNoReceiptViewModel = {
    val submissionType =
      required(ua.get(ReturnTypePage), "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
    val cisId          = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val empRef         = employerRefFrom(request)

    SubmittedNoReceiptViewModel(
      periodEnd = cache.periodEnd,
      submittedTime = cache.submittedTime,
      submittedDate = cache.submittedDate,
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
    val cisId          = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val contractorName = monthlyReturn.scheme.headOption
      .flatMap(_.name)
      .map(_.trim)
      .filter(_.nonEmpty)
      .getOrElse(throw new RuntimeException("[SubmittedNoReceipt] Scheme name is missing"))
    val employerRef    = employerRefFrom(request)
    val submissionType =
      required(ua.get(ReturnTypePage), "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
    val periodEnd      = required(
      periodEndFromUserAnswers(ua),
      "[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers"
    ).format(DateTimeFormatter.ofPattern("MMMM uuuu"))

    resolveEmail(ua, cisId).map { email =>
      val ukNow         = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
      val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase
      val submittedDate = ukNow.format(DateTimeFormatter.ofPattern("d MMMM uuuu"))

      SubmittedNoReceiptViewModel(
        periodEnd = periodEnd,
        submittedTime = submittedTime,
        submittedDate = submittedDate,
        contractorName = contractorName,
        empRef = employerRef,
        email = email,
        submissionType = submissionType,
        cisId = cisId
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
}
