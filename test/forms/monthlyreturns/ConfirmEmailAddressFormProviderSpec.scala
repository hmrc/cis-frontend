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

import forms.Validation.emailRegex
import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class ConfirmEmailAddressFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "monthlyreturns.confirmEmailAddress.error.required"
  val lengthKey   = "monthlyreturns.confirmEmailAddress.error.length"
  val invalidKey  = "monthlyreturns.confirmEmailAddress.error.invalid"
  val maxLength   = 254

  val form = new ConfirmEmailAddressFormProvider()()

  ".value" - {

    val fieldName = "value"

    "must bind valid email data" in {
      val validEmails = Seq(
        "test@test.com",
        "user123@example.co.uk",
        "firstname.lastname@test-domain.com",
        "x+tag@mail.org",
        "a@b.cd"
      )

      validEmails.foreach { email =>
        val result = form.bind(Map(fieldName -> email))
        result.errors mustBe empty
      }
    }

    "must not bind emails longer than 254 characters" in {
      val localPartLength = 242
      val longEmail       = ("a" * localPartLength) + "@example.com"

      longEmail.length mustBe 254

      val result = form.bind(Map(fieldName -> longEmail))
      result.errors mustBe empty

      val tooLongEmail = ("a" * (localPartLength + 1)) + "@example.com"

      tooLongEmail.length mustBe 255

      val tooLongResult = form.bind(Map(fieldName -> tooLongEmail))

      tooLongResult.errors must contain(
        FormError(fieldName, lengthKey, Seq(maxLength))
      )
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must reject invalid email formats" in {
      val invalidEmails = Seq(
        "invalid-email",
        "@domain.com",
        "user@",
        "user name@domain.com",
        "user@domain com"
      )

      invalidEmails.foreach { email =>
        val result = form.bind(Map(fieldName -> email))

        result.errors must contain(
          FormError(fieldName, invalidKey, Seq(emailRegex))
        )
      }
    }
  }
}
