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

package forms.mappings

import config.FrontendAppConfig

import java.time.*
import generators.Generators
import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.FormError
import play.api.i18n.Messages
import play.api.test.Helpers.stubMessages
import utils.DateFormats

class MonthYearDateFormatterSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with Mappings
    with MockitoSugar {

  private implicit val messages: Messages = stubMessages()

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.earliestTaxPeriodEndDate) thenReturn "2007-05-05"
  when(mockAppConfig.monthlyReturnsTaxStartDay) thenReturn 20
  when(mockAppConfig.monthlyReturnsTaxStartMonth) thenReturn 11
  when(mockAppConfig.monthlyReturnsSupportedYears) thenReturn 10

  val invalidKey     = "invalidKey"
  val twoRequiredKey = "twoRequiredKey"
  val requiredKey    = "requiredKey"
  val dateFormats    = DateFormats.monthYearFormats
  val fieldKeys      = List("month", "year")

  private val zone = ZoneId.of("Europe/London")
  private val clock: Clock = 
    Clock.fixed(
      LocalDate.of(2026, 7, 15)
        .atTime(12, 0)
        .atZone(zone)
        .toInstant,
      zone
    )
  private val today        = LocalDate.now(clock)

  val monthYearDateFormatter = new MonthYearDateFormatter(
    invalidKey = invalidKey,
    twoRequiredKey = twoRequiredKey,
    requiredKey = requiredKey,
    dateFormats = dateFormats,
    fieldKeys = fieldKeys,
    config = mockAppConfig,
    clock = clock
  )

  private val earliestSupportedYear = {
    val taxYearStartDate = LocalDate.of(
      today.getYear,
      11,
      20
    )

    val startingTaxYear =
      if today.isBefore(taxYearStartDate) then today.getYear
      else today.getYear + 1

    startingTaxYear - 10 + 1
  }

  val validDates = datesBetween(
    min = LocalDate.of(earliestSupportedYear, 5, 5),
    max = today.withDayOfMonth(5)
  )

  private def clockAt(date: LocalDate): Clock =
    Clock.fixed(
      date.atTime(12, 0).atZone(zone).toInstant,
      zone
    )

  private def formatterAt(date: LocalDate): MonthYearDateFormatter =
    new MonthYearDateFormatter(
      invalidKey = invalidKey,
      twoRequiredKey = twoRequiredKey,
      requiredKey = requiredKey,
      dateFormats = dateFormats,
      fieldKeys = fieldKeys,
      config = mockAppConfig,
      clock = clockAt(date)
    )

  "must bind valid dates with valid month and year" in {

    forAll(validDates -> "valid date") { date =>

      val month: String      = date.getMonthValue.toString
      val year: String       = date.getYear.toString
      val parseMonth: String = if (month.length == 1) s"0$month" else month

      val data = Map(
        "value.month" -> month,
        "value.year"  -> year
      )

      monthYearDateFormatter.bind("value", data) mustEqual Right(
        LocalDate.parse(s"$year-$parseMonth-05")
      )
    }
  }

  "must bind dates with valid future month and year within valid range" in {

    val date =
      if today.getDayOfMonth <= 5 then today.plusMonths(3)
      else today.plusMonths(4)

    val month: String      = date.getMonthValue.toString
    val year: String       = date.getYear.toString
    val parseMonth: String = if (month.length == 1) s"0$month" else month

    val data = Map(
      "value.month" -> month,
      "value.year"  -> year
    )

    monthYearDateFormatter.bind("value", data) mustEqual Right(
      LocalDate.parse(s"$year-$parseMonth-05")
    )
  }

  "must bind a date in the earliest supported year" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "12",
        "value.year" -> earliestSupportedYear.toString
      )
    )

    result mustEqual Right(
      LocalDate.of(earliestSupportedYear, 12, 5)
    )
  }

  "must change the earliest supported year on the configured tax start date" in {

    val formatterBeforeStart =
      formatterAt(LocalDate.of(2026, 11, 19))

    val formatterOnStart =
      formatterAt(LocalDate.of(2026, 11, 20))

    val data = Map(
      "value.month" -> "12",
      "value.year" -> "2017"
    )

    formatterBeforeStart.bind("value", data) mustEqual Right(
      LocalDate.of(2017, 12, 5)
    )

    formatterOnStart.bind("value", data) mustEqual Left(
      List(
        FormError(
          "value.year",
          List("monthlyreturns.dateConfirmNilPayments.error.invalid.earliestSupportedYear"),
          List("2018")
        )
      )
    )
  }

  "must fail to bind an empty date" in {

    val result =
      monthYearDateFormatter.bind("value", Map.empty[String, String])

    result mustEqual Left(
      List(
        FormError("value.month", List("monthlyreturns.dateConfirmNilPayments.error.required.month"), List()),
        FormError("value.year", List("monthlyreturns.dateConfirmNilPayments.error.required.year"), List())
      )
    )
  }

  "must fail to bind with an empty month" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "",
        "value.year"  -> "2012"
      )
    )

    result mustEqual Left(
      List(
        FormError("value.month", List("monthlyreturns.dateConfirmNilPayments.error.required.month"), List())
      )
    )
  }

  "must fail to bind with an empty year" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "6",
        "value.year"  -> ""
      )
    )

    result mustEqual Left(
      List(
        FormError("value.year", List("monthlyreturns.dateConfirmNilPayments.error.required.year"), List())
      )
    )
  }

  "must fail to bind an invalid date" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "13",
        "value.year"  -> "2&12"
      )
    )

    result mustEqual Left(
      List(
        FormError("value.month", List("monthlyreturns.dateConfirmNilPayments.error.invalid.month"), List()),
        FormError("value.year", List("monthlyreturns.dateConfirmNilPayments.error.invalid.year"), List())
      )
    )
  }

  "must fail to bind an invalid month" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "13",
        "value.year"  -> "2012"
      )
    )

    result mustEqual Left(
      List(
        FormError("value.month", List("monthlyreturns.dateConfirmNilPayments.error.invalid.month"), List())
      )
    )
  }

  "must fail to bind an invalid year" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "12",
        "value.year"  -> "2$12"
      )
    )

    result mustEqual Left(
      List(
        FormError("value.year", List("monthlyreturns.dateConfirmNilPayments.error.invalid.year"), List())
      )
    )
  }

  "must fail to bind dates with valid old month and year outside valid range before earliest tax period end date" in {

    val config = mock[FrontendAppConfig]
    when(config.earliestTaxPeriodEndDate) thenReturn "2007-05-05"
    when(config.monthlyReturnsTaxStartDay) thenReturn 20
    when(config.monthlyReturnsTaxStartMonth) thenReturn 11
    when(config.monthlyReturnsSupportedYears) thenReturn 100

    val formatter = new MonthYearDateFormatter(
      invalidKey = invalidKey,
      twoRequiredKey = twoRequiredKey,
      requiredKey = requiredKey,
      dateFormats = dateFormats,
      fieldKeys = fieldKeys,
      config = config,
      clock = clock
    )

    val result = formatter.bind(
      "value",
      Map(
        "value.month" -> "12",
        "value.year"  -> "2000"
      )
    )

    result mustEqual Left(
      List(
        FormError(
          "value.month",
          List("monthlyreturns.dateConfirmNilPayments.error.invalid.earliestTaxPeriodEndDate"),
          List("5", "May", "2007")
        )
      )
    )
  }

  "must fail to bind dates with valid future month and year outside valid range" in {

    val futureDate = today.plusMonths(5)

    val data = Map(
      "value.month" -> futureDate.getMonthValue.toString,
      "value.year"  -> futureDate.getYear.toString
    )

    val result = monthYearDateFormatter.bind("value", data)

    result mustEqual Left(
      List(
        FormError(
          "value.month",
          List("monthlyreturns.dateConfirmNilPayments.error.invalid.maxAllowedFutureReturnPeriod"),
          List()
        )
      )
    )
  }

  "must fail to bind a year beyond the maximum future return period" in {

    val futureDate = today.plusYears(10)

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> futureDate.getMonthValue.toString,
        "value.year"  -> futureDate.getYear.toString
      )
    )

    result mustEqual Left(
      List(
        FormError(
          "value.month",
          List("monthlyreturns.dateConfirmNilPayments.error.invalid.maxAllowedFutureReturnPeriod"),
          List()
        )
      )
    )
  }

  "must fail to bind dates before the earliest supported year" in {

    val result = monthYearDateFormatter.bind(
      "value",
      Map(
        "value.month" -> "12",
        "value.year"  -> (earliestSupportedYear - 1).toString
      )
    )

    result mustEqual Left(
      List(
        FormError(
          "value.year",
          List("monthlyreturns.dateConfirmNilPayments.error.invalid.earliestSupportedYear"),
          List(earliestSupportedYear.toString)
        )
      )
    )
  }
}
