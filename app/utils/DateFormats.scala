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

package utils

import forms.mappings.DateFormat

object DateFormats {

  private val MonthRegex = "^(0?[1-9]|1[0-2])$"
  private val YearRegex  = "^\\d{4}$"

  val monthYearFormats: Seq[DateFormat] = Seq(
    DateFormat(
      dateType = "month",
      errorKey = "monthlyreturns.dateConfirmNilPayments.error.invalid.month",
      regex = MonthRegex
    ),
    DateFormat(
      dateType = "year",
      errorKey = "monthlyreturns.dateConfirmNilPayments.error.invalid.year",
      regex = YearRegex
    )
  )

}
