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
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.NormalMode
import models.monthlyreturns.SelectSubcontractorsFormData
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.SelectSubcontractorsView

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
  subcontractorService: SubcontractorService,
  monthlyReturnService: MonthlyReturnService
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(defaultSelection: Option[Boolean] = None): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val requiredAnswers = for {
        cisId   <- request.userAnswers.get(CisIdPage)
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

      requiredAnswers
        .map { (cisId, taxMonth, taxYear) =>
          subcontractorService
            .buildSelectSubcontractorPage(cisId, taxMonth, taxYear, defaultSelection)
            .map { model =>

              val filledForm =
                if (model.initiallySelectedIds.nonEmpty) {
                  form.fill(
                    SelectSubcontractorsFormData(
                      subcontractorsToInclude = model.initiallySelectedIds
                    )
                  )
                } else {
                  form
                }

              Ok(view(filledForm, model.subcontractors))
            }
        }
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>

      val requiredAnswers = for {
        cisId   <- request.userAnswers.get(CisIdPage)
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

      requiredAnswers
        .map { (cisId, taxMonth, taxYear) =>
          subcontractorService
            .buildSelectSubcontractorPage(cisId, taxMonth, taxYear, None)
            .flatMap { model =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors => Future.successful(BadRequest(view(formWithErrors, model.subcontractors))),
                  formData => {
                    val selectedSubcontractors =
                      model.subcontractors.filter(x => formData.subcontractorsToInclude.contains(x.id))

                    monthlyReturnService
                      .storeAndSyncSelectedSubcontractors(
                        ua = request.userAnswers,
                        cisId = cisId,
                        taxYear = taxYear,
                        taxMonth = taxMonth,
                        selected = selectedSubcontractors
                      )
                      .map { _ =>
                        if (selectedSubcontractors.exists(_.verificationRequired == "Yes")) {
                          Redirect(routes.VerifySubcontractorsController.onPageLoad(NormalMode))
                        } else {
                          Redirect(routes.PaymentDetailsController.onPageLoad(NormalMode, 1))
                        }
                      }
                      .recover { error =>
                        logger.error(
                          s"[SelectSubcontractorsController] Failed storing/syncing selected subcontractors: ${error.toString}",
                          error
                        )
                        Redirect(controllers.routes.SystemErrorController.onPageLoad())
                      }
                  }
                )
            }
        }
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
    }

}
