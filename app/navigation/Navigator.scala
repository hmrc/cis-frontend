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

import models.*
import models.ReturnType.*
import models.amend.*
import pages.*
import pages.amend.*
import pages.monthlyreturns.*
import play.api.mvc.Call

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: (Page, ReturnType) => UserAnswers => Call = {
    // common
    case (SubmitInactivityRequestPage, _) =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(NormalMode)(userAnswers)

    // nil return
    case (DateConfirmPaymentsPage, MonthlyNilReturn) =>
      ua => controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(ua.urlPath, NormalMode)
    case (ConfirmEmailAddressPage, _)                =>
      ua => controllers.monthlyreturns.routes.DeclarationController.onPageLoad(ua.urlPath)
    case (DeclarationPage, _)                        =>
      ua => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(ua.urlPath)
    case (InactivityWarningPage, _)                  =>
      ua => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(ua.urlPath)

    // monthly return
    case (VerifySubcontractorsPage, _)                      =>
      _ => controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
    case (DateConfirmPaymentsPage, MonthlyStandardReturn)   =>
      userAnswers =>
        if (userAnswers.get(SelectedSubcontractorPage.all).exists(_.nonEmpty)) {
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
        } else {
          controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
        }
    case (SelectedSubcontractorPaymentsMadePage(index), _)  =>
      _ => controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoad(NormalMode, index, None)
    case (SelectedSubcontractorMaterialCostsPage(index), _) =>
      _ => controllers.monthlyreturns.routes.TotalTaxDeductedController.onPageLoad(NormalMode, index, None)
    case (SelectedSubcontractorTaxDeductedPage(index), _)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (PaymentDetailsConfirmationPage, _)                =>
      userAnswers => navigatorFromPaymentDetailsConfirmationPage()(userAnswers)
    case (EmploymentStatusDeclarationPage, _)               =>
      userAnswers => navigatorFromEmploymentStatusDeclarationPage(NormalMode)(userAnswers)
    case (VerifiedStatusDeclarationPage, _)                 =>
      userAnswers => navigatorFromVerifiedStatusDeclarationPage(NormalMode)(userAnswers)
    case (ConfirmationByEmailPage, _)                       =>
      userAnswers => navigatorFromConfirmationByEmailPage(NormalMode)(userAnswers)
    case (EnterYourEmailAddressPage, _)                     =>
      userAnswers =>
        if (userAnswers.get(EmploymentStatusDeclarationPage).isDefined) {
          controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(userAnswers.urlPath)
        } else {
          controllers.monthlyreturns.routes.DeclarationController.onPageLoad(userAnswers.urlPath)
        }
    // amend monthly return
    case (AreYouSureYouWantToAmendYesNoPage, _)             =>
      userAnswers =>
        userAnswers.get(AreYouSureYouWantToAmendYesNoPage) match {
          case Some(value) if value == AreYouSureYouWantToAmendYesNo.Yes =>
            controllers.monthlyreturns.routes.SubmitInactivityRequestController
              .onPageLoad(userAnswers.urlPath, NormalMode)
          case _                                                         =>
            controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad()
        }
    case (WhichSubcontractorsToAddPage, _)                  =>
      _ => controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
    case (_, _)                                             => ua => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(ua.urlPath)
  }

  private val checkRouteMap: (Page, ReturnType) => UserAnswers => Call = {
    case (SelectedSubcontractorPaymentsMadePage(index), _)  =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (SelectedSubcontractorMaterialCostsPage(index), _) =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (SelectedSubcontractorTaxDeductedPage(index), _)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (EmploymentStatusDeclarationPage, _)               =>
      userAnswers => navigatorFromEmploymentStatusDeclarationPage(CheckMode)(userAnswers)
    case (VerifiedStatusDeclarationPage, _)                 =>
      userAnswers => navigatorFromVerifiedStatusDeclarationPage(CheckMode)(userAnswers)
    case (SubmitInactivityRequestPage, _)                   =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(CheckMode)(userAnswers)
    case (ConfirmationByEmailPage, _)                       =>
      userAnswers => navigatorFromConfirmationByEmailPage(CheckMode)(userAnswers)
    case (EnterYourEmailAddressPage, _)                     =>
      ua => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(ua.urlPath)
    // amend monthly return
    case (WhichSubcontractorsToAddPage, _)                  =>
      _ => controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode)
    case (_, _)                                             => ua => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(ua.urlPath)
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

  private def navigatorFromPaymentDetailsConfirmationPage()(userAnswers: UserAnswers): Call =
    userAnswers.get(PaymentDetailsConfirmationPage) match {
      case Some(true)  =>
        controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onPageLoad(NormalMode)
      case Some(false) =>
        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      case _           => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromEmploymentStatusDeclarationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(EmploymentStatusDeclarationPage), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.VerifiedStatusDeclarationController.onPageLoad(NormalMode)
      case (Some(_), CheckMode)  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(userAnswers.urlPath)
      case (None, _)             => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromVerifiedStatusDeclarationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(VerifiedStatusDeclarationPage), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.SubmitInactivityRequestController
          .onPageLoad(userAnswers.urlPath, NormalMode)
      case (Some(_), CheckMode)  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(userAnswers.urlPath)
      case (None, _)             => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromSubmitInactivityRequestPage(mode: Mode)(userAnswers: UserAnswers): Call =
    (userAnswers.get(SubmitInactivityRequestPage), mode) match {
      case (Some(true), NormalMode)  =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController
          .onPageLoad(userAnswers.urlPath, NormalMode)
      case (Some(true), CheckMode)   =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController
          .onPageLoad(userAnswers.urlPath, CheckMode)
      case (Some(false), NormalMode) =>
        controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(userAnswers.urlPath, NormalMode)
      case (Some(false), CheckMode)  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(userAnswers.urlPath)
      case (None, _)                 => controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromConfirmationByEmailPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(ConfirmationByEmailPage), mode) match {
      case (Some(true), mode)        =>
        controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(userAnswers.urlPath, mode)
      case (Some(false), NormalMode) =>
        if (userAnswers.get(EmploymentStatusDeclarationPage).isDefined) {
          controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(userAnswers.urlPath)
        } else {
          controllers.monthlyreturns.routes.DeclarationController.onPageLoad(userAnswers.urlPath)
        }
      case (Some(false), CheckMode)  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad(userAnswers.urlPath)
      case (None, _)                 => controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
