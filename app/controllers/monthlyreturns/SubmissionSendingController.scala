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
import models.UserAnswers
import models.submission.ChrisSubmissionResponse
import pages.submission.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class SubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  submissionService: SubmissionService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      (for {
        created   <- submissionService.createAndTrack(request.userAnswers)
        submitted <- submissionService.submitToChris(created.submissionId, request.userAnswers)
        _         <- writeToFeMongo(request.userAnswers, created.submissionId, submitted)
        _         <- submissionService.updateSubmission(created.submissionId, request.userAnswers, submitted)
      } yield redirectForStatus(submitted.status))
        .recover { case ex =>
          logger.error("[Submission Sending] Create/Submit/Update flow failed", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  private def redirectForStatus(status: String): Result = status match {
    case "SUBMITTED" | "SUBMITTED_NO_RECEIPT" =>
      Redirect(controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad)
    case "FATAL_ERROR" | "DEPARTMENTAL_ERROR" =>
      Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad)
    case "PENDING" | "ACCEPTED"               =>
      Redirect(controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad)
    case _                                    =>
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
  }

  private def writeToFeMongo(
    ua: UserAnswers,
    submissionId: String,
    response: ChrisSubmissionResponse
  ): Future[Boolean] = {
    val updatedUa: Try[UserAnswers] = for {
      ua1 <- ua.set(SubmissionIdPage, submissionId)
      ua2 <- ua1.set(SubmissionStatusPage, response.status)
      ua3 <- ua2.set(IrMarkPage, response.hmrcMarkGenerated)
      ua4 <- response.responseEndPoint match {
               case Some(endpoint) =>
                 for {
                   u1 <- ua3.set(PollUrlPage, endpoint.url)
                   u2 <- u1.set(PollIntervalPage, endpoint.pollIntervalSeconds)
                 } yield u2
               case None           =>
                 Success(ua3)
             }
    } yield ua4

    updatedUa.fold(
      { err =>
        logger.error(s"[writeToFeMongo] Failed to update UserAnswers: ${err.getMessage}", err)
        Future.failed(err)
      },
      sessionRepository.set
    )
  }
}
