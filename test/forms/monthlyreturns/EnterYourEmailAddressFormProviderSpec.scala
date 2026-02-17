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

package forms.monthlyreturns

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import forms.Validation.emailRegex

class EnterYourEmailAddressFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "monthlyreturns.enterYourEmailAddress.error.required"
  val lengthKey   = "monthlyreturns.enterYourEmailAddress.error.length"
  val maxLength   = 132
  val invalidKey  = "monthlyreturns.enterYourEmailAddress.error.invalid"

  val form = new EnterYourEmailAddressFormProvider()()

  ".value" - {

    val fieldName = "value"

    "must bind valid email data" in {
      val validEmails = Seq(
        "user@domain.com",
        "user.name@domain.com",
        "user+tag@domain.com",
        "user@domain.co.uk",
        "user123@domain123.com",
        "user!#$%&'*+/=?^_`{|}~@domain.com",
        "\"quoted.local\"@example.com"
      )

      validEmails.foreach { validEmail =>
        val result = form.bind(Map(fieldName -> validEmail))
        result.errors must be(empty)
      }
    }

    "must not bind strings longer than 132 characters" in {
      val longEmail = "a" * 130 + "@domain.com"
      val result    = form.bind(Map(fieldName -> longEmail))
      result.errors must contain(FormError(fieldName, lengthKey, Seq(maxLength)))
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

      invalidEmails.foreach { invalidEmail =>
        val result = form.bind(Map("value" -> invalidEmail))
        result.errors must contain(
          FormError("value", invalidKey, Seq(emailRegex))
        )
      }
    }

    "must accept valid email formats" in {
      val validEmails = Seq(
        "user@domain.com",
        "user.name@domain.com",
        "user+tag@domain.com",
        "user@domain.co.uk",
        "user123@domain123.com",
        "user!#$%&*+-/=?^_`{|}~@domain.com",
        "user@domain!#$%&*+-/=?^_`{|}~.com"
      )

      validEmails.foreach { validEmail =>
        val result = form.bind(Map(fieldName -> validEmail))
        result.errors must not contain FormError(fieldName, invalidKey)
      }
    }
  }
}
