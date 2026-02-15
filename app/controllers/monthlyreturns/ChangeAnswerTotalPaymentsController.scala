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
import models.monthlyreturns.SelectedSubcontractor
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.ChangeAnswersTotalPaymentsView

import javax.inject.Inject

class ChangeAnswersTotalPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeAnswersTotalPaymentsView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(index: Int): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    request.userAnswers.get(SelectedSubcontractorPage(index)) match {
      case None                => Redirect(controllers.routes.SystemErrorController.onPageLoad())
      case Some(subcontractor) =>
        Ok(view(ChangeAnswersTotalPaymentsViewModel.fromModel(subcontractor)))
    }
  }
}

case class ChangeAnswersTotalPaymentsViewModel(
  id: Long,
  name: String,
  totalPaymentsMade: String,
  costOfMaterials: String,
  totalTaxDeducted: String
)

object ChangeAnswersTotalPaymentsViewModel {
  def fromModel(subcontractor: SelectedSubcontractor): ChangeAnswersTotalPaymentsViewModel =
    ChangeAnswersTotalPaymentsViewModel(
      subcontractor.id,
      subcontractor.name,
      subcontractor.totalPaymentsMade.map(_.toString).getOrElse(""),
      subcontractor.costOfMaterials.map(_.toString).getOrElse(""),
      subcontractor.totalTaxDeducted.map(_.toString).getOrElse("")
    )
}
