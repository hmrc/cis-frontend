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

package pages.validation

import base.SpecBase
import models.UserAnswers
import models.validation.SubcontractorValidationField.{AddressLine1, EmailAddress}
import models.validation.{FieldValidationFailure, SubcontractorValidationFailure}
import play.api.libs.json.JsPath

class SubcontractorValidationFailuresPageSpec extends SpecBase {

  "SubcontractorValidationFailuresPage" - {

    "have the correct path" in {
      SubcontractorValidationFailuresPage.path mustBe
        JsPath \ "validation" \ "subcontractorValidationFailures"
    }

    "have the correct toString value" in {
      SubcontractorValidationFailuresPage.toString mustBe
        "subcontractorValidationFailures"
    }

    "store and retrieve every subcontractor validation failure" in {
      val failures =
        List(
          SubcontractorValidationFailure(
            subcontractorId = 101L,
            failedFields = List(
              FieldValidationFailure(
                field = EmailAddress,
                value = Some("invalid-email")
              ),
              FieldValidationFailure(
                field = AddressLine1,
                value = None
              )
            )
          )
        )

      val userAnswers =
        UserAnswers("test-user")
          .set(
            SubcontractorValidationFailuresPage,
            failures
          )
          .get

      userAnswers
        .get(SubcontractorValidationFailuresPage) mustBe
        Some(failures)
    }

    "store an empty list so previous failures can be cleared" in {
      val userAnswers =
        UserAnswers("test-user")
          .set(
            SubcontractorValidationFailuresPage,
            List.empty[
              SubcontractorValidationFailure
            ]
          )
          .get

      userAnswers
        .get(SubcontractorValidationFailuresPage) mustBe
        Some(Nil)
    }
  }
}
