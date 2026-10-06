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

package models.finalvalidation

import play.api.libs.json.*

sealed trait MonthlyFinalValidationSource

object MonthlyFinalValidationSource {

  case object SelectSubcontractors extends MonthlyFinalValidationSource

  final case class WhichSubcontractorsToAdd(mode: String) extends MonthlyFinalValidationSource

  implicit val format: Format[MonthlyFinalValidationSource] =
    new Format[MonthlyFinalValidationSource] {

      override def writes(source: MonthlyFinalValidationSource): JsValue =
        source match {
          case SelectSubcontractors           =>
            Json.obj("source" -> "selectSubcontractors")
          case WhichSubcontractorsToAdd(mode) =>
            Json.obj(
              "source" -> "whichSubcontractorsToAdd",
              "mode"   -> mode
            )
        }

      override def reads(
        json: JsValue
      ): JsResult[MonthlyFinalValidationSource] =
        (json \ "source")
          .validate[String]
          .flatMap {
            case "selectSubcontractors" =>
              JsSuccess(SelectSubcontractors)

            case "whichSubcontractorsToAdd" =>
              (json \ "mode")
                .validate[String]
                .map(WhichSubcontractorsToAdd.apply)

            case other =>
              JsError(s"Unknown MonthlyFinalValidationSource: $other")
          }
    }
}
