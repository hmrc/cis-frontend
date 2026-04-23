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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import viewmodels.govuk.checkbox._

case class Subcontractor(id: String, name: String)

object Subcontractor {
  implicit val format: OFormat[Subcontractor] = Json.format[Subcontractor]
}

object WhichSubcontractorsToAdd {

  // TODO: Replace with real data from backend
  val mockSubcontractors: Seq[Subcontractor] = Seq(
    Subcontractor("1", "Alice, A"),
    Subcontractor("2", "Apex Construction Solutions"),
    Subcontractor("3", "Bob, B"),
    Subcontractor("4", "Bloggs, Joe"),
    Subcontractor("5", "Bloggs, Joseph"),
    Subcontractor("6", "Build Right Construction"),
    Subcontractor("7", "Charles, C"),
    Subcontractor("8", "Dave, D"),
    Subcontractor("9", "Draft Services Ltd"),
    Subcontractor("10", "Elise, E"),
    Subcontractor("11", "Frank, F"),
    Subcontractor("12", "Northern Trades Ltd"),
    Subcontractor("13", "Pro-Build Subcontractors"),
    Subcontractor("14", "Tynewear Ltd"),
    Subcontractor("15", "SubbyCo Ltd")
  )

  def checkboxItems(subcontractors: Seq[Subcontractor]): Seq[CheckboxItem] =
    subcontractors.zipWithIndex.map { case (sub, index) =>
      CheckboxItemViewModel(
        content = Text(sub.name),
        fieldId = "value",
        index = index,
        value = sub.id
      )
    }
}
