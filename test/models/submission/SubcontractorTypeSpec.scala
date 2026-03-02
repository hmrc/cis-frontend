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

import models.submission.SubcontractorType.*
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*

class SubcontractorTypeSpec extends AnyFreeSpec with Matchers {

  "SubcontractorType JSON" - {

    "reads valid values (case-insensitive)" in {
      Json.fromJson[SubcontractorType](JsString("SOLETRADER"))  shouldBe JsSuccess(SoleTrader)
      Json.fromJson[SubcontractorType](JsString("company"))     shouldBe JsSuccess(Company)
      Json.fromJson[SubcontractorType](JsString("Partnership")) shouldBe JsSuccess(Partnership)
      Json.fromJson[SubcontractorType](JsString("trust"))       shouldBe JsSuccess(Trust)
    }

    "rejects invalid value" in {
      Json.fromJson[SubcontractorType](JsString("nope")).isError shouldBe true
    }

    "writes to string" in {
      Json.toJson[SubcontractorType](SoleTrader) shouldBe JsString(SoleTrader.toString)
    }
  }
}
