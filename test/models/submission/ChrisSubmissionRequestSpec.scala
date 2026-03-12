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

package models.submission

import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.YearMonth

class ChrisSubmissionRequestSpec extends AnyWordSpec with Matchers {

  "ChrisSubmissionRequest" should {

    "JSON round-trip" in {
      val model = ChrisSubmissionRequest(
        utr = "1234567890",
        aoReference = "754PT00002240",
        informationCorrect = "yes",
        inactivity = "no",
        monthYear = "2025-09",
        email = Some("test@test.com"),
        isAgent = false,
        clientTaxOfficeNumber = "",
        clientTaxOfficeRef = "",
        returnType = MonthlyNilReturn,
        standard = None
      )

      Json.toJson(model).validate[ChrisSubmissionRequest] mustBe JsSuccess(model)
    }

    "fromNil builds request with yes/no values, trims email, sets returnType and standard=None" in {
      val common = ChrisSubmissionCommon(
        utr = "1234567890",
        aoReference = "754PT00002240",
        monthYear = YearMonth.of(2025, 9),
        email = Some("  test@test.com  "),
        isAgent = false,
        clientTaxOfficeNumber = "",
        clientTaxOfficeRef = ""
      )

      val req = ChrisSubmissionRequest.fromNil(
        common = common,
        informationCorrect = true,
        inactivity = false
      )

      req.utr mustBe "1234567890"
      req.aoReference mustBe "754PT00002240"
      req.monthYear mustBe "2025-09"
      req.email mustBe Some("test@test.com")
      req.informationCorrect mustBe "yes"
      req.inactivity mustBe "no"
      req.returnType mustBe MonthlyNilReturn
      req.standard mustBe None
    }

    "fromStandard builds request with returnType=MonthlyStandardReturn and standard=Some(...)" in {
      val common = ChrisSubmissionCommon(
        utr = "1234567890",
        aoReference = "754PT00002240",
        monthYear = YearMonth.of(2025, 9),
        email = Some(" test@test.com "),
        isAgent = true,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456"
      )

      val standard = ChrisStandardMonthlyReturn(
        subcontractors = Seq.empty,
        declarations = ChrisStandardDeclarations("yes", "yes")
      )

      val req = ChrisSubmissionRequest.fromStandard(
        common = common,
        informationCorrect = true,
        inactivity = true,
        standard = standard
      )

      req.returnType mustBe MonthlyStandardReturn
      req.standard mustBe Some(standard)
      req.email mustBe Some("test@test.com")
      req.informationCorrect mustBe "yes"
      req.inactivity mustBe "yes"
    }
  }
}
