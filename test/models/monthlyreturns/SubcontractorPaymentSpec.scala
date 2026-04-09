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
import play.api.libs.json.{JsError, JsSuccess, Json}

class SubcontractorPaymentSpec extends AnyFreeSpec with Matchers {

  "SubcontractorPayment JSON format" - {

    "must write to JSON and read back (round-trip)" in {
      val model = SubcontractorPayment(
        name = "BuildRight Construction",
        paymentsMade = "£165",
        costOfMaterials = "£95",
        taxDeducted = "£95"
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "name"            -> "BuildRight Construction",
        "paymentsMade"    -> "£165",
        "costOfMaterials" -> "£95",
        "taxDeducted"     -> "£95"
      )

      json.as[SubcontractorPayment] mustBe model
    }

    "must read valid JSON" in {
      val json = Json.obj(
        "name"            -> "Northern Trades Ltd",
        "paymentsMade"    -> "£75",
        "costOfMaterials" -> "£55",
        "taxDeducted"     -> "£55"
      )

      json.validate[SubcontractorPayment] mustBe JsSuccess(
        SubcontractorPayment("Northern Trades Ltd", "£75", "£55", "£55")
      )
    }

    "must fail when required fields are missing" in {
      val json = Json.obj(
        "name" -> "TyneWear Ltd"
      )

      json.validate[SubcontractorPayment] mustBe a[JsError]
    }

    "must serialise a sequence of subcontractors" in {
      val list = Seq(
        SubcontractorPayment("A", "£1", "£2", "£3"),
        SubcontractorPayment("B", "£4", "£5", "£6")
      )

      val json = Json.toJson(list)

      json mustBe Json.arr(
        Json.obj("name" -> "A", "paymentsMade" -> "£1", "costOfMaterials" -> "£2", "taxDeducted" -> "£3"),
        Json.obj("name" -> "B", "paymentsMade" -> "£4", "costOfMaterials" -> "£5", "taxDeducted" -> "£6")
      )

      json.as[Seq[SubcontractorPayment]] mustBe list
    }
  }
}
