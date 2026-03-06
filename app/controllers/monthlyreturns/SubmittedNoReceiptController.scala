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
import pages.monthlyreturns.{CisIdPage, ReturnTypePage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
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
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with SubmissionViewDataSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val ua = request.userAnswers

      val cisId = required(ua.get(CisIdPage), "[SubmittedNoReceipt] cisId missing from userAnswers")

      val submissionType =
        ua.get(ReturnTypePage).getOrElse(fail("[SubmittedNoReceipt] ReturnTypePage missing from userAnswers"))

      val periodEnd = required(
        periodEndFromUserAnswers(ua, submissionType),
        s"[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers for submissionType $submissionType"
      )

      val emailFuture: Future[String] = emailfromUserAnswers(ua, submissionType)
        .map(Future.successful)
        .getOrElse(monthlyReturnService.getSchemeEmail(cisId).map(_.getOrElse("")))

      for {
        email <- emailFuture
      } yield {
        val dmyFmt        = DateTimeFormatter.ofPattern("d MMM uuuu")
        val ukNow         = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
        val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase
        val submittedDate = ukNow.format(dmyFmt)
        Ok(
          view(
            periodEnd = periodEnd.format(dmyFmt),
            submittedTime = submittedTime,
            submittedDate = submittedDate,
            contractorName = contractorNameFrom(request),
            empRef = employerRefFrom(request),
            email = email
          )
        )
      }
  }
}
