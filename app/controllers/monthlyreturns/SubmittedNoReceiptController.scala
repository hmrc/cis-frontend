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
import models.ReturnType
import pages.monthlyreturns.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import services.guard.SubmissionSuccessfulCheck.{GuardFailed, GuardPassed}
import services.guard.SubmissionSuccessfulServiceGuard
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.SubmittedNoReceiptView

import java.time.format.DateTimeFormatter
import java.time.{Clock, ZoneId, ZonedDateTime}
import java.util.Locale
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
  monthlyReturnService: MonthlyReturnService,
  submissionSuccessGuard: SubmissionSuccessfulServiceGuard
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with SubmissionViewDataSupport {

  private val ukZone: ZoneId = ZoneId.of("Europe/London")

  private val monthYearFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM uuuu", Locale.UK)

  private val dayMonthYearFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK)

  private val submittedTimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mma", Locale.UK)

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      submissionSuccessGuard.check.flatMap {
        case GuardFailed =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case GuardPassed =>
          renderPage
      }
    }

  private def renderPage(implicit
    request: models.requests.DataRequest[_],
    hc: HeaderCarrier
  ): Future[play.api.mvc.Result] = {
    val ua = request.userAnswers

    val cisId = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")

    val submissionType = required(
      ua.get(ReturnTypePage),
      "[SubmittedNoReceipt] ReturnTypePage missing from userAnswers"
    )

    val periodEnd = required(
      periodEndFromUserAnswers(ua, submissionType),
      s"[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers for submissionType $submissionType"
    )

    val contractorName = contractorNameFrom(request)
    val employerRef    = employerRefFrom(request)

    val ukNow = ZonedDateTime.now(clock).withZoneSameInstant(ukZone)

    val emailF: Future[String] =
      emailfromUserAnswers(ua, submissionType).fold {
        monthlyReturnService
          .getSchemeEmail(cisId)
          .map(_.getOrElse(""))
          .recover { case ex =>
            logger.warn(s"[SubmittedNoReceipt] getSchemeEmail failed for cisId=$cisId, defaulting to empty", ex)
            ""
          }
      }(Future.successful)

    emailF.map { email =>
      val submittedTime = ukNow.format(submittedTimeFmt).toLowerCase(Locale.UK)
      val submittedDate = ukNow.format(dayMonthYearFmt)

      Ok(
        view(
          periodEnd = periodEnd.format(monthYearFmt),
          submittedTime = submittedTime,
          submittedDate = submittedDate,
          contractorName = contractorName,
          empRef = employerRef,
          email = email,
          submissionType = submissionType
        )
      )
    }
  }
}
