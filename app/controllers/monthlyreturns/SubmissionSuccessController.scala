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
import models.EmployerReference
import pages.monthlyreturns.{ConfirmEmailAddressPage, DateConfirmNilPaymentsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmissionSuccessView

import java.time.{Clock, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class SubmissionSuccessController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SubmissionSuccessView,
  clock: Clock
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def formatEmployerRef(er: EmployerReference): String =
    s"${er.taxOfficeNumber}/${er.taxOfficeReference}"

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val email: String = request.userAnswers.get(ConfirmEmailAddressPage).get
    val periodEnd     = request.userAnswers.get(DateConfirmNilPaymentsPage).get
    val periodStart   = periodEnd.minusMonths(1).withDayOfMonth(6)

    val dmyFmt        = DateTimeFormatter.ofPattern("d MMM uuuu")
    val periodRange   = s"${periodStart.format(dmyFmt)} to ${periodEnd.format(dmyFmt)}"
    val ukNow         = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
    val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("HH:mm z"))
    val submittedDate = ukNow.format(dmyFmt)

    val employerRef: String =
      request.employerReference
        .map(formatEmployerRef)
        .getOrElse {
          val msg = s"SubmissionSuccess: employerReference missing for userId=${request.userId}"
          logger.error(msg)
          throw new IllegalStateException(msg)
        }
    val reference           = "KCNJQEFYOYVU6C2BTZCDQSWUSGG5ODG"
    val contractorName      = "PAL 355 Scheme"

    Ok(
      view(
        reference = reference,
        periodRangeText = periodRange,
        submittedTime = submittedTime,
        submittedDate = submittedDate,
        contractorName = contractorName,
        empRef = employerRef,
        email = email
      )
    )
  }
}
