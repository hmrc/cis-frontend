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

package models.finalvalidation

import base.SpecBase

class FinalValidationResultSpec extends SpecBase {

  private val failure =
    SubcontractorFinalValidationFailure(
      subcontractorId = 1L,
      issues = Seq.empty,
      subbieResourceRef = Some(100L)
    )

  "FinalValidationResult" - {

    "hasErrors should indicate whether failures exist" in {
      FinalValidationResult(Seq.empty).hasErrors mustBe false
      FinalValidationResult(Seq(failure)).hasErrors mustBe true
    }

    "erroneousSubcontractorIds should return failed subcontractor ids" in {
      FinalValidationResult(Seq(failure)).erroneousSubcontractorIds mustBe Seq(1L)
    }

    "failureFor should find a failure by subcontractor id" in {
      val result = FinalValidationResult(Seq(failure))

      result.failureFor(1L) mustBe Some(failure)
      result.failureFor(999L) mustBe None
    }
  }
}
