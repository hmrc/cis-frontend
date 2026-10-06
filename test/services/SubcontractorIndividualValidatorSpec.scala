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
import models.monthlyreturns.Subcontractor
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock

class SubcontractorIndividualValidatorSpec extends SpecBase {

  private val validator =
    new SubcontractorIndividualValidator()

  "SubcontractorIndividualValidator.validate" - {

    "return no failures for an empty subcontractor list" in {
      validator.validate(Seq.empty) mustBe Nil
    }

    "ignore a subcontractor that is not an individual" in {
      val subcontractor = mock[Subcontractor]

      when(subcontractor.subcontractorType).thenReturn(Some("company"))

      validator.validate(
        Seq(subcontractor)
      ) mustBe Nil
    }

    "ignore a subcontractor with an invalid subcontractor type" in {
      val subcontractor = mock[Subcontractor]

      when(subcontractor.subcontractorType).thenReturn(Some("invalid"))

      validator.validate(
        Seq(subcontractor)
      ) mustBe Nil
    }

    "ignore a subcontractor with no subcontractor type" in {
      val subcontractor = mock[Subcontractor]

      when(subcontractor.subcontractorType).thenReturn(None)

      validator.validate(
        Seq(subcontractor)
      ) mustBe Nil
    }
  }
}
