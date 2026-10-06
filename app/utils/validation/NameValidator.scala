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

import models.validation.{FieldValidationFailure, SubcontractorValidationField}

object NameValidator {

  def validate(
    firstName: Option[String],
    secondName: Option[String],
    surname: Option[String],
    tradingName: Option[String]
  ): Seq[FieldValidationFailure] = {

    val firstNameBlank = firstName.forall(_.isBlank)

    val secondNameBlank = secondName.forall(_.isBlank)

    val surnameBlank = surname.forall(_.isBlank)

    val tradingNameBlank = tradingName.forall(_.isBlank)

    val failures = Seq.newBuilder[FieldValidationFailure]

    if (tradingNameBlank && firstNameBlank) {
      failures += FieldValidationFailure(
        field = SubcontractorValidationField.FirstName,
        value = firstName
      )
    }

    if (tradingNameBlank && surnameBlank) {
      failures += FieldValidationFailure(
        field = SubcontractorValidationField.TradingName,
        value = tradingName
      )
    }

    if (surnameBlank && (tradingNameBlank || !firstNameBlank)) {
      failures += FieldValidationFailure(
        field = SubcontractorValidationField.Surname,
        value = surname
      )
    }

    if (!secondNameBlank && firstNameBlank) {
      failures += FieldValidationFailure(
        field = SubcontractorValidationField.FirstName,
        value = firstName
      )
    }

    failures.result()
  }
}
