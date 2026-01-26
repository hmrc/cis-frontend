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
import config.FrontendAppConfig
import generators.Generators
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.LocalDate

class DateConfirmPaymentsFormProviderSpec extends SpecBase with Generators with MockitoSugar {

  private implicit val messages: Messages      = stubMessages()
  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"

  private val form = new DateConfirmPaymentsFormProvider(mockFrontendAppConfig)()

  ".taxMonth" - {

    "must bind valid month values" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2024"))
      result.errors mustBe empty
      result.value.value mustBe LocalDate.of(2024, 6, 5)
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
      result.value.value mustBe LocalDate.of(2007, 6, 5)
    }

    "must bind valid year at maximum boundary" in {
      val today          = LocalDate.now()
      val maxAllowedDate = if (today.getDayOfMonth <= 5) today.plusMonths(3) else today.plusMonths(4)
      val maxYear        = maxAllowedDate.getYear + 1
      val result         = form.bind(Map("taxMonth" -> "6", "taxYear" -> maxYear.toString))
      result.errors.filter(_.message == "dateConfirmPayments.taxYear.error.range") mustBe empty
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
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
    }

    "must fail when year is above maximum range" in {
      val today          = LocalDate.now()
      val maxAllowedDate = if (today.getDayOfMonth <= 5) today.plusMonths(3) else today.plusMonths(4)
      val maxYear        = maxAllowedDate.getYear + 1
      val result         = form.bind(Map("taxMonth" -> "6", "taxYear" -> (maxYear + 1).toString))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
    }
  }

  ".earliestTaxPeriodEndDate" - {

    "must fail when date is before earliest tax period end date" in {
      val result = form.bind(Map("taxMonth" -> "4", "taxYear" -> "2007"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
      result.errors
        .find(_.message == "dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
        .value
        .args mustEqual Seq("5", "May", "2007")
    }

    "must fail when date is in year before earliest tax period end date" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2006"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
    }

    "must fail when date is January in year before earliest tax period end date" in {
      val result = form.bind(Map("taxMonth" -> "1", "taxYear" -> "2006"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
    }

    "must fail when date is December in year before earliest tax period end date" in {
      val result = form.bind(Map("taxMonth" -> "12", "taxYear" -> "2006"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
    }

    "must pass when date is equal to earliest tax period end date" in {
      val result = form.bind(Map("taxMonth" -> "5", "taxYear" -> "2007"))
      result.errors.filter(_.message == "dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate") mustBe empty
    }

    "must pass when date is after earliest tax period end date" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2007"))
      result.errors.filter(_.message == "dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate") mustBe empty
    }

    "must pass when date is in a later year" in {
      val result = form.bind(Map("taxMonth" -> "1", "taxYear" -> "2008"))
      result.errors.filter(_.message == "dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate") mustBe empty
    }

    "must pass when date is many years later" in {
      val result = form.bind(Map("taxMonth" -> "12", "taxYear" -> "2024"))
      result.errors.filter(_.message == "dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate") mustBe empty
    }
  }

  ".maxAllowedFutureReturnPeriod" - {

    "must fail when date is more than 3 return periods ahead (when current day is 1-5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth <= 5) {
        val futureDate = today.plusMonths(4)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
      }
    }

    "must fail when date is more than 4 return periods ahead (when current day is > 5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth > 5) {
        val futureDate = today.plusMonths(5)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
      }
    }

    "must fail when date is exactly 4 months ahead (when current day is 1-5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth <= 5) {
        val futureDate = today.plusMonths(4)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
      }
    }

    "must fail when date is exactly 5 months ahead (when current day is > 5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth > 5) {
        val futureDate = today.plusMonths(5)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
      }
    }

    "must pass when date is exactly 3 months ahead (when current day is 1-5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth <= 5) {
        val futureDate = today.plusMonths(3)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.filter(
          _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
        ) mustBe empty
      }
    }

    "must pass when date is exactly 4 months ahead (when current day is > 5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth > 5) {
        val futureDate = today.plusMonths(4)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.filter(
          _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
        ) mustBe empty
      }
    }

    "must pass when date is within allowed future return periods (3 months when day is 1-5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth <= 5) {
        val futureDate = today.plusMonths(3)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.filter(
          _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
        ) mustBe empty
      }
    }

    "must pass when date is within allowed future return periods (4 months when day is > 5)" in {
      val today = LocalDate.now()
      if (today.getDayOfMonth > 5) {
        val futureDate = today.plusMonths(4)
        val result     =
          form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
        result.errors.filter(
          _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
        ) mustBe empty
      }
    }

    "must pass when date is in the past" in {
      val pastDate = LocalDate.now().minusMonths(6)
      val result   =
        form.bind(Map("taxMonth" -> pastDate.getMonthValue.toString, "taxYear" -> pastDate.getYear.toString))
      result.errors.filter(
        _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
      ) mustBe empty
    }

    "must pass when date is current month" in {
      val today  = LocalDate.now()
      val result =
        form.bind(Map("taxMonth" -> today.getMonthValue.toString, "taxYear" -> today.getYear.toString))
      result.errors.filter(
        _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
      ) mustBe empty
    }

    "must pass when date is 1 month ahead" in {
      val futureDate = LocalDate.now().plusMonths(1)
      val result     =
        form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
      result.errors.filter(
        _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
      ) mustBe empty
    }

    "must pass when date is 2 months ahead" in {
      val futureDate = LocalDate.now().plusMonths(2)
      val result     =
        form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
      result.errors.filter(
        _.message == "dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod"
      ) mustBe empty
    }
  }

  ".combinedValidations" - {

    "must pass when all validations pass" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2024"))
      result.errors mustBe empty
      result.value.value mustBe LocalDate.of(2024, 6, 5)
    }

    "must fail with earliest date error when date is before earliest and within year range" in {
      val result = form.bind(Map("taxMonth" -> "4", "taxYear" -> "2007"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
      result.errors.map(_.message) must not contain "dateConfirmPayments.taxYear.error.range"
    }

    "must fail with earliest date error when year is before earliest date year" in {
      val result = form.bind(Map("taxMonth" -> "6", "taxYear" -> "2006"))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate")
      result.errors.map(_.message) must not contain "dateConfirmPayments.taxYear.error.range"
    }

    "must fail with max future date error when date is too far in future" in {
      val today      = LocalDate.now()
      val futureDate = if (today.getDayOfMonth <= 5) today.plusMonths(4) else today.plusMonths(5)
      val result     =
        form.bind(Map("taxMonth" -> futureDate.getMonthValue.toString, "taxYear" -> futureDate.getYear.toString))
      result.errors.map(_.message) must contain("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
    }
  }
}
