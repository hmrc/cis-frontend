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
import models.monthlyreturns.SelectSubcontractorsFormData
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SubcontractorService
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
  config: FrontendAppConfig,
  subcontractorService: SubcontractorService
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
                      confirmation = false,
                      subcontractorsToInclude = model.initiallySelectedIds
                    )
                  )
                } else {
                  form
                }

              Ok(view(filledForm, model.subcontractors, config.selectSubcontractorsUpfrontDeclaration))
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
            .map { model =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    BadRequest(
                      view(formWithErrors, model.subcontractors, config.selectSubcontractorsUpfrontDeclaration)
                    ),
                  formData =>
                    if (!formData.confirmation) {
                      BadRequest(
                        view(
                          form
                            .withError("confirmation", "monthlyreturns.selectSubcontractors.confirmation.required")
                            .fill(formData),
                          model.subcontractors,
                          config.selectSubcontractorsUpfrontDeclaration
                        )
                      )
                    } else {
                      Ok(view(form.fill(formData), model.subcontractors, config.selectSubcontractorsUpfrontDeclaration))
                    }
                )
            }
        }
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
    }

}
