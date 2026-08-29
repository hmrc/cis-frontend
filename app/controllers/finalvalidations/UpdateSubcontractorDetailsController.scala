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
import models.finalvalidation.{UpdateSubcontractorDetailsPageModel, UpdateSubcontractorDetailsPageModelBuilder}
import models.requests.GetMonthlyReturnForEditRequest
import pages.finalvalidations.FinalValidationErrorPage
import pages.monthlyreturns.SelectedSubcontractorPage
import services.MonthlyReturnService

import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.finalvalidations.UpdateSubcontractorDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateSubcontractorDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  monthlyReturnService: MonthlyReturnService,
  pageModelBuilder: UpdateSubcontractorDetailsPageModelBuilder,
  val controllerComponents: MessagesControllerComponents,
  view: UpdateSubcontractorDetailsView
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(subcontractorId: Long): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      val failure =
        request.userAnswers
          .get(FinalValidationErrorPage)
          .flatMap(_.find(_.subcontractorId == subcontractorId))

      val selectedSubcontractor =
        request.userAnswers
          .get(SelectedSubcontractorPage.all)
          .flatMap(_.values.find(_.id == subcontractorId))

      val monthlyReturnRequest =
        GetMonthlyReturnForEditRequest.fromUserAnswers(request.userAnswers)

      (failure, selectedSubcontractor, monthlyReturnRequest) match {

        case (Some(validationFailure), Some(selected), Right(editRequest)) =>
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(editRequest).map { response =>

            val subcontractor = response.subcontractors.find(_.subcontractorId == subcontractorId)

            subcontractor match {

              case Some(fullSubcontractor) =>
                val rows =
                  pageModelBuilder.build(
                    fullSubcontractor,
                    validationFailure,
                    (field, target) =>
                      routes.FinalValidationChangeController.onPageLoad(subcontractorId, field.key, target.key).url
                  )

                val model =
                  UpdateSubcontractorDetailsPageModel(
                    subcontractorId = subcontractorId,
                    subcontractorName = selected.name,
                    rows = rows
                  )

                Ok(view(model))

              case None =>
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
          }

        case _ =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
