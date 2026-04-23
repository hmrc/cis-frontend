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

package models.amend

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import play.api.libs.json.Json
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem

class WhichSubcontractorsToAddSpec extends AnyFreeSpec with Matchers with OptionValues {

  "Subcontractor" - {

    "must serialise and deserialise" in {
      val sub  = Subcontractor("1", "Alice, A")
      val json = Json.toJson(sub)
      json.as[Subcontractor] mustEqual sub
    }
  }

  "WhichSubcontractorsToAdd" - {

    "mockSubcontractors must not be empty" in {
      WhichSubcontractorsToAdd.mockSubcontractors must not be empty
    }

    "checkboxItems must return one item per subcontractor" in {
      val subs  = Seq(Subcontractor("1", "Alice, A"), Subcontractor("2", "Bob, B"))
      val items = WhichSubcontractorsToAdd.checkboxItems(subs)

      items.length mustEqual 2
      items.head.value mustEqual "1"
      items(1).value mustEqual "2"
    }

    "checkboxItems must return items of type CheckboxItem" in {
      val subs  = WhichSubcontractorsToAdd.mockSubcontractors
      val items = WhichSubcontractorsToAdd.checkboxItems(subs)

      items.foreach(_ mustBe a[CheckboxItem])
    }
  }
}
