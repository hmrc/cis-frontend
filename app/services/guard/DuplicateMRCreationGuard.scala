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

package services.guard

import models.requests.DataRequest
import pages.monthlyreturns.NilReturnStatusPage

import javax.inject.Singleton
import scala.concurrent.Future
import play.api.Logging
import services.guard.DuplicateMRCreationCheck.{DuplicateFound, NoDuplicate}

sealed trait DuplicateMRCreationCheck
object DuplicateMRCreationCheck {
  case object NoDuplicate extends DuplicateMRCreationCheck
  case object DuplicateFound extends DuplicateMRCreationCheck
}

trait DuplicateMRCreationGuard {
  def check(implicit request: DataRequest[_]): Future[DuplicateMRCreationCheck]
}

@Singleton
class DuplicateMRCreationGuardImpl extends DuplicateMRCreationGuard with Logging {
  def check(implicit request: DataRequest[_]): Future[DuplicateMRCreationCheck] =
    val duplicate = request.userAnswers.get(NilReturnStatusPage).isDefined
    Future.successful(if (duplicate) DuplicateFound else NoDuplicate)
}
