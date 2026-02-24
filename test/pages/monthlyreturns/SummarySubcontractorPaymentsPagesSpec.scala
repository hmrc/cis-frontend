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

package pages.monthlyreturns

import base.SpecBase
import play.api.libs.json.JsPath

class SummarySubcontractorPaymentsPagesSpec extends SpecBase {

  "SummaryTotalPaymentsPage" - {

    "must have the correct toString and path" in {
      SummaryTotalPaymentsPage.toString mustBe "summaryTotalPayments"
      SummaryTotalPaymentsPage.path mustBe (JsPath \ "summaryTotalPayments")
    }

    "must store and retrieve a value in UserAnswers" in {
      val result = emptyUserAnswers.set(SummaryTotalPaymentsPage, BigDecimal(3600.00)).success.value
      result.get(SummaryTotalPaymentsPage) mustBe Some(BigDecimal(3600.00))
    }

    "must return None when no value has been set" in {
      emptyUserAnswers.get(SummaryTotalPaymentsPage) mustBe None
    }
  }

  "SummaryTotalMaterialsCostPage" - {

    "must have the correct toString and path" in {
      SummaryTotalMaterialsCostPage.toString mustBe "summaryTotalMaterialsCost"
      SummaryTotalMaterialsCostPage.path mustBe (JsPath \ "summaryTotalMaterialsCost")
    }

    "must store and retrieve a value in UserAnswers" in {
      val result = emptyUserAnswers.set(SummaryTotalMaterialsCostPage, BigDecimal(900.00)).success.value
      result.get(SummaryTotalMaterialsCostPage) mustBe Some(BigDecimal(900.00))
    }

    "must return None when no value has been set" in {
      emptyUserAnswers.get(SummaryTotalMaterialsCostPage) mustBe None
    }
  }

  "SummaryTotalCisDeductionsPage" - {

    "must have the correct toString and path" in {
      SummaryTotalCisDeductionsPage.toString mustBe "summaryTotalCisDeductions"
      SummaryTotalCisDeductionsPage.path mustBe (JsPath \ "summaryTotalCisDeductions")
    }

    "must store and retrieve a value in UserAnswers" in {
      val result = emptyUserAnswers.set(SummaryTotalCisDeductionsPage, BigDecimal(540.00)).success.value
      result.get(SummaryTotalCisDeductionsPage) mustBe Some(BigDecimal(540.00))
    }

    "must return None when no value has been set" in {
      emptyUserAnswers.get(SummaryTotalCisDeductionsPage) mustBe None
    }
  }

  "all three pages" - {

    "must coexist independently in the same UserAnswers" in {
      val result = emptyUserAnswers
        .set(SummaryTotalPaymentsPage, BigDecimal(3600.00))
        .success
        .value
        .set(SummaryTotalMaterialsCostPage, BigDecimal(900.00))
        .success
        .value
        .set(SummaryTotalCisDeductionsPage, BigDecimal(540.00))
        .success
        .value

      result.get(SummaryTotalPaymentsPage) mustBe Some(BigDecimal(3600.00))
      result.get(SummaryTotalMaterialsCostPage) mustBe Some(BigDecimal(900.00))
      result.get(SummaryTotalCisDeductionsPage) mustBe Some(BigDecimal(540.00))
    }
  }
}
