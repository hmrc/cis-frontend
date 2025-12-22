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

import base.SpecBase
import generators.Generators
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class DateConfirmPaymentsFormProviderSpec extends SpecBase with Generators {

  private implicit val messages: Messages = stubMessages()
  private val form                        = new DateConfirmPaymentsFormProvider()()

  ".taxMonth" - {

    "must bind valid month values" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2024"))
      result.errors mustBe empty
      result.value.value mustBe LocalDate.of(2024, 6, 1)
    }

    "must fail when month is missing" in {
      val result = form.bind(Map("taxYear" -> "2024"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.required")
    }

    "must fail when month is not a number" in {
      val result = form.bind(Map("taxMonth" -> "abc", "taxYear" -> "2024"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.nonNumeric")
    }

    "must fail when month is out of range" in {
      val result = form.bind(Map("taxMonth" -> "13", "taxYear" -> "2024"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.range")
    }
  }

  ".taxYear" - {

    "must bind valid year at minimum boundary" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2007"))
      result.errors mustBe empty
      result.value.value mustBe LocalDate.of(2007, 6, 1)
    }

    "must bind valid year at maximum boundary" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "3000"))
      result.errors mustBe empty
      result.value.value mustBe LocalDate.of(3000, 6, 1)
    }

    "must fail when year is missing" in {
      val result = form.bind(Map("taxMonth" -> "6"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxYear.error.required")
    }

    "must fail when year is not a number" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "abc"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxYear.error.nonNumeric")
    }

    "must fail when year is below minimum range" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2006"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxYear.error.range")
    }

    "must fail when year is above maximum range" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "3001"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxYear.error.range")
    }
  }
}
