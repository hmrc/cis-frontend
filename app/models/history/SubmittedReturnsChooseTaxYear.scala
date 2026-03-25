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

package models.history

import models.{Enumerable, WithName}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait SubmittedReturnsChooseTaxYear

object SubmittedReturnsChooseTaxYear extends Enumerable.Implicits {

  case object Option1 extends WithName("option1") with SubmittedReturnsChooseTaxYear
  case object Option2 extends WithName("option2") with SubmittedReturnsChooseTaxYear

  val values: Seq[SubmittedReturnsChooseTaxYear] = Seq(
    Option1, Option2
  )

  def options(implicit messages: Messages): Seq[RadioItem] = values.zipWithIndex.map {
    case (value, index) =>
      RadioItem(
        // TODO: here I am displaying the options from messages.en e.g: Option1 & Option2. Need to do that with test data
        content = Text(messages(s"history.submittedReturnsChooseTaxYear.${value.toString}")),
        value   = Some(value.toString),
        id      = Some(s"value_$index")
      )
  }

  implicit val enumerable: Enumerable[SubmittedReturnsChooseTaxYear] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
