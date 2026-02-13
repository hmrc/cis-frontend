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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.i18n.{DefaultMessagesApi, Lang, Messages, MessagesImpl}
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.Aliases.Text

class SelectedSubcontractorSpec extends AnyWordSpec with Matchers {

  "SelectedSubcontractor JSON format" should {

    "round-trip (writes -> reads) with all fields populated" in {
      val model = SelectedSubcontractor(
        id = 123L,
        name = "Test Subcontractor Ltd",
        totalPaymentsMade = Some(1500.50),
        costOfMaterials = Some(250.00),
        totalTaxDeducted = Some(300.10)
      )

      val js = Json.toJson(model)
      js.as[SelectedSubcontractor] mustBe model
    }

    "round-trip (writes -> reads) with only required fields" in {
      val model = SelectedSubcontractor(
        id = 456L,
        name = "Minimal Subcontractor",
        totalPaymentsMade = None,
        costOfMaterials = None,
        totalTaxDeducted = None
      )

      val js = Json.toJson(model)
      js.as[SelectedSubcontractor] mustBe model
    }

    "parse minimal JSON with only required fields" in {
      val json =
        Json.parse(
          """
            |{
            |  "id": 789,
            |  "name": "JSON Subcontractor"
            |}
          """.stripMargin
        )

      val parsed = json.as[SelectedSubcontractor]
      parsed.id mustBe 789L
      parsed.name mustBe "JSON Subcontractor"
      parsed.totalPaymentsMade mustBe None
      parsed.costOfMaterials mustBe None
      parsed.totalTaxDeducted mustBe None
    }

    "parse JSON with all fields populated" in {
      val json =
        Json.parse(
          """
            |{
            |  "id": 100,
            |  "name": "Full Subcontractor",
            |  "totalPaymentsMade": 2000.75,
            |  "costOfMaterials": 500.25,
            |  "totalTaxDeducted": 400.00
            |}
          """.stripMargin
        )

      val parsed = json.as[SelectedSubcontractor]
      parsed.id mustBe 100L
      parsed.name mustBe "Full Subcontractor"
      parsed.totalPaymentsMade mustBe Some(2000.75)
      parsed.costOfMaterials mustBe Some(500.25)
      parsed.totalTaxDeducted mustBe Some(400.00)
    }

    "fail to parse when id is missing" in {
      val json =
        Json.parse(
          """
            |{
            |  "name": "Missing Id"
            |}
          """.stripMargin
        )

      json.validate[SelectedSubcontractor].isError mustBe true
    }

    "fail to parse when name is missing" in {
      val json =
        Json.parse(
          """
            |{
            |  "id": 123
            |}
          """.stripMargin
        )

      json.validate[SelectedSubcontractor].isError mustBe true
    }
  }

  "SelectedSubcontractor.radioItems" should {

    implicit val messages: Messages =
      MessagesImpl(Lang("en"), new DefaultMessagesApi())

    "return an empty sequence when given no subcontractors" in {
      val items = SelectedSubcontractor.radioItems(Seq.empty)
      items mustBe empty
    }

    "create radio items with correct id, value and label for each subcontractor" in {
      val subcontractors = Seq(
        SelectedSubcontractor(1L, "First Subcontractor", None, None, None),
        SelectedSubcontractor(2L, "Second Subcontractor", None, None, None)
      )

      val items = SelectedSubcontractor.radioItems(subcontractors)

      items.size mustBe 2

      items.head.id mustBe Some("subcontractor-1")
      items.head.value mustBe Some("1")
      items.head.content mustBe Text("First Subcontractor")

      items(1).id mustBe Some("subcontractor-2")
      items(1).value mustBe Some("2")
      items(1).content mustBe Text("Second Subcontractor")
    }
  }
}
