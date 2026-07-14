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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import java.time.LocalDate

class TaxPeriodEndDateRulesSpec extends AnyFreeSpec with Matchers {

  "isOnOrAfterEarliest" - {

    "must return true when year and month are after earliest tax period end date month and year" in {
      val earliest = LocalDate.of(2007, 5, 5)

      TaxPeriodEndDateRules.isOnOrAfterEarliest(earliest, year = 2008, month = 1) mustBe true
    }

    "must return true when year and month are equal to earliest tax period end date month and year" in {
      val earliest = LocalDate.of(2007, 5, 5)

      TaxPeriodEndDateRules.isOnOrAfterEarliest(earliest, year = 2007, month = 5) mustBe true
    }

    "must return false when year and month are before earliest tax period end date month and year" in {
      val earliest = LocalDate.of(2007, 5, 5)

      TaxPeriodEndDateRules.isOnOrAfterEarliest(earliest, year = 2007, month = 4) mustBe false
    }
  }

  "isWithinMaxFuturePeriod" - {

    "must return true when tax period end date is equal to max allowed date month and year" in {
      val maxAllowed = LocalDate.of(2025, 5, 5)

      TaxPeriodEndDateRules.isWithinMaxFuturePeriod(maxAllowed, year = 2025, month = 5) mustBe true
    }

    "must return true when tax period end date is before max allowed date month and year" in {
      val maxAllowed = LocalDate.of(2025, 7, 5)

      TaxPeriodEndDateRules.isWithinMaxFuturePeriod(maxAllowed, year = 2025, month = 6) mustBe true
    }

    "must return false when tax period end date is after max allowed date month and year" in {
      val maxAllowed = LocalDate.of(2025, 7, 5)

      TaxPeriodEndDateRules.isWithinMaxFuturePeriod(maxAllowed, year = 2025, month = 8) mustBe false
    }
  }
}
