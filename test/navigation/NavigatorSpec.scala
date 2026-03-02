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
import pages._
import pages.monthlyreturns.*
import models._
import models.monthlyreturns.InactivityRequest

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "Navigator" - {

    "in Normal mode" - {

      "must go from DateConfirmNilPaymentsPage to InactivityRequestController" in {
        navigator.nextPage(
          DateConfirmNilPaymentsPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.InactivityRequestController.onPageLoad(NormalMode)
      }

      "must go from InactivityRequestPage to ConfirmEmailAddressController" in {
        navigator.nextPage(
          InactivityRequestPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.ConfirmEmailAddressController.onPageLoad(NormalMode)
      }

      "must go from ConfirmEmailAddressPage to DeclarationController" in {
        navigator.nextPage(
          ConfirmEmailAddressPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.DeclarationController.onPageLoad(NormalMode)
      }

      "must go from DeclarationPage to CheckYourAnswers when inactivity request is NO" in {
        val ua = UserAnswers("id").setOrException(InactivityRequestPage, InactivityRequest.Option2)
        navigator.nextPage(
          DeclarationPage,
          NormalMode,
          ua
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from DeclarationPage to InactivityWarning when inactivity request is YES" in {
        val ua = UserAnswers("id").setOrException(InactivityRequestPage, InactivityRequest.Option1)
        navigator.nextPage(
          DeclarationPage,
          NormalMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
      }

      "must go from DeclarationPage to InactivityWarning when inactivity request is missing" in {
        navigator.nextPage(
          DeclarationPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
      }

      "must go from InactivityWarningPage to CheckYourAnswers" in {
        navigator.nextPage(
          InactivityWarningPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
      }

      "must go from VerifySubcontractorsPage to PaymentDetailsController" in {
        navigator.nextPage(
          VerifySubcontractorsPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.PaymentDetailsController.onPageLoad(NormalMode, 1, None)
      }

      "must go from DateConfirmPaymentsPage to SelectSubcontractorsController" in {
        navigator.nextPage(
          DateConfirmPaymentsPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
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

      "must go from a page that doesn't exist in the route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          NormalMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController
          .onPageLoad()
      }
    }

    "in Check mode" - {

      "must go from DateConfirmNilPaymentsPage to CheckYourAnswers Page in CheckMode" in {
        navigator.nextPage(
          DateConfirmNilPaymentsPage,
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

      "must go from InactivityRequestPage to InactivityWarning in CheckMode when inactivity request is YES" in {
        val ua = UserAnswers("id").setOrException(InactivityRequestPage, InactivityRequest.Option1)
        navigator.nextPage(
          InactivityRequestPage,
          CheckMode,
          ua
        ) mustBe controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
      }

      "must go from InactivityRequestPage to InactivityWarning in CheckMode when inactivity request is missing" in {
        navigator.nextPage(
          InactivityRequestPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
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

      "must go from a page that doesn't exist in the edit route map to CheckYourAnswers" in {

        case object UnknownPage extends Page
        navigator.nextPage(
          UnknownPage,
          CheckMode,
          UserAnswers("id")
        ) mustBe monthlyreturns.routes.CheckYourAnswersController
          .onPageLoad()
      }
    }
  }
}
