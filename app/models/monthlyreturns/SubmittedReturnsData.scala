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

import play.api.libs.json.{Format, JsError, JsString, JsSuccess, Json, OFormat, Reads, Writes}

import java.time.LocalDateTime

case class SubmittedReturnsData(
  scheme: SubmittedSchemeData,
  monthlyReturns: Seq[SubmittedMonthlyReturnData],
  submissions: Seq[SubmittedSubmissionData]
)

object SubmittedReturnsData {
  given format: OFormat[SubmittedReturnsData] = Json.format[SubmittedReturnsData]
}

case class SubmittedSchemeData(
  name: String,
  taxOfficeNumber: String,
  taxOfficeReference: String
)

object SubmittedSchemeData {
  given format: OFormat[SubmittedSchemeData] = Json.format[SubmittedSchemeData]
}

case class SubmittedMonthlyReturnData(
  monthlyReturnId: Long,
  taxYear: Int,
  taxMonth: Int,
  nilReturnIndicator: String,
  status: String,
  amendment: String,
  supersededBy: Option[Long],
  amendmentStatus: Option[String],
  monthlyReturnItems: Option[String]
)

object SubmittedMonthlyReturnData {
  given format: OFormat[SubmittedMonthlyReturnData] = Json.format[SubmittedMonthlyReturnData]
}

case class SubmittedSubmissionData(
  submissionId: Long,
  submissionType: Option[String],
  activeObjectId: Option[Long],
  status: String,
  hmrcMarkGenerated: Option[String],
  hmrcMarkGgis: Option[String],
  emailRecipient: Option[String],
  acceptedTime: Option[LocalDateTime]
)

object SubmittedSubmissionData {

  private given Format[LocalDateTime] = Format(
    Reads {
      case JsString(value) =>
        scala.util.Try(LocalDateTime.parse(value.take(19))) match {
          case scala.util.Success(dateTime) => JsSuccess(dateTime)
          case scala.util.Failure(_)        => JsError("error.expected.localdatetime")
        }
      case _               => JsError("error.expected.jsstring")
    },
    Writes(dateTime => JsString(dateTime.toString))
  )

  given format: OFormat[SubmittedSubmissionData] = Json.format[SubmittedSubmissionData]
}
