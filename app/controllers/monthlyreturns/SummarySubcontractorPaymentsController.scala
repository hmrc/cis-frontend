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

import controllers.actions._
import javax.inject.Inject
import pages.monthlyreturns.SelectedSubcontractorPage
import pages.monthlyreturns.SelectedSubcontractorPage.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.SummarySubcontractorPaymentsViewModel
import views.html.monthlyreturns.SummarySubcontractorPaymentsView

import scala.concurrent.{ExecutionContext, Future}
import scala.math.BigDecimal.RoundingMode

class SummarySubcontractorPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SummarySubcontractorPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>
      val subcontractors = request.userAnswers
        .get(SelectedSubcontractorPage.all)
        .getOrElse(Map.empty)
        .values
        .filter(_.isComplete)
        .toSeq

      val totalPayments      = request.userAnswers.summaryTotalPayments.getOrElse(BigDecimal(0))
      val totalMaterialsCost = request.userAnswers.summaryTotalMaterialsCost.getOrElse(BigDecimal(0))
      val totalCisDeductions = request.userAnswers.summaryTotalCisDeductions.getOrElse(BigDecimal(0))

      val viewModel = SummarySubcontractorPaymentsViewModel(
        subcontractorCount = subcontractors.size,
        totalPayments = totalPayments,
        totalMaterialsCost = totalMaterialsCost,
        totalCisDeductions = totalCisDeductions
      )

      Ok(view(viewModel))
  }
}
