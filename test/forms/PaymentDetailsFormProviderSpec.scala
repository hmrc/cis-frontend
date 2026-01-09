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

package forms

import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError

import scala.math.BigDecimal.RoundingMode

class PaymentDetailsFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new PaymentDetailsFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = BigDecimal("0")
    val maximum = BigDecimal("99999999.00")

    val validDataGenerator =
      Gen
        .choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    "must not bind when the value exceeds maxLength of 13" in {
      val result = form.bind(Map(fieldName -> "12345678901234")).apply(fieldName)
      result.errors mustEqual Seq(FormError(fieldName, "paymentDetails.error.maxLength"))
    }

    "must not bind when the value does not match regex pattern" in {
      val invalidValues = Seq("abc", "12.345", "£100", "-100", "100.001", "100.123")
      invalidValues.foreach { invalidValue =>
        val result = form.bind(Map(fieldName -> invalidValue)).apply(fieldName)
        result.errors mustEqual Seq(FormError(fieldName, "paymentDetails.error.invalid"))
      }
    }

    "must bind valid values matching regex pattern" in {
      val validValues = Seq("100", "100.00", "100.0", "1000,000", "99999999.00", "0", "0.00")
      validValues.foreach { validValue =>
        val result = form.bind(Map(fieldName -> validValue)).apply(fieldName)
        result.errors mustBe empty
      }
    }

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "paymentDetails.error.maxValue")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "paymentDetails.error.required")
    )
  }
}
