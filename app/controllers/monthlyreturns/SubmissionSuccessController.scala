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
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, ContractorNamePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SubmissionSuccessView

import java.time.{Clock, LocalDate, ZoneId, ZonedDateTime}
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
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private def formatEmployerRef(er: EmployerReference): String =
    s"${er.taxOfficeNumber}/${er.taxOfficeReference}"

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val cisId = request.userAnswers.get(CisIdPage).getOrElse {
        logger.error("[SubmissionSuccess] cisId missing from userAnswers")
        throw new IllegalStateException("cisId missing from userAnswers")
      }

      val contractorName = request.userAnswers.get(ContractorNamePage).getOrElse {
        logger.error("[SubmissionSuccess] contractorName missing from userAnswers")
        throw new IllegalStateException("contractorName missing from userAnswers")
      }

      val employerRef: String =
        request.employerReference
          .map(formatEmployerRef)
          .getOrElse {
            val msg = s"SubmissionSuccess: employerReference missing for userId=${request.userId}"
            logger.error(msg)
            throw new IllegalStateException(msg)
          }

      val emailFromSession = request.userAnswers.get(ConfirmEmailAddressPage)

      val emailFuture = emailFromSession match {
        case Some(email) => Future.successful(email)
        case None        => monthlyReturnService.getSchemeEmail(cisId).map(_.getOrElse(""))
      }

      emailFuture.map { email =>
        val dmyFmt        = DateTimeFormatter.ofPattern("d MMM uuuu")
        val periodEnd     = LocalDate.of(2018, 3, 5).format(dmyFmt)
        val ukNow         = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
        val submittedTime = ukNow.format(DateTimeFormatter.ofPattern("HH:mm z"))
        val submittedDate = ukNow.format(dmyFmt)
        val reference     = "KCNJQEFYOYVU6C2BTZCDQSWUSGG5ODG"

        Ok(
          view(
            reference = reference,
            periodEnd = periodEnd,
            submittedTime = submittedTime,
            submittedDate = submittedDate,
            contractorName = contractorName,
            empRef = employerRef,
            email = email
          )
        )
      }
  }
}
