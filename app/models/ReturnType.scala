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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
import play.api.mvc.{JavascriptLiteral, QueryStringBindable}

sealed trait ReturnType {
  def amendmentFlag: String
}

object ReturnType extends Enumerable.Implicits {

  case object MonthlyNilReturn extends WithName("MonthlyNilReturn") with ReturnType {
    override val amendmentFlag: String = "N"
  }
  case object MonthlyStandardReturn extends WithName("MonthlyStandardReturn") with ReturnType {
    override val amendmentFlag: String = "N"
  }

  case object MonthlyAmendedNilReturn extends WithName("MonthlyAmendedNilReturn") with ReturnType {
    override val amendmentFlag: String = "Y"
  }

  case object MonthlyAmendedStandardReturn extends WithName("MonthlyAmendedStandardReturn") with ReturnType {
    override val amendmentFlag: String = "Y"
  }

  def amendedFrom(originalReturnType: ReturnType): Option[ReturnType] = originalReturnType match {
    case MonthlyNilReturn      => Some(MonthlyAmendedNilReturn)
    case MonthlyStandardReturn => Some(MonthlyAmendedStandardReturn)
    case _                     => None
  }

  val values: Seq[ReturnType] = Seq(
    MonthlyNilReturn,
    MonthlyStandardReturn,
    MonthlyAmendedNilReturn,
    MonthlyAmendedStandardReturn
  )

  implicit val enumerable: Enumerable[ReturnType] =
    Enumerable(values.map(v => v.toString -> v): _*)

  implicit val jsLiteral: JavascriptLiteral[ReturnType] = new JavascriptLiteral[ReturnType] {
    override def to(value: ReturnType): String = value match {
      case MonthlyNilReturn             => "MonthlyNilReturn"
      case MonthlyStandardReturn        => "MonthlyStandardReturn"
      case MonthlyAmendedNilReturn      => "MonthlyAmendedNilReturn"
      case MonthlyAmendedStandardReturn => "MonthlyAmendedStandardReturn"
    }
  }

  implicit val format: Format[ReturnType] = new Format[ReturnType] {
    override def reads(json: JsValue): JsResult[ReturnType] = json match {
      case JsString("MonthlyNilReturn")             => JsSuccess(MonthlyNilReturn)
      case JsString("MonthlyStandardReturn")        => JsSuccess(MonthlyStandardReturn)
      case JsString("MonthlyAmendedNilReturn")      => JsSuccess(MonthlyAmendedNilReturn)
      case JsString("MonthlyAmendedStandardReturn") => JsSuccess(MonthlyAmendedStandardReturn)
      case _                                        => JsError("Not a valid return type")
    }

    override def writes(o: ReturnType): JsValue = o match {
      case MonthlyNilReturn             => JsString("MonthlyNilReturn")
      case MonthlyStandardReturn        => JsString("MonthlyStandardReturn")
      case MonthlyAmendedNilReturn      => JsString("MonthlyAmendedNilReturn")
      case MonthlyAmendedStandardReturn => JsString("MonthlyAmendedStandardReturn")
    }
  }

  implicit def queryStringBindable(implicit
    strBinder: QueryStringBindable[String]
  ): QueryStringBindable[ReturnType] = new QueryStringBindable[ReturnType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ReturnType]] =
      strBinder.bind(key, params).map {
        case Right("MonthlyNilReturn")             => Right(MonthlyNilReturn)
        case Right("MonthlyStandardReturn")        => Right(MonthlyStandardReturn)
        case Right("MonthlyAmendedNilReturn")      => Right(MonthlyAmendedNilReturn)
        case Right("MonthlyAmendedStandardReturn") => Right(MonthlyAmendedStandardReturn)
        case Right(other)                          => Left(s"Unknown ReturnType: $other")
        case Left(err)                             => Left(err)
      }

    override def unbind(key: String, value: ReturnType): String =
      strBinder.unbind(key, jsLiteral.to(value))
  }
}
