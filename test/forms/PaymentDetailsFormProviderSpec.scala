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

    "must bind when the value is exactly 13 characters" in {
      val boundForm = form.bind(Map(fieldName -> "99999999.00"))
      boundForm.errors mustBe empty
      boundForm.get mustBe BigDecimal("99999999.00")
    }

    "must not bind when the value does not match regex pattern" in {
      Seq(
        ("abc", "non-numeric characters"),
        ("12.345", "more than 2 decimal places"),
        ("£100", "currency symbol"),
        ("-100", "negative sign"),
        ("100.001", "more than 2 decimal places"),
        ("100.123", "more than 2 decimal places"),
        ("100.5.5", "multiple decimal points"),
        ("100..0", "multiple decimal points"),
        ("100.00.00", "multiple decimal points")
      ).foreach { case (invalidValue, description) =>
        withClue(s"Value '$invalidValue' ($description) should be invalid") {
          val result = form.bind(Map(fieldName -> invalidValue)).apply(fieldName)
          result.errors must contain(FormError(fieldName, "paymentDetails.error.invalid"))
        }
      }
    }

    "must bind valid values matching regex pattern" in {
      val validValues = Seq(
        "100",
        "100.00",
        "100.0",
        "100.",
        "1000,000",
        "1,000,000",
        "99999999.00",
        "0",
        "0.00",
        "0.0",
        "0.",
        "100.5",
        "100.50"
      )
      validValues.foreach { validValue =>
        val result = form.bind(Map(fieldName -> validValue)).apply(fieldName)
        result.errors mustBe empty
      }
    }

    "must bind values with commas in various positions" in {
      val validCommaValues = Seq("1,000", "10,000", "100,000", "1,000,000", "99,999,999")
      validCommaValues.foreach { validValue =>
        val result = form.bind(Map(fieldName -> validValue)).apply(fieldName)
        result.errors mustBe empty
      }
    }

    "must bind exactly at maximum value boundary" in {
      val boundForm = form.bind(Map(fieldName -> "99999999.00"))
      boundForm.errors mustBe empty
      boundForm.get mustBe BigDecimal("99999999.00")
    }

    "must not bind when value exceeds maximum value" in {
      val boundForm = form.bind(Map(fieldName -> "99999999.01"))
      boundForm.errors must contain(FormError(fieldName, "paymentDetails.error.maxValue"))
    }

    "must correctly parse values with commas" in {
      val boundForm = form.bind(Map(fieldName -> "1,234,567.89"))
      boundForm.errors mustBe empty
      boundForm.get mustBe BigDecimal("1234567.89")
    }

    "must correctly unbind values" in {
      val value  = BigDecimal("12345.67")
      val result = form.fill(value)
      result.data.get(fieldName) mustBe Some("12345.67")
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
