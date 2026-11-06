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

import base.SpecBase
import controllers.monthlyreturns
import pages.*
import pages.monthlyreturns.*
import models.*
import models.amend.*
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyNilReturn}
import models.monthlyreturns.{InactivityRequest, SelectedSubcontractor}
import pages.amend.{AreYouSureYouWantToAmendYesNoPage, WhichSubcontractorsToAddPage}

import java.util.UUID

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from DateConfirmPaymentsPage (MonthlyNilReturn) to SubmitInactivityRequestController" in {
        navigator.nextPage(
          DateConfirmPaymentsPage,
          NormalMode,
          UserAnswers("id").setOrException(ReturnTypePage, MonthlyNilReturn)
        ) mustBe controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
      }

      "must go from SubmitInactivityRequestPage to ConfirmationByEmailController when inactivity request is NO" in {
        val ua = UserAnswers("id").setOrException(SubmitInactivityRequestPage, false)
        navigator.nextPage(
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode)
      }

      "must go from ConfirmEmailAddressPage to DeclarationController" in {
        navigator.nextPage(
          ConfirmEmailAddressPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
      }

      "must go from DeclarationPage to CheckYourAnswers" in {
        navigator.nextPage(
          DeclarationPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from InactivityWarningPage to CheckYourAnswers" in {
        navigator.nextPage(
          InactivityWarningPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from VerifySubcontractorsPage to SubcontractorDetailsAddedController" in {
        navigator.nextPage(
          VerifySubcontractorsPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      }

      "must go from VerifySubcontractorsPage to SubcontractorDetailsAddedController when subcontractors are present" in {
        val ua = UserAnswers("id")
          .setOrException(SelectedSubcontractorPage(0), completeSub)
          .setOrException(SelectedSubcontractorPage(2), incompleteSub)
          .setOrException(SelectedSubcontractorPage(5), incompleteSub)

        navigator.nextPage(
          VerifySubcontractorsPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      }

      "must go from DateConfirmPaymentsPage to SelectSubcontractorsController when no subcontractors are present" in {
        navigator.nextPage(
          DateConfirmPaymentsPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
      }

      "must go from DateConfirmPaymentsPage to SubcontractorDetailsAddedController when subcontractors are present" in {
        val ua = UserAnswers("id").setOrException(SelectedSubcontractorPage(0), completeSub)
        navigator.nextPage(
          DateConfirmPaymentsPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      }

      "must go from SelectedSubcontractorPaymentsMadePage to CostOfMaterialsController" in {
        navigator.nextPage(
          SelectedSubcontractorPaymentsMadePage(1),
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.CostOfMaterialsController.onPageLoad(NormalMode, 1, None)
      }

      "must go from SelectedSubcontractorMaterialCostsPage to TotalTaxDeductedController" in {
        navigator.nextPage(
          SelectedSubcontractorMaterialCostsPage(2),
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.TotalTaxDeductedController.onPageLoad(NormalMode, 2, None)
      }

      "must go from SelectedSubcontractorTaxDeductedPage to JourneyRecoveryController" in {
        navigator.nextPage(
          SelectedSubcontractorTaxDeductedPage(1),
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(1)
      }

      "must go from PaymentDetailsConfirmationPage to EmploymentStatusDeclarationController when answer is true" in {
        val ua = UserAnswers("id").setOrException(PaymentDetailsConfirmationPage, true)
        navigator.nextPage(
          PaymentDetailsConfirmationPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.EmploymentStatusDeclarationController.onPageLoad(NormalMode)
      }

      "must go from PaymentDetailsConfirmationPage to SubcontractorDetailsAddedController when answer is false" in {
        val ua = UserAnswers("id").setOrException(PaymentDetailsConfirmationPage, false)
        navigator.nextPage(
          PaymentDetailsConfirmationPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      }

      "must go from PaymentDetailsConfirmationPage to JourneyRecovery when answer is missing" in {
        navigator.nextPage(
          PaymentDetailsConfirmationPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EmploymentStatusDeclarationPage to VerifiedStatusDeclarationController when answer is present" in {
        val ua = UserAnswers("id").setOrException(EmploymentStatusDeclarationPage, true)
        navigator.nextPage(
          EmploymentStatusDeclarationPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.VerifiedStatusDeclarationController.onPageLoad(NormalMode)
      }

      "must go from EmploymentStatusDeclarationPage to JourneyRecovery when answer is missing" in {
        navigator.nextPage(
          EmploymentStatusDeclarationPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from VerifiedStatusDeclarationPage to SubmitInactivityRequestController when answer is present" in {
        val ua = UserAnswers("id").setOrException(VerifiedStatusDeclarationPage, true)
        navigator.nextPage(
          VerifiedStatusDeclarationPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
      }

      "must go from VerifiedStatusDeclarationPage to JourneyRecovery when answer is missing" in {
        navigator.nextPage(
          VerifiedStatusDeclarationPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SubmitInactivityRequestPage to InactivityRequestWarningController when answer is true" in {
        val ua = UserAnswers("id").setOrException(SubmitInactivityRequestPage, true)
        navigator.nextPage(
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(NormalMode)
      }

      "must go from SubmitInactivityRequestPage to ConfirmationByEmailController when answer is false" in {
        val ua = UserAnswers("id").setOrException(SubmitInactivityRequestPage, false)
        navigator.nextPage(
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(NormalMode)
      }

      "must go from SubmitInactivityRequestPage to JourneyRecovery when answer is missing" in {
        navigator.nextPage(
          SubmitInactivityRequestPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ConfirmationByEmailPage to EnterYourEmailAddressController when answer is true" in {
        val ua = UserAnswers("id").setOrException(ConfirmationByEmailPage, true)
        navigator.nextPage(
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(NormalMode)
      }

      "must go from ConfirmationByEmailPage to DeclarationController when answer is false and no employment status declaration exists" in {
        val ua = UserAnswers("id").setOrException(ConfirmationByEmailPage, false)
        navigator.nextPage(
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
      }

      "must go from ConfirmationByEmailPage to CheckYourAnswers when answer is false and employment status declaration exists" in {
        val ua = UserAnswers("id")
          .setOrException(ConfirmationByEmailPage, false)
          .setOrException(EmploymentStatusDeclarationPage, true)
        navigator.nextPage(
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from ConfirmationByEmailPage to JourneyRecovery when answer is missing" in {
        navigator.nextPage(
          ConfirmationByEmailPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterYourEmailAddressPage to DeclarationController when no employment status declaration exists" in {
        navigator.nextPage(
          EnterYourEmailAddressPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoad()
      }

      "must go from EnterYourEmailAddressPage to CheckYourAnswers when employment status declaration exists" in {
        val ua = UserAnswers("id").setOrException(EmploymentStatusDeclarationPage, true)
        navigator.nextPage(
          EnterYourEmailAddressPage,
          NormalMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from a page that doesn't exist in the route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController
          .onPageLoad()
      }

      "must go from AreYouSureYouWantToAmendYesNoPage to SubmitInactivityRequestController when user answered Yes for Amended StandardReturn" in {
        val ua = UserAnswers("id")
          .setOrException(AreYouSureYouWantToAmendYesNoPage, AreYouSureYouWantToAmendYesNo.Yes)
          .setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        navigator.nextPage(
          AreYouSureYouWantToAmendYesNoPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
      }

      "must go from AreYouSureYouWantToAmendYesNoPage to WhatDoYouWantToAmendNilController when user answered No for Amended StandardReturn" in {
        val ua = UserAnswers("id")
          .setOrException(AreYouSureYouWantToAmendYesNoPage, AreYouSureYouWantToAmendYesNo.No)
          .setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        navigator.nextPage(
          AreYouSureYouWantToAmendYesNoPage,
          NormalMode,
          ua
        ) mustBe controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad()
      }

      "must go from WhichSubcontractorsToAddPage to SubcontractorDetailsAddedController when Amended StandardReturn" in {
        val ua = UserAnswers("id")
          .setOrException(WhichSubcontractorsToAddPage, Set.empty)
          .setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        navigator.nextPage(
          WhichSubcontractorsToAddPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      }

      "must go from WhichSubcontractorsToAddPage to SubcontractorDetailsAddedController when Amended NilReturn" in {
        val ua = UserAnswers("id")
          .setOrException(WhichSubcontractorsToAddPage, Set.empty)
          .setOrException(ReturnTypePage, MonthlyAmendedNilReturn)
        navigator.nextPage(
          WhichSubcontractorsToAddPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
      }
    }

    "in Check mode" - {

      "must go from DateConfirmNilPaymentsPage to CheckYourAnswers Page in CheckMode" in {
        navigator.nextPage(
          DateConfirmPaymentsPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from InactivityRequestPage to CheckYourAnswers in CheckMode when inactivity request is NO" in {
        val ua = UserAnswers("id").setOrException(InactivityRequestPage, InactivityRequest.Option2)
        navigator.nextPage(
          InactivityRequestPage,
          CheckMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from SubmitInactivityRequestPage to InactivityRequestWarningController in CheckMode when inactivity request is YES" in {
        val ua = UserAnswers("id").setOrException(SubmitInactivityRequestPage, true)
        navigator.nextPage(
          SubmitInactivityRequestPage,
          CheckMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(CheckMode)
      }

      "must go from SubmitInactivityRequestPage to JourneyRecovery in CheckMode when inactivity request is missing" in {
        navigator.nextPage(
          SubmitInactivityRequestPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ConfirmEmailAddressPage to CheckYourAnswers Page in CheckMode" in {
        navigator.nextPage(
          ConfirmEmailAddressPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from SelectedSubcontractorPaymentsMadePage to CheckAnswersTotalPayments Page in CheckMode" in {
        navigator.nextPage(
          SelectedSubcontractorPaymentsMadePage(1),
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(1)
      }

      "must go from SelectedSubcontractorMaterialCostsPage to CheckAnswersTotalPayments Page in CheckMode" in {
        navigator.nextPage(
          SelectedSubcontractorMaterialCostsPage(1),
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(1)
      }

      "must go from SelectedSubcontractorTaxDeductedPage to CheckAnswersTotalPayments Page in CheckMode" in {
        navigator.nextPage(
          SelectedSubcontractorTaxDeductedPage(1),
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckAnswersTotalPaymentsController.onPageLoad(1)
      }

      "must go from EmploymentStatusDeclarationPage to CheckYourAnswers in CheckMode when answer is present" in {
        val ua = UserAnswers("id").setOrException(EmploymentStatusDeclarationPage, true)
        navigator.nextPage(
          EmploymentStatusDeclarationPage,
          CheckMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from EmploymentStatusDeclarationPage to JourneyRecovery in CheckMode when answer is missing" in {
        navigator.nextPage(
          EmploymentStatusDeclarationPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from VerifiedStatusDeclarationPage to CheckYourAnswers in CheckMode when answer is present" in {
        val ua = UserAnswers("id").setOrException(VerifiedStatusDeclarationPage, true)
        navigator.nextPage(
          VerifiedStatusDeclarationPage,
          CheckMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from VerifiedStatusDeclarationPage to JourneyRecovery in CheckMode when answer is missing" in {
        navigator.nextPage(
          VerifiedStatusDeclarationPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from SubmitInactivityRequestPage to InactivityRequestWarningController in CheckMode when answer is true" in {
        val ua = UserAnswers("id").setOrException(SubmitInactivityRequestPage, true)
        navigator.nextPage(
          SubmitInactivityRequestPage,
          CheckMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.InactivityRequestWarningController.onPageLoad(CheckMode)
      }

      "must go from SubmitInactivityRequestPage to CheckYourAnswers in CheckMode when answer is false" in {
        val ua = UserAnswers("id").setOrException(SubmitInactivityRequestPage, false)
        navigator.nextPage(
          SubmitInactivityRequestPage,
          CheckMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from SubmitInactivityRequestPage to JourneyRecovery in CheckMode when answer is missing" in {
        navigator.nextPage(
          SubmitInactivityRequestPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from ConfirmationByEmailPage to EnterYourEmailAddressController in CheckMode when answer is true" in {
        val ua = UserAnswers("id").setOrException(ConfirmationByEmailPage, true)
        navigator.nextPage(
          ConfirmationByEmailPage,
          CheckMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoad(CheckMode)
      }

      "must go from ConfirmationByEmailPage to CheckYourAnswers in CheckMode when answer is false" in {
        val ua = UserAnswers("id").setOrException(ConfirmationByEmailPage, false)
        navigator.nextPage(
          ConfirmationByEmailPage,
          CheckMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from ConfirmationByEmailPage to JourneyRecovery in CheckMode when answer is missing" in {
        navigator.nextPage(
          ConfirmationByEmailPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must go from EnterYourEmailAddressPage to CheckYourAnswers in CheckMode" in {
        navigator.nextPage(
          EnterYourEmailAddressPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController
          .onPageLoad()
      }

      "must go from WhichSubcontractorsToAddPage to SubcontractorDetailsAddedController when Amended StandardReturn" in {
        val ua = UserAnswers("id")
          .setOrException(WhichSubcontractorsToAddPage, Set.empty)
          .setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        navigator.nextPage(
          WhichSubcontractorsToAddPage,
          CheckMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode)
      }

      "must go from WhichSubcontractorsToAddPage to SubcontractorDetailsAddedController when Amended NilReturn" in {
        val ua = UserAnswers("id")
          .setOrException(WhichSubcontractorsToAddPage, Set.empty)
          .setOrException(ReturnTypePage, MonthlyAmendedNilReturn)
        navigator.nextPage(
          WhichSubcontractorsToAddPage,
          CheckMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(CheckMode)
      }
    }

    "in UUID-scoped journey" - {

      "in Normal mode" - {

        "must go from DateConfirmPaymentsPage to SubmitInactivityRequestController" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            DateConfirmPaymentsPage,
            NormalMode,
            UserAnswers("id")
          ) mustBe controllers.monthlyreturns.routes.SubmitInactivityRequestController
            .onPageLoadWithId(flowId, NormalMode)
        }

        "must go from SubmitInactivityRequestPage to InactivityRequestWarningController when answer is true" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(SubmitInactivityRequestPage(flowId.toString), true)

          navigator.nextPage(
            flowId,
            SubmitInactivityRequestPage,
            NormalMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.InactivityRequestWarningController
            .onPageLoadWithId(flowId, NormalMode)
        }

        "must go from SubmitInactivityRequestPage to ConfirmationByEmailController when answer is false" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(SubmitInactivityRequestPage(flowId.toString), false)

          navigator.nextPage(
            flowId,
            SubmitInactivityRequestPage,
            NormalMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoadWithId(flowId, NormalMode)
        }

        "must go from SubmitInactivityRequestPage to JourneyRecovery when answer is missing" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            SubmitInactivityRequestPage,
            NormalMode,
            UserAnswers("id")
          ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
        }

        "must go from ConfirmationByEmailPage to EnterYourEmailAddressController when answer is true" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(ConfirmationByEmailPage(flowId.toString), true)

          navigator.nextPage(
            flowId,
            ConfirmationByEmailPage,
            NormalMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.EnterYourEmailAddressController
            .onPageLoadWithId(flowId, NormalMode)
        }

        "must go from ConfirmationByEmailPage to DeclarationController when answer is false" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(ConfirmationByEmailPage(flowId.toString), false)

          navigator.nextPage(
            flowId,
            ConfirmationByEmailPage,
            NormalMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(flowId)
        }

        "must go from ConfirmationByEmailPage to JourneyRecovery when answer is missing" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            ConfirmationByEmailPage,
            NormalMode,
            UserAnswers("id")
          ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
        }

        "must go from EnterYourEmailAddressPage to DeclarationController" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            EnterYourEmailAddressPage,
            NormalMode,
            UserAnswers("id")
          ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(flowId)
        }

        "must go from DeclarationPage to CheckYourAnswersController" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            DeclarationPage,
            NormalMode,
            UserAnswers("id")
          ) mustBe controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
        }
      }

      "in Check mode" - {

        "must go from DateConfirmPaymentsPage to CheckYourAnswersController" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            DateConfirmPaymentsPage,
            CheckMode,
            UserAnswers("id")
          ) mustBe controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
        }

        "must go from SubmitInactivityRequestPage to InactivityRequestWarningController when answer is true" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(SubmitInactivityRequestPage(flowId.toString), true)

          navigator.nextPage(
            flowId,
            SubmitInactivityRequestPage,
            CheckMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.InactivityRequestWarningController
            .onPageLoadWithId(flowId, CheckMode)
        }

        "must go from SubmitInactivityRequestPage to CheckYourAnswersController when answer is false" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(SubmitInactivityRequestPage(flowId.toString), false)

          navigator.nextPage(
            flowId,
            SubmitInactivityRequestPage,
            CheckMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
        }

        "must go from SubmitInactivityRequestPage to JourneyRecovery when answer is missing" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            SubmitInactivityRequestPage,
            CheckMode,
            UserAnswers("id")
          ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
        }

        "must go from ConfirmationByEmailPage to EnterYourEmailAddressController when answer is true" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(ConfirmationByEmailPage(flowId.toString), true)

          navigator.nextPage(
            flowId,
            ConfirmationByEmailPage,
            CheckMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.EnterYourEmailAddressController.onPageLoadWithId(flowId, CheckMode)
        }

        "must go from ConfirmationByEmailPage to CheckYourAnswersController when answer is false" in {
          val flowId = UUID.randomUUID()
          val ua     = UserAnswers("id").setOrException(ConfirmationByEmailPage(flowId.toString), false)

          navigator.nextPage(
            flowId,
            ConfirmationByEmailPage,
            CheckMode,
            ua
          ) mustBe controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
        }

        "must go from ConfirmationByEmailPage to JourneyRecovery when answer is missing" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            ConfirmationByEmailPage,
            CheckMode,
            UserAnswers("id")
          ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
        }

        "must go from EnterYourEmailAddressPage to CheckYourAnswersController" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            EnterYourEmailAddressPage,
            CheckMode,
            UserAnswers("id")
          ) mustBe controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
        }

        "must go from DeclarationPage to CheckYourAnswersController" in {
          val flowId = UUID.randomUUID()

          navigator.nextPage(
            flowId,
            DeclarationPage,
            CheckMode,
            UserAnswers("id")
          ) mustBe controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoadWithId(flowId)
        }
      }

      "must go to JourneyRecovery when the page does not exist in the UUID-scoped route map" in {
        val flowId = UUID.randomUUID()

        case object UnknownPage extends Page

        navigator.nextPage(
          flowId,
          UnknownPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()

        navigator.nextPage(
          flowId,
          UnknownPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must not use legacy SubmitInactivityRequestPage when the scoped answer is missing" in {
        val flowId = UUID.randomUUID()
        val ua     = UserAnswers("id").setOrException(SubmitInactivityRequestPage, true)

        navigator.nextPage(
          flowId,
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must not use legacy ConfirmationByEmailPage when the scoped answer is missing" in {
        val flowId = UUID.randomUUID()
        val ua     = UserAnswers("id").setOrException(ConfirmationByEmailPage, true)

        navigator.nextPage(
          flowId,
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }

      "must navigate using the SubmitInactivityRequestPage answer for the correct flowId" in {
        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()

        val ua = UserAnswers("id")
          .setOrException(SubmitInactivityRequestPage(firstFlowId.toString), true)
          .setOrException(SubmitInactivityRequestPage(secondFlowId.toString), false)

        navigator.nextPage(
          firstFlowId,
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.InactivityRequestWarningController
          .onPageLoadWithId(firstFlowId, NormalMode)

        navigator.nextPage(
          secondFlowId,
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.ConfirmationByEmailController
          .onPageLoadWithId(secondFlowId, NormalMode)
      }

      "must navigate using the ConfirmationByEmailPage answer for the correct flowId" in {
        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()

        val ua = UserAnswers("id")
          .setOrException(ConfirmationByEmailPage(firstFlowId.toString), true)
          .setOrException(ConfirmationByEmailPage(secondFlowId.toString), false)

        navigator.nextPage(
          firstFlowId,
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.EnterYourEmailAddressController
          .onPageLoadWithId(firstFlowId, NormalMode)

        navigator.nextPage(
          secondFlowId,
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoadWithId(secondFlowId)
      }

      "must go to JourneyRecovery when the answer exists only for another flowId" in {
        val firstFlowId  = UUID.randomUUID()
        val secondFlowId = UUID.randomUUID()

        val ua = UserAnswers("id")
          .setOrException(SubmitInactivityRequestPage(firstFlowId.toString), true)
          .setOrException(ConfirmationByEmailPage(firstFlowId.toString), true)

        navigator.nextPage(
          secondFlowId,
          SubmitInactivityRequestPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()

        navigator.nextPage(
          secondFlowId,
          ConfirmationByEmailPage,
          NormalMode,
          ua
        ) mustBe controllers.routes.JourneyRecoveryController.onPageLoad()
      }
    }
  }

  private val completeSub = SelectedSubcontractor(
    id = 1L,
    name = "Sub",
    totalPaymentsMade = Some(100),
    costOfMaterials = Some(50),
    totalTaxDeducted = Some(20)
  )

  private val incompleteSub = SelectedSubcontractor(
    id = 2L,
    name = "Sub",
    totalPaymentsMade = None,
    costOfMaterials = None,
    totalTaxDeducted = None
  )
}
