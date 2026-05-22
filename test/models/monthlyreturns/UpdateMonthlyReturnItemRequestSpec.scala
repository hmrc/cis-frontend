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

import play.api.libs.json.Json
import base.SpecBase

class UpdateMonthlyReturnItemRequestSpec extends SpecBase {

  "UpdateMonthlyReturnItemRequest" - {

    "must serialise and deserialise" in {

      val model = UpdateMonthlyReturnItemRequest(
        instanceId = "1",
        taxYear = 2024,
        taxMonth = 3,
        subcontractorId = 123456789L,
        subcontractorName = "Test Subcontractor",
        totalPayments = "1000.00",
        costOfMaterials = "100.00",
        totalDeducted = "200.00",
        amendment = "N"
      )

      val json = Json.obj(
        "instanceId"        -> "1",
        "taxYear"           -> 2024,
        "taxMonth"          -> 3,
        "subcontractorId"   -> 123456789L,
        "subcontractorName" -> "Test Subcontractor",
        "totalPayments"     -> "1000.00",
        "costOfMaterials"   -> "100.00",
        "totalDeducted"     -> "200.00",
        "amendment"         -> "N"
      )

      Json.toJson(model) mustEqual json
      json.as[UpdateMonthlyReturnItemRequest] mustEqual model
    }
  }
}
