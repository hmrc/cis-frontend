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
import models.monthlyreturns.InactivityRequest

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    // nil return
    case DateConfirmNilPaymentsPage =>
      _ => controllers.monthlyreturns.routes.InactivityRequestController.onPageLoad(NormalMode)
    case InactivityRequestPage      =>
      _ => controllers.monthlyreturns.routes.ConfirmEmailAddressController.onPageLoad(NormalMode)
    case ConfirmEmailAddressPage    =>
      _ => controllers.monthlyreturns.routes.DeclarationController.onPageLoad(NormalMode)
    case DeclarationPage            =>
      userAnswers =>
        userAnswers.get(InactivityRequestPage) match {
          case Some(InactivityRequest.Option2)        =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
          case Some(InactivityRequest.Option1) | None =>
            controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
        }
    case InactivityWarningPage      =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()

    // monthly return
    case VerifySubcontractorsPage                      =>
      _ => controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(NormalMode, 1, None)
    case DateConfirmPaymentsPage                       =>
      _ => controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
    case SelectedSubcontractorPaymentsMadePage(index)  =>
      _ => controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoad(NormalMode, index, None)
    case SelectedSubcontractorMaterialCostsPage(index) =>
      _ => controllers.monthlyreturns.routes.TotalTaxDeductedController.onPageLoad(NormalMode, index, None)
    case SelectedSubcontractorTaxDeductedPage(index)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case PaymentDetailsConfirmationPage                =>
      userAnswers => navigatorFromPaymentDetailsConfirmationPage(NormalMode)(userAnswers)
    case EmploymentStatusDeclarationPage               =>
      userAnswers => navigatorFromEmploymentStatusDeclarationPage(NormalMode)(userAnswers)
    case VerifiedStatusDeclarationPage                 =>
      userAnswers => navigatorFromVerifiedStatusDeclarationPage(NormalMode)(userAnswers)
    case SubmitInactivityRequestPage                   =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(NormalMode)(userAnswers)
    case ConfirmationByEmailPage                       =>
      userAnswers => navigatorFromConfirmationByEmailPage(NormalMode)(userAnswers)
    case EnterYourEmailAddressPage                     =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
    case _                                             => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case InactivityRequestPage                         =>
      userAnswers =>
        userAnswers.get(InactivityRequestPage) match {
          case Some(InactivityRequest.Option2)        =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
          case Some(InactivityRequest.Option1) | None =>
            controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
        }
    case SelectedSubcontractorPaymentsMadePage(index)  =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case SelectedSubcontractorMaterialCostsPage(index) =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case SelectedSubcontractorTaxDeductedPage(index)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case PaymentDetailsConfirmationPage                =>
      userAnswers => navigatorFromPaymentDetailsConfirmationPage(CheckMode)(userAnswers)
    case EmploymentStatusDeclarationPage               =>
      userAnswers => navigatorFromEmploymentStatusDeclarationPage(CheckMode)(userAnswers)
    case VerifiedStatusDeclarationPage                 =>
      userAnswers => navigatorFromVerifiedStatusDeclarationPage(CheckMode)(userAnswers)
    case SubmitInactivityRequestPage                   =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(CheckMode)(userAnswers)
    case ConfirmationByEmailPage                       =>
      userAnswers => navigatorFromConfirmationByEmailPage(CheckMode)(userAnswers)
    case EnterYourEmailAddressPage                     =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
    case _                                             => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)
  }

  private def navigatorFromPaymentDetailsConfirmationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(PaymentDetailsConfirmationPage), mode) match {
      case (Some(true), _)           =>
        controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onPageLoad(mode)
      case (Some(false), NormalMode) =>
        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      case (Some(false), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)                 => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromEmploymentStatusDeclarationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(EmploymentStatusDeclarationPage), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.VerifiedStatusDeclarationController.onPageLoad(NormalMode)
      case (Some(_), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)             => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromVerifiedStatusDeclarationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(VerifiedStatusDeclarationPage), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
      case (Some(_), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)             => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromSubmitInactivityRequestPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(SubmitInactivityRequestPage), mode) match {
      case (Some(true), _)           =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad()
      case (Some(false), NormalMode) =>
        controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode)
      case (Some(false), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)                 => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromConfirmationByEmailPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(ConfirmationByEmailPage), mode) match {
      case (Some(true), _)  =>
        controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(NormalMode)
      case (Some(false), _) => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)        => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
