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
import models.requests.DataRequest
import pages.monthlyreturns.{CisIdPage, ReturnTypePage}
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

      for {
        vm <- buildViewModel(ua)
        _  <- monthlyReturnService.completeSubmissionJourney(ua)
      } yield Ok(view(vm))
    }

  private def buildViewModel(ua: UserAnswers)(implicit request: DataRequest[_]): Future[SubmittedNoReceiptViewModel] = {
    val cisId          = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")
    val contractorName = contractorNameFrom(request)
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
