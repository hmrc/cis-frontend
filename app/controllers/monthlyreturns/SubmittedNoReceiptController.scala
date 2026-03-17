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
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{CisIdPage, ConfirmEmailAddressPage, ContractorNamePage, DateConfirmPaymentsPage, ReturnTypePage}
import play.api.Logging
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
    with Logging {

  private def formatEmployerRef(er: EmployerReference): String =
    s"${er.taxOfficeNumber}/${er.taxOfficeReference}"

  private def fail(errorMessage: String): Nothing = {
    logger.error(errorMessage)
    throw new IllegalStateException(errorMessage)
  }

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val cisId = request.userAnswers.get(CisIdPage).getOrElse {
        logger.error("[SubmittedNoReceipt] cisId missing from userAnswers")
        throw new IllegalStateException("cisId missing from userAnswers")
      }

      val contractorName: String = {
        val errorMessage: String = s"[SubmittedNoReceipt] contractorName missing for userId=${request.userId}"
        if (!request.isAgent) {
          request.userAnswers.get(ContractorNamePage).getOrElse {
            fail(errorMessage)
          }
        } else {
          request.userAnswers.get(AgentClientDataPage).flatMap(_.schemeName).getOrElse {
            fail(errorMessage)
          }
        }
      }

      val employerRef: String = {
        val errorMessage: String = s"[SubmissionSuccess] employerReference missing for userId=${request.userId}"
        if (!request.isAgent) {
          request.employerReference.map(formatEmployerRef).getOrElse {
            fail(errorMessage)
          }
        } else {
          request.userAnswers
            .get(AgentClientDataPage)
            .filter(_.taxOfficeNumber.nonEmpty)
            .map(data => formatEmployerRef(EmployerReference(data.taxOfficeNumber, data.taxOfficeReference)))
            .getOrElse {
              fail(errorMessage)
            }
        }
      }

      val emailFromSession = request.userAnswers.get(ConfirmEmailAddressPage)

      val emailFuture = emailFromSession match {
        case Some(email) => Future.successful(email)
        case None        => monthlyReturnService.getSchemeEmail(cisId).map(_.getOrElse(""))
      }

      emailFuture.map { email =>
        val dmyFmt         = DateTimeFormatter.ofPattern("MMMM uuuu")
        val periodEnd      = request.userAnswers
          .get(DateConfirmPaymentsPage)
          .map(_.format(dmyFmt))
          .getOrElse {
            logger.error("[SubmittedNoReceipt] taxPeriodEnd missing from userAnswers")
            throw new IllegalStateException("taxPeriodEnd missing from userAnswers")
          }
        val ukNow          = ZonedDateTime.now(clock).withZoneSameInstant(ZoneId.of("Europe/London"))
        val submittedTime  = ukNow.format(DateTimeFormatter.ofPattern("h:mma")).toLowerCase
        val submittedDate  = ukNow.format(DateTimeFormatter.ofPattern("d MMMM uuuu"))
        val submissionType = request.userAnswers
          .get(ReturnTypePage)
          .getOrElse {
            logger.error("[SubmittedNoReceipt] ReturnTypePage missing from userAnswers")
            throw new IllegalStateException("ReturnTypePage missing from userAnswers")
          }

        Ok(
          view(
            periodEnd = periodEnd,
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
