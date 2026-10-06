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
import play.api.libs.json.*

class FinalValidationDraftSpec extends SpecBase {

  private val details = FinalValidationSubcontractorDetails(
    firstName = Some("John"),
    surname = Some("Smith")
  )

  private def subcontractor(
    id: Long,
    readiness: FinalValidationReadiness
  ) =
    FinalValidationDraftSubcontractor(
      subcontractorId = id,
      subbieResourceRef = 100L,
      baseVersion = Some(1),
      subcontractorType = Some("Individual"),
      displayName = "John Smith",
      base = details,
      proposed = details,
      changedTargets = Set.empty,
      issues = Seq.empty,
      readiness = readiness
    )

  "FinalValidationReadiness" - {

    "serialise and deserialise" in {
      Json.toJson[FinalValidationReadiness](FinalValidationReadiness.Complete) mustBe
        JsString("Complete")

      Json.fromJson[FinalValidationReadiness](JsString("Incomplete")) mustBe
        JsSuccess(FinalValidationReadiness.Incomplete)
    }

    "reject an unknown value" in {
      Json.fromJson[FinalValidationReadiness](JsString("Unknown")) mustBe a[JsError]
    }
  }

  "FinalValidationDraft" - {

    "find a subcontractor" in {
      val expected = subcontractor(1L, FinalValidationReadiness.Complete)

      val draft = FinalValidationDraft(Seq(expected))

      draft.subcontractor(1L) mustBe Some(expected)
      draft.subcontractor(999L) mustBe None
    }

    "return true when all subcontractors are complete" in {
      val draft = FinalValidationDraft(
        Seq(
          subcontractor(1L, FinalValidationReadiness.Complete),
          subcontractor(2L, FinalValidationReadiness.Complete)
        )
      )

      draft.allComplete mustBe true
    }

    "return false when a subcontractor is incomplete" in {
      val draft = FinalValidationDraft(
        Seq(
          subcontractor(1L, FinalValidationReadiness.Complete),
          subcontractor(2L, FinalValidationReadiness.Incomplete)
        )
      )

      draft.allComplete mustBe false
    }

    "serialise and deserialise" in {
      val draft = FinalValidationDraft(
        Seq(subcontractor(1L, FinalValidationReadiness.Complete))
      )

      Json.toJson(draft).as[FinalValidationDraft] mustBe draft
    }
  }
}
