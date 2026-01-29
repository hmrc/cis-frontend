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

import base.SpecBase

import java.time.{LocalDate, LocalDateTime}

class SubcontractorServiceSpec extends SpecBase {

  private val service = new SubcontractorService()

  "SubcontractorService.verificationPeriodStart" - {

    "returns 6 April of the tax year two years ago (date on/after 6 April)" in {
      // today 2026-01-29 -> two years ago 2024-01-29 which is before 6 April 2024 => taxYear = 2023
      service.verificationPeriodStart(LocalDate.of(2026, 1, 29)) mustBe LocalDate.of(2023, 4, 6)
    }

    "returns 6 April of the tax year two years ago (date after 6 April)" in {
      // today 2026-12-01 -> two years ago 2024-12-01 which is after 6 April 2024 => taxYear = 2024
      service.verificationPeriodStart(LocalDate.of(2026, 12, 1)) mustBe LocalDate.of(2024, 4, 6)
    }
  }

  "SubcontractorService.verificationRequired" - {

    "returns true when not verified" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("N"),
        verificationDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        verificationPeriodStart = start
      ) mustBe true
    }

    "returns true when verified but verificationDate missing" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = None,
        lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
        verificationPeriodStart = start
      ) mustBe true
    }

    "returns false when verificationDate is within period even if lastMonthlyReturnDate is before period" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2024, 4, 6, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 4, 5, 0, 0)),
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns false when verificationDate is within period and lastMonthlyReturnDate is missing" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2024, 6, 1, 0, 0)),
        lastMonthlyReturnDate = None,
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns false when lastMonthlyReturnDate is within period" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2020, 1, 1, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 5, 1, 0, 0)),
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns false when lastMonthlyReturnDate is exactly on the period start" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2020, 1, 1, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 4, 6, 0, 0)),
        verificationPeriodStart = start
      ) mustBe false
    }

    "returns true when both verificationDate and lastMonthlyReturnDate are before period" in {
      val start = LocalDate.of(2024, 4, 6)

      service.verificationRequired(
        verified = Some("Y"),
        verificationDate = Some(LocalDateTime.of(2024, 4, 5, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2024, 4, 5, 0, 0)),
        verificationPeriodStart = start
      ) mustBe true
    }
  }
}
