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

package models.monthlyreturns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json

class SubmissionConfirmationCacheSpec extends AnyFreeSpec with Matchers {

  private val model = SubmissionConfirmationCache(
    periodEnd = "March 2018",
    contractorName = "PAL 355 Scheme",
    email = "test@test.com",
    submittedTime = "8:46am",
    submittedDate = "6 January 2017"
  )

  private val json = Json.obj(
    "periodEnd"      -> "March 2018",
    "contractorName" -> "PAL 355 Scheme",
    "email"          -> "test@test.com",
    "submittedTime"  -> "8:46am",
    "submittedDate"  -> "6 January 2017"
  )

  "SubmissionConfirmationCache" - {

    "must serialise to JSON" in {
      Json.toJson(model) mustEqual json
    }

    "must deserialise from JSON" in {
      json.as[SubmissionConfirmationCache] mustEqual model
    }

    "must round-trip through JSON" in {
      Json.toJson(model).as[SubmissionConfirmationCache] mustEqual model
    }
  }
}
