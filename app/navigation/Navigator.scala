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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import pages.*
import pages.monthlyreturns.*
import models.*
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.monthlyreturns.InactivityRequest
import utils.UserAnswerUtils.*

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: (Page, ReturnType) => UserAnswers => Call = {
    // nil return
    case (DateConfirmPaymentsPage, MonthlyNilReturn) =>
      _ => controllers.monthlyreturns.routes.InactivityRequestController.onPageLoad(NormalMode)
    case (InactivityRequestPage, _)                  =>
      _ => controllers.monthlyreturns.routes.ConfirmEmailAddressController.onPageLoad(NormalMode)
    case (ConfirmEmailAddressPage, _)                =>
      _ => controllers.monthlyreturns.routes.DeclarationController.onPageLoad(NormalMode)
    case (DeclarationPage, _)                        =>
      userAnswers =>
        userAnswers.get(InactivityRequestPage) match {
          case Some(InactivityRequest.Option2)        =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
          case Some(InactivityRequest.Option1) | None =>
            controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
        }
    case (InactivityWarningPage, _)                  =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()

    // monthly return
    case (VerifySubcontractorsPage, _)                      =>
      ua => controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(NormalMode, ua.firstIncompleteSubcontractorIndex, None)
    case (DateConfirmPaymentsPage, MonthlyStandardReturn)   =>
      _ => controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
    case (SelectedSubcontractorPaymentsMadePage(index), _)  =>
      _ => controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoad(NormalMode, index, None)
    case (SelectedSubcontractorMaterialCostsPage(index), _) =>
      _ => controllers.monthlyreturns.routes.TotalTaxDeductedController.onPageLoad(NormalMode, index, None)
    case (SelectedSubcontractorTaxDeductedPage(index), _)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (_, _)                                             => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  private val checkRouteMap: (Page, ReturnType) => UserAnswers => Call = {
    case (InactivityRequestPage, _)                         =>
      userAnswers =>
        userAnswers.get(InactivityRequestPage) match {
          case Some(InactivityRequest.Option2)        =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
          case Some(InactivityRequest.Option1) | None =>
            controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
        }
    case (SelectedSubcontractorPaymentsMadePage(index), _)  =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (SelectedSubcontractorMaterialCostsPage(index), _) =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (SelectedSubcontractorTaxDeductedPage(index), _)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (_, _)                                             => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = {
    val returnType = userAnswers.get(ReturnTypePage).getOrElse(ReturnType.MonthlyStandardReturn)
    mode match {
      case NormalMode =>
        normalRoutes(page, returnType)(userAnswers)
      case CheckMode  =>
        checkRouteMap(page, returnType)(userAnswers)
    }
  }
}
