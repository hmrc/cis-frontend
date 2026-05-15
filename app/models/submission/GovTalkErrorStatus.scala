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

  private val KindField = "kind"

  private val RecoverableErrorKind  = "RecoverableError"
  private val FatalErrorKind        = "FatalError"
  private val DepartmentalErrorKind = "DepartmentalError"
  private val ServerErrorKind       = "ServerError"
  private val NoResponseKind        = "NoResponse"
  private val OtherStatusKind       = "OtherStatus"

  implicit val format: Format[GovTalkErrorStatus] = new Format[GovTalkErrorStatus] {

    override def reads(json: JsValue): JsResult[GovTalkErrorStatus] =
      (json \ KindField).validate[String].flatMap {
        case RecoverableErrorKind  =>
          for {
            code <- (json \ "errorCode").validate[String]
            text <- (json \ "errorText").validate[String]
          } yield RecoverableError(code, text)
        case FatalErrorKind        =>
          for {
            code <- (json \ "errorCode").validate[String]
            text <- (json \ "errorText").validate[String]
          } yield FatalError(code, text)
        case DepartmentalErrorKind =>
          (json \ "errorText").validate[String].map(DepartmentalError.apply)
        case ServerErrorKind       =>
          (json \ "httpStatus").validate[Int].map(ServerError.apply)
        case NoResponseKind        =>
          JsSuccess(NoResponse)
        case OtherStatusKind       =>
          JsSuccess(OtherStatus)
        case other                 =>
          JsError(s"Unknown GovTalkErrorStatus kind: $other")
      }

    override def writes(o: GovTalkErrorStatus): JsValue = o match {
      case RecoverableError(code, text) =>
        Json.obj(KindField -> RecoverableErrorKind, "errorCode" -> code, "errorText" -> text)
      case FatalError(code, text)       =>
        Json.obj(KindField -> FatalErrorKind, "errorCode" -> code, "errorText" -> text)
      case DepartmentalError(text)      =>
        Json.obj(KindField -> DepartmentalErrorKind, "errorText" -> text)
      case ServerError(httpStatus)      =>
        Json.obj(KindField -> ServerErrorKind, "httpStatus" -> httpStatus)
      case NoResponse                   =>
        Json.obj(KindField -> NoResponseKind)
      case OtherStatus                  =>
        Json.obj(KindField -> OtherStatusKind)
    }
  }
}
