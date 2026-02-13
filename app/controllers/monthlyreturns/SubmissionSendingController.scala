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
import models.submission.SubmissionDetails
import pages.submission.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.SubmissionSendingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  submissionService: SubmissionService,
  view: SubmissionSendingView,
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
        created   <- submissionService.create(request.userAnswers)
        submitted <-
          submissionService.submitToChrisAndPersist(created.submissionId, request.userAnswers, request.isAgent)
        _         <- submissionService.updateSubmission(created.submissionId, request.userAnswers, submitted)
      } yield submitted.status match {
        case "PENDING" | "ACCEPTED" =>
          Redirect(controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect)
        case _                      => Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad)
      }).recover { case ex =>
        logger.error("[Submission Sending] Create/Submit/Update flow failed", ex)
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
    }

  def onPollAndRedirect: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val pollInterval = submissionService.getPollInterval(request.userAnswers).toString

      request.userAnswers.get(SubmissionDetailsPage) match {
        case None                   => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(submissionStatus) =>
          submissionService.checkAndUpdateSubmissionStatus(request.userAnswers).map {
            case "PENDING" | "ACCEPTED"               => Ok(view()).withHeaders("Refresh" -> pollInterval)
            case "TIMED_OUT"                          => Redirect(routes.SubmissionAwaitingController.onPageLoad)
            case "SUBMITTED"                          => Redirect(routes.SubmissionSuccessController.onPageLoad)
            case "SUBMITTED_NO_RECEIPT"               => Redirect(routes.SubmittedNoReceiptController.onPageLoad)
            case "DEPARTMENTAL_ERROR" | "FATAL_ERROR" => Redirect(routes.SubmissionUnsuccessfulController.onPageLoad)
            case _                                    => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      }
    }
}
