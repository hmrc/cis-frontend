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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.i18n.Lang
import utils.DateTimeFormats.dateTimeFormat

import java.time.LocalDate
import java.util.Locale

class DateTimeFormatsSpec extends AnyFreeSpec with Matchers {

  ".dateTimeFormat" - {

    "must format dates in English" in {
      val formatter = dateTimeFormat()(Lang("en"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "January 2023"
    }

    "must format dates in Welsh" in {
      val formatter = dateTimeFormat()(Lang("cy"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "Ionawr 2023"
    }

    "must default to English format" in {
      val formatter = dateTimeFormat()(Lang("de"))
      val result    = LocalDate.of(2023, 1, 1).format(formatter)
      result mustEqual "January 2023"
    }
  }

  "monthFormatter" - {

    "format month correctly in UK English locale" in {
      val formatter = DateTimeFormats.monthFormatter(Locale.UK)
      val date      = LocalDate.of(2026, 3, 15) // March

      val formatted = date.format(formatter)
      formatted shouldBe "March"
    }

    "format month correctly in Welsh locale" in {
      val welshLocale = new Locale("cy", "GB")
      val formatter   = DateTimeFormats.monthFormatter(welshLocale)
      val date        = LocalDate.of(2026, 3, 15)

      val formatted = date.format(formatter)
      formatted shouldBe "Mawrth"
    }
  }
}
