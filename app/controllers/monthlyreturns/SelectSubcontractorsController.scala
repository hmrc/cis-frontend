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
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

import javax.inject.Inject

class SelectSubcontractorsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SelectSubcontractorsView,
  formProvider: SelectSubcontractorsFormProvider,
  config: FrontendAppConfig
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val subcontractors = Seq(
    SelectSubcontractorsViewModel(1, "Alice, A", "Yes", "Unknown", "Unknown"),
    SelectSubcontractorsViewModel(2, "Bob, B", "Yes", "Unknown", "Unknown"),
    SelectSubcontractorsViewModel(3, "Charles, C", "Yes", "Unknown", "Unknown"),
    SelectSubcontractorsViewModel(4, "Dave, D", "Yes", "Unknown", "Unknown"),
    SelectSubcontractorsViewModel(5, "Elise, E", "Yes", "Unknown", "Unknown"),
    SelectSubcontractorsViewModel(6, "Frank, F", "Yes", "Unknown", "Unknown")
  )

  private val form = formProvider()

  private def onPageLoad(
    includeByDefault: Option[Boolean] = None,
    subcontractorViewModels: Seq[SelectSubcontractorsViewModel] = subcontractors
  ): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      val filledForm = includeByDefault match {
        case Some(true) =>
          form.fill(SelectSubcontractorsFormData(false, subcontractorViewModels.map(_.id)))
        case _          => form
      }

      Ok(view(filledForm, subcontractorViewModels, config.selectSubcontractorsUpfrontDeclaration))
    }

  def onPageLoadNonEmpty(monthsToIncludeDefault: Option[Boolean] = None): Action[AnyContent] =
    onPageLoad(monthsToIncludeDefault, subcontractors)

  def onPageLoadEmpty(monthsToIncludeDefault: Option[Boolean] = None): Action[AnyContent] =
    onPageLoad(monthsToIncludeDefault, Nil)

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => {
          logger.warn(s"formWithErrors: $formWithErrors")
          logger.warn(s"formWithErrors.value: ${formWithErrors.value}")
          BadRequest(view(formWithErrors, subcontractors, config.selectSubcontractorsUpfrontDeclaration))
        },
        formData =>
          if (!formData.confirmation) {
            logger.warn(s"formUnconfirmed: ${form.bindFromRequest()}")
            logger.warn(s"formUnconfirmed.value: ${form.fill(formData).value}")
            BadRequest(
              view(
                form
                  .withError("confirmation", "monthlyreturns.selectSubcontractors.confirmation.required")
                  .fill(formData),
                subcontractors,
                config.selectSubcontractorsUpfrontDeclaration
              )
            )
          } else {
            logger.warn(s"formConfirmed: ${form.bindFromRequest()}")
            logger.warn(s"formConfirmed.value: $formData")
            Ok(
              view(form.fill(formData), subcontractors, config.selectSubcontractorsUpfrontDeclaration)
            )
          }
      )

  }
}
