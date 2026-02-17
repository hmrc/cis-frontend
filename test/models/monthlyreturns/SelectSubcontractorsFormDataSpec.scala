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
import play.api.libs.json.Json

class SelectSubcontractorsFormDataSpec extends AnyWordSpec with Matchers {

  "SelectSubcontractorsFormData" should {
    "serialise and deserialise correctly via Json" in {
      val model = new SelectSubcontractorsFormData(
        subcontractorsToInclude = List(1, 2, 3, 4)
      )

      val json = Json.toJson(model)
      json.as[SelectSubcontractorsFormData] mustBe model
    }

    "produce the correct Json structure" in {
      val model = new SelectSubcontractorsFormData(
        subcontractorsToInclude = List(1)
      )

      val json = Json.toJson(model)
      (json \ "subcontractorsToInclude").as[List[Int]] mustBe List(1)
    }

    "handle an empty subcontractorsToInclude list" in {
      val model = SelectSubcontractorsFormData(subcontractorsToInclude = Nil)

      val json = Json.toJson(model)
      json.as[SelectSubcontractorsFormData] mustBe model
    }

    "support equal comparison" in {
      val model1 = SelectSubcontractorsFormData(List(1))
      val model2 = SelectSubcontractorsFormData(List(1))

      model1 mustBe model2
    }
  }
}
