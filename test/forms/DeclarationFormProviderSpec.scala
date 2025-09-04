/*
 * Copyright 2025 HM Revenue & Customs
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

package forms

import forms.behaviours.EnumFieldBehaviours
import models.Declaration
import play.api.data.FormError

class DeclarationFormProviderSpec extends EnumFieldBehaviours {

  val form = new DeclarationFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "declaration.error.required"

    behave like enumField[Declaration](
      form,
      fieldName,
      validValues = Declaration.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryEnumField(
      form,
      fieldName,
      requiredKey
    )
  }
}
