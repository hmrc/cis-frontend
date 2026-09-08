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
import play.api.libs.json.{JsError, Json}
import models.finalvalidation.MonthlyFinalValidationSource.*

class MonthlyFinalValidationSourceSpec extends SpecBase {

  "MonthlyFinalValidationSource" - {

    "serialise and deserialise SelectSubcontractors" in {
      val json = Json.obj(
        "source" -> "selectSubcontractors"
      )

      Json.toJson[MonthlyFinalValidationSource](SelectSubcontractors) mustBe json
      json.as[MonthlyFinalValidationSource] mustBe SelectSubcontractors
    }

    "serialise and deserialise WhichSubcontractorsToAdd" in {
      val source = WhichSubcontractorsToAdd("add")

      val json = Json.obj(
        "source" -> "whichSubcontractorsToAdd",
        "mode"   -> "add"
      )

      Json.toJson[MonthlyFinalValidationSource](source) mustBe json
      json.as[MonthlyFinalValidationSource] mustBe source
    }

    "reject an unknown source" in {
      Json
        .fromJson[MonthlyFinalValidationSource](
          Json.obj("source" -> "unknown")
        ) mustBe a[JsError]
    }
  }
}
