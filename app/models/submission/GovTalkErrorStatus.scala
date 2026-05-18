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

package models.submission

import play.api.libs.json.*

sealed trait GovTalkErrorStatus

object GovTalkErrorStatus {

  final case class RecoverableError(errorCode: String, errorText: String) extends GovTalkErrorStatus

  final case class FatalError(errorCode: String, errorText: String) extends GovTalkErrorStatus

  final case class DepartmentalError(errorText: String) extends GovTalkErrorStatus

  final case class ServerError(httpStatus: Int) extends GovTalkErrorStatus

  case object NoResponse extends GovTalkErrorStatus

  case object OtherStatus extends GovTalkErrorStatus

  private given OFormat[RecoverableError]  = Json.format
  private given OFormat[FatalError]        = Json.format
  private given OFormat[DepartmentalError] = Json.format
  private given OFormat[ServerError]       = Json.format

  implicit val format: OFormat[GovTalkErrorStatus] = new OFormat[GovTalkErrorStatus] {

    override def reads(json: JsValue): JsResult[GovTalkErrorStatus] =
      (json \ "kind").validate[String].flatMap {
        case "RecoverableError"  => json.validate[RecoverableError]
        case "FatalError"        => json.validate[FatalError]
        case "DepartmentalError" => json.validate[DepartmentalError]
        case "ServerError"       => json.validate[ServerError]
        case "NoResponse"        => JsSuccess(NoResponse)
        case "OtherStatus"       => JsSuccess(OtherStatus)
        case other               => JsError(s"Unknown GovTalkErrorStatus kind: $other")
      }

    override def writes(o: GovTalkErrorStatus): JsObject = o match {
      case e: RecoverableError  => Json.toJsObject(e) + ("kind" -> JsString("RecoverableError"))
      case e: FatalError        => Json.toJsObject(e) + ("kind" -> JsString("FatalError"))
      case e: DepartmentalError => Json.toJsObject(e) + ("kind" -> JsString("DepartmentalError"))
      case e: ServerError       => Json.toJsObject(e) + ("kind" -> JsString("ServerError"))
      case NoResponse           => Json.obj("kind" -> "NoResponse")
      case OtherStatus          => Json.obj("kind" -> "OtherStatus")
    }
  }
}
