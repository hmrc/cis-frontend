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

package models.amend

import base.SpecBase
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import play.api.libs.json.Json

class AmendmentDetailsSpec extends SpecBase {

  "AmendmentDetails" - {

    "must round-trip JSON" in {
      val model = AmendmentDetails(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        contractorName = "Test Contractor",
        originalReturnType = MonthlyStandardReturn,
        acceptedTime = Some("2025-04-01T12:00:00Z")
      )

      Json.fromJson[AmendmentDetails](Json.toJson(model)).get mustBe model
    }

    "must round-trip JSON when acceptedTime is missing" in {
      val model = AmendmentDetails(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        contractorName = "Test Contractor",
        originalReturnType = MonthlyNilReturn,
        acceptedTime = None
      )

      Json.fromJson[AmendmentDetails](Json.toJson(model)).get mustBe model
    }
  }
}
