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
import models.submission.SubmissionDetails
import pages.monthlyreturns.*
import pages.submission.SubmissionDetailsPage
import models.ReturnType.reads
import models.requests.DataRequest
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmissionSuccessView
import utils.IrMarkReferenceGenerator
import viewmodels.checkAnswers.monthlyreturns.SubmissionSuccessViewModel

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SubmissionSuccessView,
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

  private def buildViewModel(ua: UserAnswers)(implicit
    request: DataRequest[_],
    hc: HeaderCarrier
  ): Future[SubmissionSuccessViewModel] = {
    val cisId = required(ua.get(CisIdPage), "[SubmissionSuccess] cisId missing from userAnswers")

    val submissionType =
      required(ua.get(ReturnTypePage), "[SubmissionSuccess] ReturnTypePage missing from userAnswers")

    val periodEnd = required(
      periodEndFromUserAnswers(ua),
      "[SubmissionSuccess] taxPeriodEnd missing from userAnswers"
    )

    val reference = IrMarkReferenceGenerator.fromBase64(
      required(ua.get(SubmissionDetailsPage), "[SubmissionSuccess] submissionDetails missing from userAnswers").irMark
    )

    val contractorName = contractorNameFrom(request)
    val empRef         = employerRefFrom(request)

    resolveEmail(ua, cisId).map { email =>
      val monthYearFmt = DateTimeFormatter.ofPattern("MMMM uuuu")
      val dateFmt      = DateTimeFormatter.ofPattern("d MMMM uuuu")
      val timeFmt      = DateTimeFormatter.ofPattern("h:mma")
      val nowUk        = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))

      SubmissionSuccessViewModel(
        reference = reference,
        periodEnd = periodEnd.format(monthYearFmt),
        submittedTime = nowUk.format(timeFmt).toLowerCase,
        submittedDate = nowUk.format(dateFmt),
        contractorName = contractorName,
        empRef = empRef,
        email = email,
        submissionType = submissionType,
        cisId = cisId
      )
    }
  }

  private def resolveEmail(ua: UserAnswers, cisId: String)(implicit
    hc: HeaderCarrier
  ): Future[String] =
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
