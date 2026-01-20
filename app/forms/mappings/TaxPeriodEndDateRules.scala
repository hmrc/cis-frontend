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

import java.time.{LocalDate, YearMonth}
import scala.math.Ordering.Implicits.infixOrderingOps

object TaxPeriodEndDateRules {

  val TaxPeriodEndDay: Int = 5

  def isOnOrAfterEarliest(earliestTaxPeriodEndDate: LocalDate, year: Int, month: Int): Boolean =
    (year, month) >= (earliestTaxPeriodEndDate.getYear, earliestTaxPeriodEndDate.getMonthValue)

  def isWithinMaxFuturePeriod(maxAllowedDate: LocalDate, year: Int, month: Int): Boolean = {
    val taxPeriodEndDate = LocalDate.of(year, month, TaxPeriodEndDay)
    !YearMonth.from(taxPeriodEndDate).isAfter(YearMonth.from(maxAllowedDate))
  }
}
