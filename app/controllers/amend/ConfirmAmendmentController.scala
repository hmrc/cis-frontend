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
import models.monthlyreturns.ContinueReturnJourneyQueryParams
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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

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

  def onPageLoad(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] = identify.async {
    implicit request =>
      val amendmentDetails = AmendmentDetails(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth
      )

      val updatedUserAnswers =
        UserAnswers(request.userId)
          .set(CisIdPage, queryParams.instanceId)
          .flatMap(_.set(AmendmentDetailsPage, amendmentDetails))
          .get

      sessionRepository.set(updatedUserAnswers).map { _ =>
        Ok(view())
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
            } yield Redirect(
              controllers.amend.routes.ConfirmAmendmentController.onPageLoad(
                ContinueReturnJourneyQueryParams(
                  instanceId = amendmentDetails.instanceId,
                  taxYear = amendmentDetails.taxYear,
                  taxMonth = amendmentDetails.taxMonth
                )
              )
            ) // TODO: DTR-4657
          ).recover { case ex =>
            logger.warn(
              s"[ConfirmAmendmentController] Failed to create amended monthly return for instanceId ${amendmentDetails.instanceId}",
              ex
            )
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

        case None =>
          logger.warn(
            s"[ConfirmAmendmentController] AmendmentDetails missing from userAnswers}"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      }
    }
}
