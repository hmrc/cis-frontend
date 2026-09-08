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
import models.validation.SubcontractorValidationField.{AddressLine1, EmailAddress, Postcode}
import play.api.libs.json.Json

class SubcontractorValidationFailureSpec extends SpecBase {

  "SubcontractorValidationFailure JSON format" - {

    "round-trip every failed field for a subcontractor" in {
      val failure =
        SubcontractorValidationFailure(
          subcontractorId = 101L,
          failedFields = List(
            FieldValidationFailure(
              field = EmailAddress,
              value = Some("invalid-email")
            ),
            FieldValidationFailure(
              field = Postcode,
              value = Some("ABCDEFGHI")
            ),
            FieldValidationFailure(
              field = AddressLine1,
              value = None
            )
          )
        )

      Json
        .fromJson[SubcontractorValidationFailure](Json.toJson(failure))
        .get mustBe failure
    }

    "round-trip an empty failed-fields list" in {
      val failure =
        SubcontractorValidationFailure(
          subcontractorId = 101L,
          failedFields = Nil
        )

      Json
        .fromJson[SubcontractorValidationFailure](Json.toJson(failure))
        .get mustBe failure
    }

    "write the stable subcontractor identifier and failed fields" in {
      val json =
        Json.toJson(
          SubcontractorValidationFailure(
            subcontractorId = 101L,
            failedFields = List(
              FieldValidationFailure(
                field = EmailAddress,
                value = Some("invalid-email")
              )
            )
          )
        )

      (json \ "subcontractorId").as[Long] mustBe 101L
      (json \ "failedFields").as[List[FieldValidationFailure]] mustBe
        List(
          FieldValidationFailure(
            field = EmailAddress,
            value = Some("invalid-email")
          )
        )
    }
  }
}
