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

package utils

import base.SpecBase
import models.{ReturnType, UserAnswers}
import models.monthlyreturns.{Declaration, InactivityRequest, SelectedSubcontractor}
import pages.monthlyreturns.*
import utils.UserAnswerUtils.*

class UserAnswerUtilsSpec extends SpecBase {

  "UserAnswerUtils.firstIncompleteSubcontractorIndex" - {

    "returns 1 when there are no subcontractors" in {
      UserAnswers("id").firstIncompleteSubcontractorIndex mustBe 1
    }

    "returns 1 when all subcontractors are complete" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(0), completeSub(0))
        .setOrException(SelectedSubcontractorPage(1), completeSub(1))

      ua.firstIncompleteSubcontractorIndex mustBe 1
    }

    "returns the index of the single incomplete subcontractor" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(3), incompleteSub(3))

      ua.firstIncompleteSubcontractorIndex mustBe 3
    }

    "returns the minimum index among multiple incomplete subcontractors" in {
      val ua = UserAnswers("id")
        .setOrException(SelectedSubcontractorPage(0), completeSub(0))
        .setOrException(SelectedSubcontractorPage(2), incompleteSub(2))
        .setOrException(SelectedSubcontractorPage(5), incompleteSub(5))

      ua.firstIncompleteSubcontractorIndex mustBe 2
    }
  }

  "UserAnswerUtils.clearMonthlyReturnJourney" - {

    "removes all monthly return journey pages from UserAnswers" in {
      val ua = UserAnswers("id")
        .set(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1)).get
        .set(InactivityRequestPage, InactivityRequest.Option1).get
        .set(ConfirmationByEmailPage, true).get
        .set(EnterYourEmailAddressPage, "test@example.com").get
        .set(DeclarationPage, Set(Declaration.Confirmed)).get
        .set(SelectedSubcontractorPage(1), completeSub(1)).get
        .set(VerifySubcontractorsPage, true).get
        .set(SubcontractorDetailsAddedPage, true).get
        .set(PaymentDetailsConfirmationPage, true).get
        .set(EmploymentStatusDeclarationPage, true).get
        .set(VerifiedStatusDeclarationPage, true).get
        .set(SubmitInactivityRequestPage, true).get
        .set(ConfirmEmailAddressPage, "test@example.com").get

      val result = ua.clearMonthlyReturnJourney

      result.isSuccess mustBe true
      val cleared = result.get

      cleared.get(DateConfirmPaymentsPage) mustBe None
      cleared.get(InactivityRequestPage) mustBe None
      cleared.get(ConfirmationByEmailPage) mustBe None
      cleared.get(EnterYourEmailAddressPage) mustBe None
      cleared.get(DeclarationPage) mustBe None
      cleared.get(SelectedSubcontractorPage(1)) mustBe None
      cleared.get(VerifySubcontractorsPage) mustBe None
      cleared.get(SubcontractorDetailsAddedPage) mustBe None
      cleared.get(PaymentDetailsConfirmationPage) mustBe None
      cleared.get(EmploymentStatusDeclarationPage) mustBe None
      cleared.get(VerifiedStatusDeclarationPage) mustBe None
      cleared.get(SubmitInactivityRequestPage) mustBe None
      cleared.get(ConfirmEmailAddressPage) mustBe None
    }

    "retains non-journey pages such as CisIdPage and ReturnTypePage" in {
      val ua = UserAnswers("id")
        .set(CisIdPage, "CIS-123").get
        .set(ReturnTypePage, ReturnType.MonthlyStandardReturn).get
        .set(DateConfirmPaymentsPage, java.time.LocalDate.of(2025, 1, 1)).get
        .set(VerifySubcontractorsPage, true).get

      val result = ua.clearMonthlyReturnJourney

      result.isSuccess mustBe true
      val cleared = result.get

      cleared.get(CisIdPage) mustBe Some("CIS-123")
      cleared.get(ReturnTypePage) mustBe Some(ReturnType.MonthlyStandardReturn)
      cleared.get(DateConfirmPaymentsPage) mustBe None
      cleared.get(VerifySubcontractorsPage) mustBe None
    }

    "succeeds on empty UserAnswers" in {
      val result = UserAnswers("id").clearMonthlyReturnJourney
      result.isSuccess mustBe true
    }
  }

  private def completeSub(index: Int) = SelectedSubcontractor(
    id = index.toLong,
    name = s"Sub $index",
    totalPaymentsMade = Some(100),
    costOfMaterials = Some(50),
    totalTaxDeducted = Some(20)
  )

  private def incompleteSub(index: Int) = SelectedSubcontractor(
    id = index.toLong,
    name = s"Sub $index",
    totalPaymentsMade = None,
    costOfMaterials = None,
    totalTaxDeducted = None
  )
}
