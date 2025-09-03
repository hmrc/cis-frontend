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

package forms.monthlyreturns

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form

class ConfirmEmailAddressFormProvider @Inject() extends Mappings {

  private val EmailRegex = "^[A-Za-z0-9!#$%&*+-/=?^_`{|}~.]+@[A-Za-z0-9!#$%&*+-/=?^_`{|}~.]+$"
  private val MaxEmailLength = 254

  def apply(): Form[String] =
    Form(
      "value" -> text("monthlyreturns.confirmEmailAddress.error.required")
        .verifying(maxLength(MaxEmailLength, "monthlyreturns.confirmEmailAddress.error.length"))
        .verifying(regexp(EmailRegex, "monthlyreturns.confirmEmailAddress.error.invalid"))
    )
}
