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

import play.api.mvc.{JavascriptLiteral, QueryStringBindable}

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

  implicit val jsLiteral: JavascriptLiteral[ReturnType] = new JavascriptLiteral[ReturnType] {
    override def to(value: ReturnType): String = value match {
      case MonthlyNilReturn      => "MonthlyNilReturn"
      case MonthlyStandardReturn => "MonthlyStandardReturn"
    }
  }

  implicit def queryStringBindable(implicit
    strBinder: QueryStringBindable[String]
  ): QueryStringBindable[ReturnType] = new QueryStringBindable[ReturnType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ReturnType]] =
      strBinder.bind(key, params).map {
        case Right("MonthlyNilReturn")      => Right(MonthlyNilReturn)
        case Right("MonthlyStandardReturn") => Right(MonthlyStandardReturn)
        case Right(other)                   => Left(s"Unknown ReturnType: $other")
        case Left(err)                      => Left(err)
      }

    override def unbind(key: String, value: ReturnType): String =
      strBinder.unbind(key, jsLiteral.to(value))
  }
}
