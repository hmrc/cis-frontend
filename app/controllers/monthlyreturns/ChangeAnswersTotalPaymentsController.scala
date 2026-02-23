/*
 * Copyright 2026 HM Revenue & Customs
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
import models.{NormalMode, UserAnswers}
import models.monthlyreturns.{SelectedSubcontractor, UpdateMonthlyReturnItemRequest}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.ChangeAnswersTotalPaymentsViewModel
import views.html.monthlyreturns.ChangeAnswersTotalPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ChangeAnswersTotalPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeAnswersTotalPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(index: Int): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(SelectedSubcontractorPage(index)) match {
      case None                => Redirect(controllers.routes.SystemErrorController.onPageLoad())
      case Some(subcontractor) =>
        Ok(view(ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor), index))
    }
  }

  def onSubmit(index: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val ua = request.userAnswers

      buildUpdatePayload(ua, index) match {
        case None          =>
          Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))
        case Some(payload) =>
          monthlyReturnService
            .updateMonthlyReturnItem(payload)
            .flatMap { _ =>
              sessionRepository.set(ua).map { _ =>
                Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))
              }
            }
            .recover {
              case u: UpstreamErrorResponse =>
                logger.error(
                  s"[ChangeAnswersTotalPaymentsController][onSubmit] - updateMonthlyReturnItems failed: status: ${u.statusCode}, " +
                    s"index: $index, subcontractorId: ${payload.subcontractorId}, message: ${u.message}"
                )
                Redirect(controllers.routes.SystemErrorController.onPageLoad())

              case NonFatal(e) =>
                logger.error(
                  s"[ChangeAnswersTotalPaymentsController][onSubmit] - updateMonthlyReturnItems failed: index: $index, " +
                    s"subcontractorId: ${payload.subcontractorId}, message: ${e.getMessage}",
                  e
                )
                Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
      }
  }

  private def buildUpdatePayload(ua: UserAnswers, index: Int): Option[UpdateMonthlyReturnItemRequest] =
    for {
      instanceId    <- ua.get(CisIdPage)
      monthYear     <- ua.get(DateConfirmPaymentsPage)
      subcontractor <- ua.get(SelectedSubcontractorPage(index))
    } yield UpdateMonthlyReturnItemRequest(
      instanceId = instanceId,
      taxYear = monthYear.getYear,
      taxMonth = monthYear.getMonthValue,
      subcontractorId = subcontractor.id,
      subcontractorName = subcontractor.name,
      totalPayments = subcontractor.totalPaymentsMade.toString,
      costOfMaterials = subcontractor.costOfMaterials.toString,
      totalDeducted = subcontractor.totalTaxDeducted.toString
    )
}
