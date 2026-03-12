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

package models.submission

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers

import java.time.YearMonth

class ChrisSubmissionCommonSpec extends AnyWordSpec with Matchers {

  "ChrisSubmissionCommon" should {

    "create an instance with provided values" in {
      val model = ChrisSubmissionCommon(
        utr = "1234567890",
        aoReference = "754PT00002240",
        monthYear = YearMonth.of(2025, 9),
        email = Some("test@test.com"),
        isAgent = true,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456"
      )

      model.utr mustBe "1234567890"
      model.aoReference mustBe "754PT00002240"
      model.monthYear mustBe YearMonth.of(2025, 9)
      model.email mustBe Some("test@test.com")
      model.isAgent mustBe true
      model.clientTaxOfficeNumber mustBe "123"
      model.clientTaxOfficeRef mustBe "AB456"
    }

    "support case class equality" in {
      val a = ChrisSubmissionCommon(
        "1234567890",
        "754PT00002240",
        YearMonth.of(2025, 9),
        Some("test@test.com"),
        false,
        "",
        ""
      )

      val b = a.copy()

      a mustBe b
    }
  }
}
