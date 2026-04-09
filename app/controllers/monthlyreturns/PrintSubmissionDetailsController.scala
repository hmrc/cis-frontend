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
import models.monthlyreturns.SubcontractorPayment
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.PrintSubmissionDetailsView

import javax.inject.Inject

class PrintSubmissionDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: PrintSubmissionDetailsView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>
    val monthYear              = "April 2026"
    val submittedTime          = "8:46am"
    val submittedDate          = "16 March 2025"
    val receiptReferenceNumber = "6QEDAHDREBY455GDNCPMDCNDFBDBJSJSJDNDDHDJDZ5"
    val submissionType         = "Monthly return"
    val contractorName         = "PAL 355 Scheme"
    val payeReference          = "123/AB456"
    val totalPaymentsMade      = "£1900"
    val totalCostOfMaterials   = "£616"
    val totalTaxDeducted       = "£380"
    val subcontractors         = Seq(
      SubcontractorPayment("BuildRight Construction", "£165", "£95", "£95"),
      SubcontractorPayment("Northern Trades Ltd", "£75", "£55", "£55"),
      SubcontractorPayment("TyneWear Ltd", "£165", "£125", "£55")
    )

    Ok(
      view(
        monthYear,
        submittedTime,
        submittedDate,
        receiptReferenceNumber,
        submissionType,
        contractorName,
        payeReference,
        totalPaymentsMade,
        totalCostOfMaterials,
        totalTaxDeducted,
        subcontractors
      )
    )
  }
}
