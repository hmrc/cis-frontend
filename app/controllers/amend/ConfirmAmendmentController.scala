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

package controllers.amend

import controllers.actions.*
import models.UserAnswers
import models.amend.{AmendmentDetails, CreateAmendedMonthlyReturnRequest}
import pages.amend.{AmendmentDetailsPage, ConfirmAmendmentPage}
import pages.monthlyreturns.CisIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.AmendMonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.ConfirmAmendmentView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ConfirmAmendmentController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmAmendmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val firstDate: Instant  = Instant.parse("2016-02-05T00:00:00Z")
  private val secondDate: Instant = Instant.parse("2016-04-06T00:00:00Z")

  private def isInWarningPeriod(acceptedTime: Option[String]): Boolean =
    acceptedTime
      .flatMap(t => Try(Instant.parse(t)).toOption)
      .exists(instant => !instant.isBefore(firstDate) && instant.isBefore(secondDate))

  def onPageLoad(handoffId: String): Action[AnyContent] = identify.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    amendMonthlyReturnService
      .getAmendmentHandoff(handoffId)
      .flatMap {
        case Some(handoff) =>
          val amendmentDetails = AmendmentDetails(
            instanceId = handoff.instanceId,
            taxYear = handoff.taxYear,
            taxMonth = handoff.taxMonth,
            returnType = handoff.returnType,
            acceptedTime = handoff.acceptedTime
          )

          val updatedUserAnswers =
            UserAnswers(request.userId)
              .set(CisIdPage, handoff.instanceId)
              .flatMap(_.set(AmendmentDetailsPage, amendmentDetails))
              .get

          val showWarning = isInWarningPeriod(handoff.acceptedTime)

          sessionRepository.set(updatedUserAnswers).map { _ =>
            Ok(view(showWarning))
          }

        case None =>
          logger.warn(s"[ConfirmAmendmentController] No handoff found for handoffId: $handoffId")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit: Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      request.userAnswers.flatMap(_.get(AmendmentDetailsPage)) match {
        case Some(amendmentDetails) =>
          implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

          val createRequest = CreateAmendedMonthlyReturnRequest(
            instanceId = amendmentDetails.instanceId,
            taxYear = amendmentDetails.taxYear,
            taxMonth = amendmentDetails.taxMonth,
            version = 0
          )

          (
            for {
              _             <- amendMonthlyReturnService.createAmendedMonthlyReturn(createRequest)
              updatedAnswers = request.userAnswers.get.set(ConfirmAmendmentPage, true).get
              _             <- sessionRepository.set(updatedAnswers)
            } yield Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()) // TODO: DTR-4657
          ).recover { case ex =>
            logger.warn(
              s"[ConfirmAmendmentController] Failed to create amended monthly return for instanceId ${amendmentDetails.instanceId}",
              ex
            )
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

        case None =>
          logger.warn(
            s"[ConfirmAmendmentController] AmendmentDetails missing from userAnswers"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
