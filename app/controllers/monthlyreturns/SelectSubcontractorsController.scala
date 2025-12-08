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
  formProvider: SelectSubcontractorsFormProvider
) extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val subcontractors = Seq(
    SelectSubcontractorsViewModel("Alice, A", "Yes", "Unknown", "Unknown", false),
    SelectSubcontractorsViewModel("Bob, B", "Yes", "Unknown", "Unknown", false),
    SelectSubcontractorsViewModel("Charles, C", "Yes", "Unknown", "Unknown", false),
    SelectSubcontractorsViewModel("Dave, D", "Yes", "Unknown", "Unknown", false),
    SelectSubcontractorsViewModel("Elise, E", "Yes", "Unknown", "Unknown", false),
    SelectSubcontractorsViewModel("Frank, F", "Yes", "Unknown", "Unknown", false)
  )

  private val form = formProvider()

  private def onPageLoad(
    monthsToIncludeDefault: Option[Boolean] = None,
    subcontractorViewModels: Seq[SelectSubcontractorsViewModel]
  ): Action[AnyContent] =
    (identify andThen getData andThen requireData) { implicit request =>
      logger.warn(s"DEFAULT: $monthsToIncludeDefault")
      val filledForm = monthsToIncludeDefault match {
        case Some(default) =>
          form.fill(SelectSubcontractorsFormData(false, subcontractorViewModels.map(_ => default).toList))
        case None          => form
      }
      logger.warn(s"FILLED FORM: $filledForm")

      val monthsToInclude = filledForm.value.toList.flatMap(_.monthsToInclude)
      logger.warn(s"FILLED FORM MONTHS: $monthsToInclude")
      logger.warn(s"FILLED VALUES: ${filledForm.value}")

      val subcontractorsWithFormValues = subcontractorViewModels.zipWithIndex.map { (subcontractor, index) =>
        val monthToInclude = monthsToInclude.lift(index).getOrElse(false)
        subcontractor.copy(includeThisMonth = monthToInclude)
      }.toList
      logger.warn(s"SUBCONTRACTORS WITH FORM VALUES: $subcontractorsWithFormValues")

      Ok(view(filledForm, subcontractorsWithFormValues))
    }

  def onPageLoadNonEmpty(monthsToIncludeDefault: Option[Boolean] = None): Action[AnyContent] =
    onPageLoad(monthsToIncludeDefault, subcontractors)

  def onPageLoadEmpty(monthsToIncludeDefault: Option[Boolean] = None): Action[AnyContent] =
    onPageLoad(monthsToIncludeDefault, Seq())

  def onSubmit: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    logger.warn(s"FILLED VALUES: ${form.bindFromRequest().value}")

    Ok(view(form.bindFromRequest(), subcontractors))
  }
}
