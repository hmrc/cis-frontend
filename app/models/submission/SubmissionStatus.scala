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

sealed trait SubmissionStatus
object SubmissionStatus {
  case object Started extends SubmissionStatus
  case object Pending extends SubmissionStatus
  case object Accepted extends SubmissionStatus
  case object TimedOut extends SubmissionStatus
  case object Submitted extends SubmissionStatus
  case object SubmittedNoReceipt extends SubmissionStatus
  case object DepartmentalError extends SubmissionStatus
  case object FatalError extends SubmissionStatus
  final case class Unknown(value: String) extends SubmissionStatus

  def fromString(s: String): SubmissionStatus =
    s match {
      case "STARTED"              => Started
      case "PENDING"              => Pending
      case "ACCEPTED"             => Accepted
      case "TIMED_OUT"            => TimedOut
      case "SUBMITTED"            => Submitted
      case "SUBMITTED_NO_RECEIPT" => SubmittedNoReceipt
      case "DEPARTMENTAL_ERROR"   => DepartmentalError
      case "FATAL_ERROR"          => FatalError
      case other                  => Unknown(other)
    }
}
