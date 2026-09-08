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
import models.validation.SubcontractorValidationField.{AddressLine1, EmailAddress}
import play.api.libs.json.{JsString, Json}

class FieldValidationFailureSpec extends SpecBase {

  "FieldValidationFailure JSON format" - {

    "round-trip a failure containing the current value" in {
      val failure =
        FieldValidationFailure(
          field = EmailAddress,
          value = Some("invalid-email")
        )

      Json
        .fromJson[FieldValidationFailure](Json.toJson(failure))
        .get mustBe failure
    }

    "round-trip a failure containing no value" in {
      val failure =
        FieldValidationFailure(
          field = AddressLine1,
          value = None
        )

      Json
        .fromJson[FieldValidationFailure](Json.toJson(failure))
        .get mustBe failure
    }

    "write the expected field and value properties" in {
      val json =
        Json.toJson(
          FieldValidationFailure(
            field = EmailAddress,
            value = Some("invalid-email")
          )
        )

      (json \ "field").get mustBe JsString("emailAddress")
      (json \ "value").get mustBe JsString("invalid-email")
    }

  }
}
