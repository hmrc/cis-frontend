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

import config.FrontendAppConfig
import forms.mappings.Mappings
import forms.mappings.TaxPeriodEndDateRules
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.i18n.Messages

import java.time.format.DateTimeFormatter
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

class DateConfirmPaymentsFormProvider @Inject() (appConfig: FrontendAppConfig) extends Mappings {

  private val MinMonth: Int        = 1
  private val MaxMonth: Int        = 12
  private val MinYear: Int         = 1900
  private val FirstDayOfMonth: Int = 1

  def apply()(implicit messages: Messages): Form[LocalDate] = {
    val earliestTaxPeriodEndDate: LocalDate = LocalDate.parse(appConfig.earliestTaxPeriodEndDate)
    val formattedDateInSeq                  =
      earliestTaxPeriodEndDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK)).split(" ").toSeq

    val today          = LocalDate.now()
    val maxAllowedDate =
      if (today.getDayOfMonth <= TaxPeriodEndDateRules.TaxPeriodEndDay) today.plusMonths(3) else today.plusMonths(4)

    val earliestDateConstraint: Constraint[LocalDate] = Constraint { date =>
      if (
        TaxPeriodEndDateRules.isOnOrAfterEarliest(
          earliestTaxPeriodEndDate,
          date.getYear,
          date.getMonthValue
        )
      ) {
        Valid
      } else {
        Invalid("dateConfirmPayments.taxMonth.error.earliestTaxPeriodEndDate", formattedDateInSeq: _*)
      }
    }

    val maxFutureDateConstraint: Constraint[LocalDate] = Constraint { date =>
      if (
        TaxPeriodEndDateRules.isWithinMaxFuturePeriod(
          maxAllowedDate,
          date.getYear,
          date.getMonthValue
        )
      ) {
        Valid
      } else {
        Invalid("dateConfirmPayments.taxMonth.error.maxAllowedFutureReturnPeriod")
      }
    }

    Form(
      mapping(
        "taxMonth" -> int(
          requiredKey = "dateConfirmPayments.taxMonth.error.required",
          wholeNumberKey = "dateConfirmPayments.taxMonth.error.wholeNumber",
          nonNumericKey = "dateConfirmPayments.taxMonth.error.nonNumeric"
        ).verifying("dateConfirmPayments.taxMonth.error.range", month => month >= MinMonth && month <= MaxMonth),
        "taxYear"  -> int(
          requiredKey = "dateConfirmPayments.taxYear.error.required",
          wholeNumberKey = "dateConfirmPayments.taxYear.error.wholeNumber",
          nonNumericKey = "dateConfirmPayments.taxYear.error.nonNumeric"
        ).verifying("dateConfirmPayments.taxYear.error.range", year => year >= MinYear)
      )((month, year) => LocalDate.of(year, month, FirstDayOfMonth))(date => Some((date.getMonthValue, date.getYear)))
        .verifying(earliestDateConstraint)
        .verifying(maxFutureDateConstraint)
    )
  }
}
