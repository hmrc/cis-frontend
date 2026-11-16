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

import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyNilReturn, MonthlyStandardReturn}
import models.{ReturnType, UserAnswers}
import models.amend.AmendmentDetails
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
        .set(SubmitInactivityRequestPage, true)
        .success
        .value
        .set(DeclarationPage, Set(Declaration.Confirmed))
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua)

      result shouldBe Right(
        UpdateMonthlyReturnRequest(
          instanceId = "CIS-123",
          taxYear = 2024,
          taxMonth = 3,
          amendment = "N",
          decNoMoreSubPayments = Some("Y"),
          decNilReturnNoPayments = Some("Y"),
          decInformationCorrect = Some("Y"),
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

  "UpdateMonthlyReturnRequest.forStartedStandardAmendment" should {

    "build a started standard amendment request from amendment details" in {
      val amendmentDetails = AmendmentDetails(
        instanceId = "CIS-123",
        taxYear = 2025,
        taxMonth = 4,
        originalReturnType = MonthlyNilReturn,
        contractorName = "Test Contractor",
        acceptedTime = Some("2025-04-01T12:00:00Z")
      )

      val result =
        UpdateMonthlyReturnRequest.forStartedStandardAmendment(amendmentDetails)

      result shouldBe UpdateMonthlyReturnRequest(
        instanceId = "CIS-123",
        taxYear = 2025,
        taxMonth = 4,
        amendment = "Y",
        nilReturnIndicator = "N",
        status = "STARTED"
      )
    }
  }

  "UpdateMonthlyReturnRequest.fromUserAnswers with a journey id" should {

    val id = "5f0c1a2e-0000-4000-8000-000000000001"

    def scopedBase(returnType: ReturnType, cisId: String = "CIS-123", date: LocalDate = LocalDate.of(2024, 3, 1)) =
      UserAnswers("test-user")
        .set(ReturnTypePage(id), returnType)
        .success
        .value
        .set(CisIdPage(id), cisId)
        .success
        .value
        .set(DateConfirmPaymentsPage(id), date)
        .success
        .value

    "build a request for MonthlyNilReturn (minimal required answers)" in {
      val ua = scopedBase(MonthlyNilReturn)
        .set(SubmitInactivityRequestPage(id), true)
        .success
        .value
        .set(DeclarationPage(id), Set(Declaration.Confirmed))
        .success
        .value

      UpdateMonthlyReturnRequest.fromUserAnswers(ua, id) shouldBe Right(
        UpdateMonthlyReturnRequest(
          instanceId = "CIS-123",
          taxYear = 2024,
          taxMonth = 3,
          amendment = "N",
          decNoMoreSubPayments = Some("Y"),
          decNilReturnNoPayments = Some("Y"),
          decInformationCorrect = Some("Y"),
          nilReturnIndicator = "Y",
          status = "STARTED",
          version = None
        )
      )
    }

    "build a request for MonthlyStandardReturn with standard declarations" in {
      val ua = scopedBase(MonthlyStandardReturn, cisId = "CIS-456", date = LocalDate.of(2024, 4, 1))
        .set(PaymentDetailsConfirmationPage(id), true)
        .success
        .value
        .set(EmploymentStatusDeclarationPage(id), true)
        .success
        .value
        .set(VerifiedStatusDeclarationPage(id), false)
        .success
        .value
        .set(SubmitInactivityRequestPage(id), true)
        .success
        .value

      UpdateMonthlyReturnRequest.fromUserAnswers(ua, id) shouldBe Right(
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

    "build a request for MonthlyStandardReturn without inactivity when SubmitInactivityRequestPage is false" in {
      val ua = scopedBase(MonthlyStandardReturn, cisId = "CIS-789", date = LocalDate.of(2024, 5, 1))
        .set(PaymentDetailsConfirmationPage(id), false)
        .success
        .value
        .set(EmploymentStatusDeclarationPage(id), false)
        .success
        .value
        .set(VerifiedStatusDeclarationPage(id), true)
        .success
        .value
        .set(SubmitInactivityRequestPage(id), false)
        .success
        .value

      UpdateMonthlyReturnRequest.fromUserAnswers(ua, id) shouldBe Right(
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
      val ua = scopedBase(MonthlyStandardReturn)
        .set(PaymentDetailsConfirmationPage(id), true)
        .success
        .value

      UpdateMonthlyReturnRequest.fromUserAnswers(ua, id).toOption.get.decNoMoreSubPayments shouldBe None
    }

    "set decInformationCorrect when DeclarationPage has a value" in {
      val ua = scopedBase(MonthlyNilReturn)
        .set(DeclarationPage(id), Set(Declaration.Confirmed))
        .success
        .value

      UpdateMonthlyReturnRequest.fromUserAnswers(ua, id).toOption.get.decInformationCorrect shouldBe Some("Y")
    }

    "leave the nil declaration fields empty when the declaration set is empty" in {
      val ua = scopedBase(MonthlyNilReturn)
        .set(DeclarationPage(id), Set.empty[Declaration])
        .success
        .value

      val result = UpdateMonthlyReturnRequest.fromUserAnswers(ua, id).toOption.get

      result.decInformationCorrect  shouldBe None
      result.decNilReturnNoPayments shouldBe None
    }

    "leave the standard declaration fields empty when they are missing" in {
      val result = UpdateMonthlyReturnRequest.fromUserAnswers(scopedBase(MonthlyStandardReturn), id).toOption.get

      result.decEmpStatusConsidered shouldBe None
      result.decAllSubsVerified     shouldBe None
      result.decInformationCorrect  shouldBe None
      result.nilReturnIndicator     shouldBe "N"
    }

    "use the amendment flag of an amended return type" in {
      val result = UpdateMonthlyReturnRequest.fromUserAnswers(scopedBase(MonthlyAmendedStandardReturn), id).toOption.get

      result.amendment          shouldBe MonthlyAmendedStandardReturn.amendmentFlag
      result.nilReturnIndicator shouldBe "N"

      UpdateMonthlyReturnRequest
        .fromUserAnswers(scopedBase(MonthlyAmendedNilReturn), id)
        .toOption
        .get
        .nilReturnIndicator shouldBe "Y"
    }

    "ignore the legacy unscoped answers" in {
      val legacyOnly = UserAnswers("test-user")
        .set(ReturnTypePage, MonthlyNilReturn)
        .success
        .value
        .set(CisIdPage, "CIS-123")
        .success
        .value
        .set(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
        .success
        .value

      UpdateMonthlyReturnRequest.fromUserAnswers(legacyOnly, id) shouldBe Left("Missing return type")
    }

    "not read another journey's answers" in {
      UpdateMonthlyReturnRequest.fromUserAnswers(scopedBase(MonthlyNilReturn), "another-flow-id") shouldBe
        Left("Missing return type")
    }

    "report each missing scoped answer in order" in {
      UpdateMonthlyReturnRequest.fromUserAnswers(UserAnswers("test-user"), id) shouldBe Left("Missing return type")

      val noCisId = UserAnswers("test-user").set(ReturnTypePage(id), MonthlyNilReturn).success.value
      UpdateMonthlyReturnRequest.fromUserAnswers(noCisId, id) shouldBe Left("Missing instanceId")

      val noDate = noCisId.set(CisIdPage(id), "CIS-123").success.value
      UpdateMonthlyReturnRequest.fromUserAnswers(noDate, id) shouldBe Left("Missing confirmed payment date")
    }
  }
}
