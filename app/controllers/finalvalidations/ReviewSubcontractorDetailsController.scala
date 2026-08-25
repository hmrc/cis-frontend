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

package controllers.finalvalidations

import controllers.actions.*
import models.requests.GetMonthlyReturnForEditRequest
import pages.validation.SubcontractorValidationFailuresPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.finalvalidations.ReviewSubcontractorDetailsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReviewSubcontractorDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ReviewSubcontractorDetailsView,
  monthlyReturnService: MonthlyReturnService
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      given HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val validationFailures =
        request.userAnswers
          .get(SubcontractorValidationFailuresPage)
          .filter(_.nonEmpty)

      (
        validationFailures,
        GetMonthlyReturnForEditRequest.fromUserAnswers(request.userAnswers)
      ) match {
        case (None, _) =>
          logger.warn(
            "[ReviewSubcontractorDetailsController.onPageLoad] Missing subcontractor validation failures"
          )

          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )

        case (_, Left(error)) =>
          logger.warn(
            s"[ReviewSubcontractorDetailsController.onPageLoad] Failed to build monthly-return request: $error"
          )

          Future.successful(
            Redirect(
              controllers.routes.JourneyRecoveryController.onPageLoad()
            )
          )

        case (Some(failures), Right(monthlyReturnRequest)) =>
          monthlyReturnService
            .retrieveMonthlyReturnForEditDetails(monthlyReturnRequest)
            .map { response =>
              val namesBySubcontractorId =
                response.subcontractors.map { subcontractor =>
                  subcontractor.subcontractorId ->
                    subcontractor.displayName
                      .map(_.trim)
                      .filter(_.nonEmpty)
                }.toMap

              val failedSubcontractorNames =
                failures.map { failure =>
                  namesBySubcontractorId
                    .get(failure.subcontractorId)
                    .flatten
                    .getOrElse("No name provided")
                }

              Ok(view(failedSubcontractorNames))
            }
            .recover { case error =>
              logger.error(
                "[ReviewSubcontractorDetailsController.onPageLoad] Failed to retrieve subcontractor details",
                error
              )

              Redirect(
                controllers.routes.SystemErrorController.onPageLoad()
              )
            }
      }
    }
}
