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

package models.requests

import play.api.mvc.{Request, WrappedRequest}
import models.{EmployerReference, UserAnswers}

case class OptionalDataRequest[A](
  request: Request[A],
  userId: String,
  userAnswers: Option[UserAnswers],
  employerReference: Option[EmployerReference] = None,
  agentReference: Option[String] = None,
  isAgent: Boolean = false
) extends WrappedRequest[A](request)

trait DataRequestFields[A] {
  val request: Request[A]
  val userId: String
  val userAnswers: UserAnswers
  val employerReference: Option[EmployerReference]
  val agentReference: Option[String]
  val isAgent: Boolean
}

case class DataRequest[A](
  request: Request[A],
  userId: String,
  userAnswers: UserAnswers,
  employerReference: Option[EmployerReference] = None,
  agentReference: Option[String] = None,
  isAgent: Boolean = false
) extends WrappedRequest[A](request)
    with DataRequestFields[A]

case class CisIdDataRequest[A](
  request: Request[A],
  userId: String,
  userAnswers: UserAnswers,
  cisId: String,
  employerReference: Option[EmployerReference] = None,
  agentReference: Option[String] = None,
  isAgent: Boolean = false
) extends WrappedRequest[A](request)
    with DataRequestFields[A]
