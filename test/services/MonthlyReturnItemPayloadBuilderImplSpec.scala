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

package services

import base.SpecBase
import models.ReturnType
import models.monthlyreturns.SelectedSubcontractor
import org.scalatest.matchers.must.Matchers
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, ReturnTypePage, SelectedSubcontractorForFlowPage, SelectedSubcontractorPage}

import java.time.LocalDate

class MonthlyReturnItemPayloadBuilderImplSpec extends SpecBase with Matchers {

  private val builder = new MonthlyReturnItemPayloadBuilderImpl()

  private val index      = 1
  private val instanceId = "i-123"
  private val monthDate  = LocalDate.of(2026, 3, 1)

  private val subcontractor = SelectedSubcontractor(
    id = 99,
    name = "Tyne Test Ltd",
    totalPaymentsMade = Some(BigDecimal("1200")),
    costOfMaterials = Some(BigDecimal("500")),
    totalTaxDeducted = Some(BigDecimal("240"))
  )

  "MonthlyReturnItemPayloadBuilderImpl" - {

    "must build an UpdateMonthlyReturnItemRequest when required answers exist" in {
      val ua =
        emptyUserAnswers
          .set(CisIdPage, instanceId)
          .success
          .value
          .set(DateConfirmPaymentsPage, monthDate)
          .success
          .value
          .set(SelectedSubcontractorPage(index), subcontractor)
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value

      val result = builder.build(ua, index).value

      result.instanceId mustBe instanceId
      result.taxYear mustBe 2026
      result.taxMonth mustBe 3
      result.subcontractorId mustBe 99
      result.subcontractorName mustBe "Tyne Test Ltd"

      result.totalPayments mustBe "1,200.00"
      result.costOfMaterials mustBe "500.00"
      result.totalDeducted mustBe "240.00"
    }

    "must default costOfMaterials and totalDeducted to 0.00 when missing" in {
      val subcontractorMissingOptionals =
        subcontractor.copy(costOfMaterials = None, totalTaxDeducted = None)

      val ua =
        emptyUserAnswers
          .set(CisIdPage, instanceId)
          .success
          .value
          .set(DateConfirmPaymentsPage, monthDate)
          .success
          .value
          .set(SelectedSubcontractorPage(index), subcontractorMissingOptionals)
          .success
          .value
          .set(ReturnTypePage, ReturnType.MonthlyStandardReturn)
          .success
          .value

      val result = builder.build(ua, index).value

      result.totalPayments mustBe "1,200.00"
      result.costOfMaterials mustBe "0.00"
      result.totalDeducted mustBe "0.00"
    }

    "must return None when CisIdPage is missing" in {
      val ua =
        emptyUserAnswers
          .set(DateConfirmPaymentsPage, monthDate)
          .success
          .value
          .set(SelectedSubcontractorPage(index), subcontractor)
          .success
          .value

      builder.build(ua, index) mustBe None
    }

    "must return None when DateConfirmPaymentsPage is missing" in {
      val ua =
        emptyUserAnswers
          .set(CisIdPage, instanceId)
          .success
          .value
          .set(SelectedSubcontractorPage(index), subcontractor)
          .success
          .value

      builder.build(ua, index) mustBe None
    }

    "must return None when SelectedSubcontractorPage(index) is missing" in {
      val ua =
        emptyUserAnswers
          .set(CisIdPage, instanceId)
          .success
          .value
          .set(DateConfirmPaymentsPage, monthDate)
          .success
          .value

      builder.build(ua, index) mustBe None
    }

    "must return None when totalPaymentsMade is missing (required subcontractor field)" in {
      val subcontractorMissingTotalPayments =
        subcontractor.copy(totalPaymentsMade = None)

      val ua =
        emptyUserAnswers
          .set(CisIdPage, instanceId)
          .success
          .value
          .set(DateConfirmPaymentsPage, monthDate)
          .success
          .value
          .set(SelectedSubcontractorPage(index), subcontractorMissingTotalPayments)
          .success
          .value

      builder.build(ua, index) mustBe None
    }

    "must build an UpdateMonthlyReturnItemRequest using ID-scoped answers" in {
      val id = "test-id"

      val ua = emptyUserAnswers
        .setOrException(CisIdPage(id), instanceId)
        .setOrException(DateConfirmPaymentsPage(id), monthDate)
        .setOrException(SelectedSubcontractorForFlowPage(id, index), subcontractor)
        .setOrException(ReturnTypePage(id), ReturnType.MonthlyStandardReturn)

      val result = builder.build(ua, id, index).value

      result.instanceId mustBe instanceId
      result.taxYear mustBe 2026
      result.taxMonth mustBe 3
      result.subcontractorId mustBe 99
      result.subcontractorName mustBe "Tyne Test Ltd"
      result.totalPayments mustBe "1,200.00"
      result.costOfMaterials mustBe "500.00"
      result.totalDeducted mustBe "240.00"
      result.amendment mustBe "N"
    }

    "must default costOfMaterials and totalDeducted to 0.00 when missing with id" in {
      val id = "test-id"

      val subcontractorMissingOptionals =
        subcontractor.copy(costOfMaterials = None, totalTaxDeducted = None)

      val ua = emptyUserAnswers
        .setOrException(CisIdPage(id), instanceId)
        .setOrException(DateConfirmPaymentsPage(id), monthDate)
        .setOrException(SelectedSubcontractorForFlowPage(id, index), subcontractorMissingOptionals)
        .setOrException(ReturnTypePage(id), ReturnType.MonthlyStandardReturn)

      val result = builder.build(ua, id, index).value

      result.totalPayments mustBe "1,200.00"
      result.costOfMaterials mustBe "0.00"
      result.totalDeducted mustBe "0.00"
    }

    "must return None when required ID-scoped answers are missing" in {
      val id = "test-id"

      val ua = emptyUserAnswers
        .setOrException(CisIdPage(id), instanceId)
        .setOrException(DateConfirmPaymentsPage(id), monthDate)
        .setOrException(ReturnTypePage(id), ReturnType.MonthlyStandardReturn)

      builder.build(ua, id, index) mustBe None

      val withSubcontractor = ua.setOrException(
        SelectedSubcontractorForFlowPage(id, index),
        subcontractor.copy(totalPaymentsMade = None)
      )

      builder.build(withSubcontractor, id, index) mustBe None
    }

    "must not use legacy answers when ID-scoped answers are missing" in {
      val id = "test-id"

      val ua = emptyUserAnswers
        .setOrException(CisIdPage, instanceId)
        .setOrException(DateConfirmPaymentsPage, monthDate)
        .setOrException(SelectedSubcontractorPage(index), subcontractor)
        .setOrException(ReturnTypePage, ReturnType.MonthlyStandardReturn)

      builder.build(ua, id, index) mustBe None
    }

    "must use answers from the correct ID-scoped journey" in {
      val ua = emptyUserAnswers
        .setOrException(CisIdPage("first-id"), "CIS-123")
        .setOrException(DateConfirmPaymentsPage("first-id"), LocalDate.of(2026, 3, 1))
        .setOrException(SelectedSubcontractorForFlowPage("first-id", index), subcontractor)
        .setOrException(ReturnTypePage("first-id"), ReturnType.MonthlyStandardReturn)
        .setOrException(CisIdPage("second-id"), "CIS-456")
        .setOrException(DateConfirmPaymentsPage("second-id"), LocalDate.of(2026, 4, 1))
        .setOrException(SelectedSubcontractorForFlowPage("second-id", index), subcontractor.copy(id = 100))
        .setOrException(ReturnTypePage("second-id"), ReturnType.MonthlyAmendedStandardReturn)

      val firstResult  = builder.build(ua, "first-id", index).value
      val secondResult = builder.build(ua, "second-id", index).value

      firstResult.instanceId mustBe "CIS-123"
      firstResult.taxMonth mustBe 3
      firstResult.subcontractorId mustBe 99
      firstResult.amendment mustBe "N"

      secondResult.instanceId mustBe "CIS-456"
      secondResult.taxMonth mustBe 4
      secondResult.subcontractorId mustBe 100
      secondResult.amendment mustBe "Y"
    }
  }
}
