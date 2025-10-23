/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}
import java.time.Instant

case class UpdateSubmissionRequest(
  instanceId: String,
  taxYear: Int,
  taxMonth: Int,
  hmrcMarkGenerated: Option[String] = None,
  submittableStatus: String,
  amendment: Option[String] = None,
  hmrcMarkGgis: Option[String] = None,
  submissionRequestDate: Option[Instant] = None,
  acceptedTime: Option[String] = None,
  emailRecipient: Option[String] = None,
  agentId: Option[String] = None,
  govtalkErrorCode: Option[String] = None,
  govtalkErrorType: Option[String] = None,
  govtalkErrorMessage: Option[String] = None
)

object UpdateSubmissionRequest {
  implicit val format: OFormat[UpdateSubmissionRequest] = Json.format[UpdateSubmissionRequest]
}
