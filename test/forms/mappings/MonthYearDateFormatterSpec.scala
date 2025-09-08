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

import java.time.LocalDate
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

  val invalidKey     = "invalidKey"
  val twoRequiredKey = "twoRequiredKey"
  val requiredKey    = "requiredKey"
  val dateFormats    = DateFormats.monthYearFormats
  val fieldKeys      = List("month", "year")

  val monthYearDateFormatter = new MonthYearDateFormatter(
    invalidKey = invalidKey,
    twoRequiredKey = twoRequiredKey,
    requiredKey = requiredKey,
    dateFormats = dateFormats,
    fieldKeys = fieldKeys,
    config = mockAppConfig
  )

  val validDates = datesBetween(
    min = LocalDate.of(2010, 5, 5),
    max = LocalDate.of(2020, 5, 5)
  )

  val invalidDatesBefore = datesBetween(
    min = LocalDate.of(2000, 5, 5),
    max = LocalDate.of(2010, 5, 4)
  )

  val invalidDatesAfter = datesBetween(
    min = LocalDate.of(2050, 5, 5),
    max = LocalDate.of(2060, 5, 5)
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

      monthYearDateFormatter.bind("value", data) mustEqual Right(LocalDate.parse(s"$year-$parseMonth-05"))
    }
  }

  "must bind dates with valid future month and year within valid range" in {

    val today = LocalDate.now()

    val date = if today.getDayOfMonth <= 5 then today.plusMonths(3) else today.plusMonths(4)

    val month: String      = date.getMonthValue.toString
    val year: String       = date.getYear.toString
    val parseMonth: String = if (month.length == 1) s"0$month" else month

    val data = Map(
      "value.month" -> month,
      "value.year"  -> year
    )

    monthYearDateFormatter.bind("value", data) mustEqual Right(LocalDate.parse(s"$year-$parseMonth-05"))

  }

  "must fail to bind an empty date" in {

    val result = monthYearDateFormatter.bind("value", Map.empty[String, String])

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

    val result = monthYearDateFormatter.bind(
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

    val today = LocalDate.now().plusMonths(5)

    val data = Map(
      "value.month" -> today.getMonthValue.toString,
      "value.year"  -> today.getYear.toString
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

}
