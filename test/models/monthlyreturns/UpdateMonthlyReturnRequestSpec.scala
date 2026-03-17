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

package models.monthlyreturns

import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.UserAnswers
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.TryValues
import pages.monthlyreturns.*
import play.api.libs.json.{JsSuccess, Json}

import java.time.LocalDate

class UpdateMonthlyReturnRequestSpec extends AnyWordSpec with Matchers with TryValues {

  "UpdateMonthlyReturnRequest.format" should {

    "serialize and deserialize (round-trip) model" in {
      val model = UpdateMonthlyReturnRequest(
        instanceId = "instance-123",
        taxYear = 2024,
        taxMonth = 1,
        amendment = "N",
        nilReturnIndicator = "N",
        status = "STARTED"
      )

      val json = Json.toJson(model)
      json.validate[UpdateMonthlyReturnRequest] shouldBe JsSuccess(model)
    }
  }

  "UpdateMonthlyReturnRequest.fromUserAnswers" should {

    "build a request for MonthlyNilReturn (minimal required answers)" in {
      val ua = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value
        .set(CisIdPage, "CIS-123")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua)

      result shouldBe Right(
        UpdateMonthlyReturnRequest(
          instanceId = "CIS-123",
          taxYear = 2024,
          taxMonth = 3,
          amendment = "N",
          decNilReturnNoPayments = Some("Y"),
          decInformationCorrect = None,
          nilReturnIndicator = "Y",
          status = "STARTED",
          version = None
        )
      )
    }

    "build a request for MonthlyStandardReturn with standard declarations" in {
      val ua = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(CisIdPage, "CIS-456")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 4, 1))
        .success
        .value
        .set(PaymentDetailsConfirmationPage, true)
        .success
        .value
        .set(EmploymentStatusDeclarationPage, true)
        .success
        .value
        .set(VerifiedStatusDeclarationPage, false)
        .success
        .value
        .set(SubmitInactivityRequestPage, true)
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua)

      result shouldBe Right(
        UpdateMonthlyReturnRequest(
          instanceId = "CIS-456",
          taxYear = 2024,
          taxMonth = 4,
          amendment = "N",
          decEmpStatusConsidered = Some("Y"),
          decAllSubsVerified = Some("N"),
          decNoMoreSubPayments = Some("Y"),
          decInformationCorrect = Some("Y"),
          nilReturnIndicator = "N",
          status = "STARTED",
          version = None
        )
      )
    }

    "build a request for MonthlyStandardReturn without optional inactivity when SubmitInactivityRequestPage is false" in {
      val ua = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(CisIdPage, "CIS-789")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 5, 1))
        .success
        .value
        .set(PaymentDetailsConfirmationPage, false)
        .success
        .value
        .set(EmploymentStatusDeclarationPage, false)
        .success
        .value
        .set(VerifiedStatusDeclarationPage, true)
        .success
        .value
        .set(SubmitInactivityRequestPage, false)
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua)

      result shouldBe Right(
        UpdateMonthlyReturnRequest(
          instanceId = "CIS-789",
          taxYear = 2024,
          taxMonth = 5,
          amendment = "N",
          decEmpStatusConsidered = Some("N"),
          decAllSubsVerified = Some("Y"),
          decNoMoreSubPayments = None,
          decInformationCorrect = Some("N"),
          nilReturnIndicator = "N",
          status = "STARTED",
          version = None
        )
      )
    }

    "not include inactivity when SubmitInactivityRequestPage is missing" in {
      val ua = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(CisIdPage, "CIS-999")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 6, 1))
        .success
        .value
        .set(PaymentDetailsConfirmationPage, true)
        .success
        .value
        .set(EmploymentStatusDeclarationPage, true)
        .success
        .value
        .set(VerifiedStatusDeclarationPage, true)
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua).toOption.get

      result.decNoMoreSubPayments shouldBe None
    }

    "set decInformationCorrect when DeclarationPage has a value" in {
      val ua = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value
        .set(CisIdPage, "CIS-321")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 7, 1))
        .success
        .value
        .set(InactivityRequestPage, InactivityRequest.Option1)
        .success
        .value
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua).toOption.get

      result.decInformationCorrect shouldBe Some("Y")
    }
  }
}
