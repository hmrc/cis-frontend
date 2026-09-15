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

package models.validation

import base.SpecBase
import play.api.libs.json.{JsNumber, JsString, Json}

class SubcontractorValidationFieldSpec extends SpecBase {

  "SubcontractorValidationField JSON format" - {

    "write every supported field using its expected value" in {
      SubcontractorValidationField.values.foreach { field =>
        Json.toJson(field) mustBe JsString(field.value)
      }
    }

    "read every supported field" in {
      SubcontractorValidationField.values.foreach { field =>
        Json
          .fromJson[SubcontractorValidationField](JsString(field.value))
          .get mustBe field
      }
    }

    "reject an unsupported field" in {
      Json
        .fromJson[SubcontractorValidationField](JsString("unsupportedField"))
        .isError mustBe true
    }

    "reject a value that is not a string" in {
      Json
        .fromJson[SubcontractorValidationField](JsNumber(1))
        .isError mustBe true
    }
  }
}
