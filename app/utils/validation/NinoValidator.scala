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

package utils.validation

import forms.Validation.ninoRegex
import models.validation.{FieldValidationFailure, SubcontractorValidationField}

object NinoValidator {

  private val length = 9

  def validate(
    value: Option[String]
  ): Option[FieldValidationFailure] =
    value
      .filter(_.trim.nonEmpty)
      .flatMap { nino =>
        Option.when(
          nino.length > length ||
            !nino.matches(ninoRegex)
        ) {
          FieldValidationFailure(
            field = SubcontractorValidationField.Nino,
            value = Some(nino)
          )
        }
      }
}
