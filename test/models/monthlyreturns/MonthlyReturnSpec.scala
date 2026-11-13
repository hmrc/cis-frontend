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

package models.monthlyreturns

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*

import java.time.LocalDateTime

class MonthlyReturnSpec extends AnyWordSpec with Matchers {

  private val full = MonthlyReturn(
    1L,
    2025,
    4,
    Some("Y"),
    Some("Y"),
    Some("Y"),
    Some("Y"),
    Some("Y"),
    Some("N"),
    Some("SUBMITTED"),
    Some(LocalDateTime.of(2025, 5, 6, 7, 8, 9)),
    Some("Y"),
    Some(2L)
  )

  "MonthlyReturn JSON format" should {
    "round-trip a fully populated return" in {
      Json.toJson(full).validate[MonthlyReturn] mustBe JsSuccess(full)
    }

    "default optionals to None" in {
      Json.obj("monthlyReturnId" -> 1, "taxYear" -> 2025, "taxMonth" -> 4).as[MonthlyReturn] mustBe
        MonthlyReturn(1L, 2025, 4)
    }

    "fail when a required field is missing" in {
      Json.obj("taxYear" -> 2025).validate[MonthlyReturn].isError mustBe true
    }
  }

  "UserMonthlyReturns" should {
    "have an empty value with no returns" in {
      UserMonthlyReturns.empty.monthlyReturnList mustBe empty
    }

    "round-trip a list of returns" in {
      val model = UserMonthlyReturns(Seq(full, MonthlyReturn(2L, 2025, 5)))
      Json.toJson(model).validate[UserMonthlyReturns] mustBe JsSuccess(model)
    }

    "round-trip the empty value" in {
      Json.toJson(UserMonthlyReturns.empty).as[UserMonthlyReturns] mustBe UserMonthlyReturns.empty
    }

    "fail when the list is missing" in {
      Json.obj().validate[UserMonthlyReturns].isError mustBe true
    }
  }
}
