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
import play.api.Logging
import play.api.mvc.Call

import java.util.UUID
import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () extends Logging {

  private val normalRoutes: (Page, ReturnType) => UserAnswers => Call = {
    // common
    case (SubmitInactivityRequestPage, _) =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(NormalMode)(userAnswers)

    // nil return
    case (DateConfirmPaymentsPage, MonthlyNilReturn) =>
      _ => controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
    case (ConfirmEmailAddressPage, _)                =>
      _ => controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
    case (DeclarationPage, _)                        =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
    case (InactivityWarningPage, _)                  =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()

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
          controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
        } else {
          controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
        }
    // amend monthly return
    case (AreYouSureYouWantToAmendYesNoPage, _)             =>
      userAnswers =>
        userAnswers.get(AreYouSureYouWantToAmendYesNoPage) match {
          case Some(value) if value == AreYouSureYouWantToAmendYesNo.Yes =>
            controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
          case _                                                         =>
            controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad()
        }
    case (WhichSubcontractorsToAddPage, _)                  =>
      _ => controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
    case (_, _)                                             => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  private val checkRouteMap: (Page, ReturnType) => UserAnswers => Call = {
    case (SelectedSubcontractorPaymentsMadePage(index, _), _)  =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (SelectedSubcontractorMaterialCostsPage(index, _), _) =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (SelectedSubcontractorTaxDeductedPage(index, _), _)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(index)
    case (EmploymentStatusDeclarationPage, _)                  =>
      userAnswers => navigatorFromEmploymentStatusDeclarationPage(CheckMode)(userAnswers)
    case (VerifiedStatusDeclarationPage, _)                    =>
      userAnswers => navigatorFromVerifiedStatusDeclarationPage(CheckMode)(userAnswers)
    case (SubmitInactivityRequestPage, _)                      =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(CheckMode)(userAnswers)
    case (ConfirmationByEmailPage, _)                          =>
      userAnswers => navigatorFromConfirmationByEmailPage(CheckMode)(userAnswers)
    case (EnterYourEmailAddressPage, _)                        =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
    // amend monthly return
    case (WhichSubcontractorsToAddPage, _)                     =>
      _ => controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode)
    case (_, _)                                                => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
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
      case _           =>
        logger.error(
          "[Navigator][navigatorFromPaymentDetailsConfirmationPage] - PaymentDetailsConfirmationPage not set"
        )
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromEmploymentStatusDeclarationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(EmploymentStatusDeclarationPage), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.VerifiedStatusDeclarationController.onPageLoad(NormalMode)
      case (Some(_), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)             =>
        logger.error(
          "[Navigator][navigatorFromEmploymentStatusDeclarationPage] - EmploymentStatusDeclarationPage not set"
        )
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromVerifiedStatusDeclarationPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(VerifiedStatusDeclarationPage), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
      case (Some(_), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)             =>
        logger.error("[Navigator][navigatorFromVerifiedStatusDeclarationPage] - VerifiedStatusDeclarationPage not set")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromSubmitInactivityRequestPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(SubmitInactivityRequestPage), mode) match {
      case (Some(true), NormalMode)  =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(NormalMode)
      case (Some(true), CheckMode)   =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(CheckMode)
      case (Some(false), NormalMode) =>
        controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode)
      case (Some(false), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)                 =>
        logger.error("[Navigator][navigatorFromSubmitInactivityRequestPage] - SubmitInactivityRequestPage not set")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  private def navigatorFromConfirmationByEmailPage(
    mode: Mode
  )(userAnswers: UserAnswers): Call =
    (userAnswers.get(ConfirmationByEmailPage), mode) match {
      case (Some(true), mode)        =>
        controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(mode)
      case (Some(false), NormalMode) =>
        if (userAnswers.get(EmploymentStatusDeclarationPage).isDefined) {
          controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
        } else {
          controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
        }
      case (Some(false), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      case (None, _)                 =>
        logger.error("[Navigator][navigatorFromConfirmationByEmailPage] - ConfirmationByEmailPage not set")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }

  // Multi-tab: Normal mode navigation
  private def normalRoutesWithId(flowId: UUID): (Page, ReturnType) => UserAnswers => Call = {
    // common
    case (SubmitInactivityRequestPage, _) =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(flowId, NormalMode, userAnswers)

    // nil return
    case (DateConfirmPaymentsPage, MonthlyNilReturn) =>
      _ => controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode)

    case (DeclarationPage, _) =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)

    case (ConfirmationByEmailPage, _) =>
      userAnswers => navigatorFromConfirmationByEmailPage(flowId, NormalMode, userAnswers)

    case (EnterYourEmailAddressPage, _) =>
      _ => navigatorFromEnterYourEmailAddressPage(flowId, NormalMode)

    // monthly return
    case (VerifySubcontractorsPage, _)                         =>
      _ => controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoadWithId(flowId, NormalMode)
    case (DateConfirmPaymentsPage, MonthlyStandardReturn)      =>
      userAnswers =>
        if (userAnswers.get(SelectedSubcontractorPage.all).exists(_.nonEmpty)) {
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoadWithId(flowId, NormalMode)
        } else {
          controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadWithId(flowId, None)
        }
    case (SelectedSubcontractorPaymentsMadePage(index, _), _)  =>
      _ => controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoadWithId(flowId, NormalMode, index, None)
    case (SelectedSubcontractorMaterialCostsPage(index, _), _) =>
      _ =>
        controllers.monthlyreturns.routes.TotalTaxDeductedController.onPageLoadWithId(flowId, NormalMode, index, None)
    case (SelectedSubcontractorTaxDeductedPage(index, _), _)   =>
      _ => controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoadWithId(flowId, index)
    case (PaymentDetailsConfirmationPage, _)                   =>
      userAnswers => navigatorFromPaymentDetailsConfirmationPage(flowId, userAnswers)
    case (EmploymentStatusDeclarationPage, _)                  =>
      userAnswers => navigatorFromEmploymentStatusDeclarationPage(flowId, NormalMode, userAnswers)
    case (VerifiedStatusDeclarationPage, _)                    =>
      userAnswers => navigatorFromVerifiedStatusDeclarationPage(flowId, NormalMode, userAnswers)

    // TODO: Add UUID-scoped amended monthly return navigation.

    case _ =>
      _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  // Multi-tab: Check mode navigation
  private def checkRouteMapWithId(flowId: UUID): (Page, ReturnType) => UserAnswers => Call = {
    case (SubmitInactivityRequestPage, _) =>
      userAnswers => navigatorFromSubmitInactivityRequestPage(flowId, CheckMode, userAnswers)

    case (ConfirmationByEmailPage, _) =>
      userAnswers => navigatorFromConfirmationByEmailPage(flowId, CheckMode, userAnswers)

    case (EnterYourEmailAddressPage, _) =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)

    // nil return
    case (_, MonthlyNilReturn) =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)

    // TODO: Add UUID-scoped standard monthly return check navigation.

    // TODO: Add UUID-scoped amended monthly return check navigation.

    case _ =>
      _ => controllers.routes.JourneyRecoveryController.onPageLoad()
  }

  // Multi-tab: Navigate within a UUID-scoped monthly return journey
  def nextPage(
    flowId: UUID,
    page: Page,
    mode: Mode,
    userAnswers: UserAnswers
  ): Call = {
    val id = flowId.toString

    userAnswers.get(ReturnTypePage(id)) match {
      case Some(returnType) =>
        mode match {
          case NormalMode => normalRoutesWithId(flowId)(page, returnType)(userAnswers)
          case CheckMode  => checkRouteMapWithId(flowId)(page, returnType)(userAnswers)
        }
      case None             =>
        logger.error("[Navigator][nextPage] - UUID-scoped ReturnTypePage missing")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigatorFromSubmitInactivityRequestPage(
    flowId: UUID,
    mode: Mode,
    userAnswers: UserAnswers
  ): Call = {
    val id = flowId.toString

    (userAnswers.get(SubmitInactivityRequestPage(id)), mode) match {
      case (Some(true), NormalMode)  =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoadWithId(flowId, NormalMode)
      case (Some(true), CheckMode)   =>
        controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoadWithId(flowId, CheckMode)
      case (Some(false), NormalMode) =>
        controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(flowId, NormalMode)
      case (Some(false), CheckMode)  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
      case (None, _)                 =>
        logger.error("[Navigator][navigatorFromSubmitInactivityRequestPage] - Scoped answer not set")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigatorFromConfirmationByEmailPage(
    flowId: UUID,
    mode: Mode,
    userAnswers: UserAnswers
  ): Call = {
    val id = flowId.toString

    (userAnswers.get(ConfirmationByEmailPage(id)), mode) match {
      case (Some(true), mode)        =>
        controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, mode)
      case (Some(false), NormalMode) =>
//        controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(flowId) // TODO DTR-9474: Removed due to not existing on this branch. Reinstate during merge.
        controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
      case (Some(false), CheckMode)  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
      case (None, _)                 =>
        logger.error("[Navigator][navigatorFromConfirmationByEmailPage] - ID-scoped ConfirmationByEmailPage not set")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigatorFromEnterYourEmailAddressPage(
    flowId: UUID,
    mode: Mode
  ): Call =
    mode match {
      case NormalMode =>
//        controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(flowId) // TODO DTR-9474: Removed due to not existing on this branch. Reinstate during merge.
        controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
      case CheckMode  =>
        controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
    }

  private def navigatorFromPaymentDetailsConfirmationPage(flowId: UUID, userAnswers: UserAnswers): Call = {
    val id = flowId.toString
    userAnswers.get(PaymentDetailsConfirmationPage(id)) match {
      case Some(true)  =>
        controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onPageLoadWithId(flowId, NormalMode)
      case Some(false) =>
        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoadWithId(flowId, NormalMode)
      case _           =>
        logger.error(
          "[Navigator][navigatorFromPaymentDetailsConfirmationPage] - PaymentDetailsConfirmationPage not set"
        )
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigatorFromEmploymentStatusDeclarationPage(flowId: UUID, mode: Mode, userAnswers: UserAnswers): Call = {
    val id = flowId.toString
    (userAnswers.get(EmploymentStatusDeclarationPage(id)), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.VerifiedStatusDeclarationController.onPageLoadWithId(flowId, NormalMode)
      case (Some(_), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
      case (None, _)             =>
        logger.error(
          "[Navigator][navigatorFromEmploymentStatusDeclarationPage] - EmploymentStatusDeclarationPage not set"
        )
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
  }

  private def navigatorFromVerifiedStatusDeclarationPage(
    flowId: UUID,
    mode: Mode,
    userAnswers: UserAnswers
  ): Call =
    val id = flowId.toString
    (userAnswers.get(VerifiedStatusDeclarationPage(id)), mode) match {
      case (Some(_), NormalMode) =>
        controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(flowId, NormalMode)
      case (Some(_), CheckMode)  => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
      case (None, _)             =>
        logger.error("[Navigator][navigatorFromVerifiedStatusDeclarationPage] - VerifiedStatusDeclarationPage not set")
        controllers.routes.JourneyRecoveryController.onPageLoad()
    }
}
