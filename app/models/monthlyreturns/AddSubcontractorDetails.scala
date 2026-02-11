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

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait AddSubcontractorDetails

object AddSubcontractorDetails extends Enumerable.Implicits {

  case object Option1 extends WithName("option1") with AddSubcontractorDetails
  case object Option2 extends WithName("option2") with AddSubcontractorDetails

  val values: Seq[AddSubcontractorDetails] = Seq(
    Option1,
    Option2
  )

  def options(implicit messages: Messages): Seq[RadioItem] =
    values.map { value =>
      RadioItem(
        content = Text(messages(s"monthlyreturns.addSubcontractorDetails.${value.toString}")),
        value = Some(value.toString),
        id = Some(value.toString)
      )
    }

  def optionsWithLabels(labels: Seq[String]): Seq[RadioItem] =
    values.zip(labels).map { case (value, label) =>
      RadioItem(
        content = Text(label),
        value = Some(value.toString),
        id = Some(value.toString)
      )
    }

  implicit val enumerable: Enumerable[AddSubcontractorDetails] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
