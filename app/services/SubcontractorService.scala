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

package services

import services.SubcontractorService.{TAX_YEAR_START_DAY, TAX_YEAR_START_MONTH}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Singleton}

@Singleton
class SubcontractorService @Inject() {
  def verificationPeriodStart(today: LocalDate): LocalDate = {
    val dateTwoYearsAgo       = today.minusYears(2)
    val taxYearCandidate      = dateTwoYearsAgo.getYear
    val taxYearStartCandidate = LocalDate.of(taxYearCandidate, TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY)

    val taxYear =
      if (dateTwoYearsAgo.isBefore(taxYearStartCandidate)) {
        taxYearCandidate - 1
      } else {
        taxYearCandidate
      }

    LocalDate.of(taxYear, TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY)
  }

  def verificationRequired(
    verified: Option[String],
    verificationDate: Option[LocalDateTime],
    lastMonthlyReturnDate: Option[LocalDateTime],
    verificationPeriodStart: LocalDate
  ): Boolean = {
    val isVerified = verified.contains("Y")

    if (!isVerified || verificationDate.isEmpty) {
      true
    } else {
      val verificationDateOk = verificationDate
        .map(_.toLocalDate)
        .exists(date => !date.isBefore(verificationPeriodStart))

      val lastMonthlyReturnOk = lastMonthlyReturnDate
        .map(_.toLocalDate)
        .exists(date => !date.isBefore(verificationPeriodStart))

      !(verificationDateOk || lastMonthlyReturnOk)
    }
  }
}

object SubcontractorService {
  private val TAX_YEAR_START_MONTH = 4
  private val TAX_YEAR_START_DAY   = 6
}
