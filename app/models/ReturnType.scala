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

package models

sealed trait ReturnType

object ReturnType extends Enumerable.Implicits {

  case object MonthlyNilReturn extends WithName("monthlyNilReturn") with ReturnType
  case object MonthlyStandardReturn extends WithName("monthlyStandardReturn") with ReturnType

  val values: Seq[ReturnType] = Seq(
    MonthlyNilReturn,
    MonthlyStandardReturn
  )

  implicit val enumerable: Enumerable[ReturnType] =
    Enumerable(values.map(v => v.toString -> v): _*)
}
