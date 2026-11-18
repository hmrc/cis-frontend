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
import forms.behaviours.DateBehaviours
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages

import java.time.*
import java.time.format

class DateConfirmPaymentsFormProviderSpec extends DateBehaviours with SpecBase with MockitoSugar {

  private implicit val messages: Messages      = stubMessages()
  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockFrontendAppConfig.earliestTaxPeriodEndDate) `thenReturn` "2007-05-05"
  when(mockFrontendAppConfig.monthlyReturnsTaxStartDay) `thenReturn` 20
  when(mockFrontendAppConfig.monthlyReturnsTaxStartMonth) `thenReturn` 11
  when(mockFrontendAppConfig.monthlyReturnsSupportedYears) `thenReturn` 10

  private val clock: Clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneId.of("Europe/London"))
  private val form         = new DateConfirmPaymentsFormProvider(mockFrontendAppConfig, clock)()

  ".value" - {

    val currentDate = LocalDate.now()

    val taxYearStartDate =
      LocalDate.of(
        currentDate.getYear,
        11,
        20
      )

    val startingTaxYear =
      if currentDate.isBefore(taxYearStartDate) then currentDate.getYear
      else currentDate.getYear + 1

    val earliestSupportedYear =
      startingTaxYear - 10 + 1

    val validData = oneOf(
      Seq(
        LocalDate.of(earliestSupportedYear, 4, 5),
        LocalDate.of(earliestSupportedYear + 1, 11, 5),
        currentDate.withDayOfMonth(5)
      )
    )

    "bind valid data using a month name" in {
      forAll(validData -> "valid date") { (date: LocalDate) =>
        val data = Map(
          "value.month" -> date.getMonth.getDisplayName(format.TextStyle.FULL, java.util.Locale.UK),
          "value.year"  -> date.getYear.toString
        )
        form.bind(data).value.value mustEqual date.withDayOfMonth(5)
      }
    }

    "bind valid data using a short month name" in {
      form.bind(Map("value.month" -> "Nov", "value.year" -> "2025")).value.value mustEqual LocalDate.of(2025, 11, 5)
    }

    "remove all spaces from the month before validating" in {
      form.bind(Map("value.month" -> " N o v    ", "value.year" -> "2025")).value.value mustEqual LocalDate.of(
        2025,
        11,
        5
      )
    }

    "not bind a numeric month" in {
      val result = form.bind(Map("value.month" -> "11", "value.year" -> "2025"))
      result.errors.map(_.message) mustEqual Seq("monthlyreturns.dateConfirmNilPayments.error.invalid.month")
    }

    behave like mandatoryMonthYearDateField(
      form,
      Seq("value.month", "value.year"),
      Seq(
        "monthlyreturns.dateConfirmNilPayments.error.required.month",
        "monthlyreturns.dateConfirmNilPayments.error.required.year"
      )
    )
  }
}
