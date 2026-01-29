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

import config.FrontendAppConfig
import controllers.actions.*
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.monthlyreturns.{SelectSubcontractorsFormData, Subcontractor}
import models.requests.DataRequest
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectSubcontractorsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SelectSubcontractorsView,
  formProvider: SelectSubcontractorsFormProvider,
  config: FrontendAppConfig,
  monthlyReturnService: MonthlyReturnService,
  subcontractorService: SubcontractorService
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

//  private val subcontractors = Seq(
//    SelectSubcontractorsViewModel(1, "Alice, A", "Yes", "Unknown", "Unknown"),
//    SelectSubcontractorsViewModel(2, "Bob, B", "Yes", "Unknown", "Unknown"),
//    SelectSubcontractorsViewModel(3, "Charles, C", "Yes", "Unknown", "Unknown"),
//    SelectSubcontractorsViewModel(4, "Dave, D", "Yes", "Unknown", "Unknown"),
//    SelectSubcontractorsViewModel(5, "Elise, E", "Yes", "Unknown", "Unknown"),
//    SelectSubcontractorsViewModel(6, "Frank, F", "Yes", "Unknown", "Unknown")
//  )

  private val form = formProvider()

  def onPageLoad(
    defaultSelection: Option[Boolean] = None
  ): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      requiredAnswers(request) match {
        case Some((cisId, taxMonth, taxYear)) =>
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(cisId, taxMonth, taxYear).map { data =>
            val verificationPeriodStart = subcontractorService.verificationPeriodStart(LocalDate.now())
            val subcontractorViewModels = data.subcontractors.map(s => toViewModel(s, verificationPeriodStart))

            val filledForm = defaultSelection match {
              case Some(true) =>
                form.fill(SelectSubcontractorsFormData(false, subcontractorViewModels.map(_.id)))
              case _          =>
                form
            }

            Ok(view(filledForm, subcontractorViewModels, config.selectSubcontractorsUpfrontDeclaration))
          }
        case None                             =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      requiredAnswers(request) match {
        case Some((cisId, taxMonth, taxYear)) =>
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(cisId, taxMonth, taxYear).flatMap { data =>

            val verificationPeriodStart = subcontractorService.verificationPeriodStart(LocalDate.now())
            val subcontractorViewModels = data.subcontractors.map(s => toViewModel(s, verificationPeriodStart))

            form
              .bindFromRequest()
              .fold(
                formWithErrors =>
                  Future.successful(
                    BadRequest(
                      view(formWithErrors, subcontractorViewModels, config.selectSubcontractorsUpfrontDeclaration)
                    )
                  ),
                formData =>
                  if (!formData.confirmation) {
                    val formWithError = form
                      .withError("confirmation", "monthlyreturns.selectSubcontractors.confirmation.required")
                      .fill(formData)

                    Future.successful(
                      BadRequest(
                        view(formWithError, subcontractorViewModels, config.selectSubcontractorsUpfrontDeclaration)
                      )
                    )
                  } else {
                    Future.successful(
                      Ok(
                        view(
                          form.fill(formData),
                          subcontractorViewModels,
                          config.selectSubcontractorsUpfrontDeclaration
                        )
                      )
                    )
                  }
              )
          }

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def requiredAnswers(request: DataRequest[AnyContent]): Option[(String, Int, Int)] =
    for {
      cisId   <- request.userAnswers.get(CisIdPage)
      taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
    } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

  private def toViewModel(
    subcontractor: Subcontractor,
    verificationPeriodStart: LocalDate
  ): SelectSubcontractorsViewModel = {
    val required = subcontractorService.verificationRequired(
      subcontractor.verified,
      subcontractor.verificationDate,
      subcontractor.lastMonthlyReturnDate,
      verificationPeriodStart
    )

    SelectSubcontractorsViewModel(
      id = subcontractor.subcontractorId.toInt,
      name = "Unknown",
      verificationRequired = if (required) "Yes" else "No",
      verificationNumber = "Unknown",
      taxTreatment = "Unknown"
    )
  }
}
