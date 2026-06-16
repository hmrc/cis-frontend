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
import forms.monthlyreturns.SubcontractorDetailsAddedFormProvider
import models.Mode
import models.requests.GetMonthlyReturnForEditRequest
import pages.amend.AmendmentDetailsPage
import pages.monthlyreturns.{AllSubcontractorDetailsAdded, CisIdPage, DateConfirmPaymentsPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.SubcontractorDetailsAddedBuilder
import views.html.monthlyreturns.SubcontractorDetailsAddedView
import services.MonthlyReturnService

import scala.concurrent.Future
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class SubcontractorDetailsAddedController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  formProvider: SubcontractorDetailsAddedFormProvider,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: SubcontractorDetailsAddedView,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val ua = request.userAnswers

      val requiredAnswers = for {
        cisId   <- ua.get(CisIdPage)
        taxDate <- ua.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

      requiredAnswers match {
        case Some((cisId, month, year)) =>
          val isAmendment = ua.get(AmendmentDetailsPage).isDefined

          monthlyReturnService
            .retrieveMonthlyReturnForEditDetails(
              GetMonthlyReturnForEditRequest(
                instanceId = cisId,
                taxMonth = month,
                taxYear = year,
                isAmendment = isAmendment
              )
            )
            .map { returns =>
              returns.monthlyReturn.headOption.flatMap(_.status) match {
                case Some("STARTED" | "VALIDATED") =>
                  SubcontractorDetailsAddedBuilder.build(ua) match {
                    case Some(viewModel) =>
                      Ok(view(form, mode, viewModel))

                    case None =>
                      Redirect(controllers.routes.SystemErrorController.onPageLoad())
                  }

                case _ =>
                  Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
            }

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      SubcontractorDetailsAddedBuilder.build(request.userAnswers) match {
        case None =>
          Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))

        case Some(viewModel) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, viewModel))),
              isAddingMoreSubcontractors =>
                val allSubcontractorDetailsAdded = !isAddingMoreSubcontractors

                val updatedUa =
                  request.userAnswers
                    .set(AllSubcontractorDetailsAdded, allSubcontractorDetailsAdded)
                    .getOrElse(request.userAnswers)

                sessionRepository.set(updatedUa).map { _ =>
                  if (allSubcontractorDetailsAdded && viewModel.hasIncomplete) {
                    val withError =
                      form
                        .fill(isAddingMoreSubcontractors)
                        .withError("value", "monthlyreturns.subcontractorDetailsAdded.error.incomplete")
                    BadRequest(view(withError, mode, viewModel))
                  } else if (allSubcontractorDetailsAdded) {
                    Redirect(controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController.onPageLoad())
                  } else {
                    Redirect(
                      controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
                    )
                  }
                }
            )
      }
    }

  def onCancelAmendment(): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId) { implicit request =>
      Redirect(controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoad())
    }
}
