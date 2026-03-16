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
import models.ReturnType
import models.submission.SubmissionDetails
import pages.monthlyreturns.*
import pages.submission.SubmissionDetailsPage
import models.ReturnType.reads
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmissionSuccessView
import utils.IrMarkReferenceGenerator

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

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val ua = request.userAnswers

      val cisId = required(ua.get(CisIdPage), "[SubmissionSuccess] cisId missing from userAnswers")

      val submissionType =
        ua.get(ReturnTypePage).getOrElse(fail("[SubmissionSuccess] ReturnTypePage missing from userAnswers"))

      val periodEnd = required(
        periodEndFromUserAnswers(ua, submissionType),
        s"[SubmissionSuccess] taxPeriodEnd missing from userAnswers for submissionType $submissionType"
      )

      val reference = IrMarkReferenceGenerator.fromBase64(
        required(ua.get(SubmissionDetailsPage), "[SubmissionSuccess] submissionDetails missing from userAnswers").irMark
      )

      val emailFuture: Future[String] = emailfromUserAnswers(ua, submissionType)
        .map(Future.successful)
        .getOrElse(monthlyReturnService.getSchemeEmail(cisId).map(_.getOrElse("")))

      for {
        email <- emailFuture
      } yield {
        val myFmt         = DateTimeFormatter.ofPattern("MMMM uuuu")
        val dmyFmt        = DateTimeFormatter.ofPattern("d MMMM uuuu")
        val ukNow         = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
        val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase
        val submittedDate = ukNow.format(dmyFmt)
        Ok(
          view(
            reference = reference,
            periodEnd = periodEnd.format(myFmt),
            submittedTime = submittedTime,
            submittedDate = submittedDate,
            contractorName = contractorNameFrom(request),
            empRef = employerRefFrom(request),
            email = email,
            submissionType = submissionType,
            cisId = cisId
          )
        )
      }
  }
}
