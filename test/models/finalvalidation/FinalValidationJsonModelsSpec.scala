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

class FinalValidationJsonModelsSpec extends SpecBase {

  "FinalValidationHandoffPayload" - {

    "serialise and deserialise" in {
      val payload = FinalValidationHandoffPayload(
        draftId = "draft-1",
        instanceId = "instance-1",
        subcontractorId = 1L,
        subbieResourceRef = 100L,
        field = FinalValidationField.Utr,
        changeTarget = FinalValidationChangeTarget.Utr
      )

      Json.toJson(payload).as[FinalValidationHandoffPayload] mustBe payload
    }
  }

  "FinalValidationIssue" - {

    "serialise and deserialise" in {
      val issue = FinalValidationIssue(
        field = FinalValidationField.Utr,
        value = Some("1234567890")
      )

      Json.toJson(issue).as[FinalValidationIssue] mustBe issue
    }
  }

  "SubcontractorFinalValidationFailure" - {

    "serialise and deserialise" in {
      val failure = SubcontractorFinalValidationFailure(
        subcontractorId = 1L,
        issues = Seq(
          FinalValidationIssue(
            FinalValidationField.Utr,
            Some("1234567890")
          )
        ),
        subbieResourceRef = Some(100L)
      )

      Json.toJson(failure).as[SubcontractorFinalValidationFailure] mustBe failure
    }
  }
}
