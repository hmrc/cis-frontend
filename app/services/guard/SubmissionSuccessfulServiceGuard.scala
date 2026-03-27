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
import pages.submission.SubmissionDetailsPage

import javax.inject.Singleton
import scala.concurrent.Future
import play.api.Logging
import services.guard.SubmissionSuccessfulCheck.{GuardFailed, GuardPassed}

sealed trait SubmissionSuccessfulCheck
object SubmissionSuccessfulCheck {
  case object GuardPassed extends SubmissionSuccessfulCheck
  case object GuardFailed extends SubmissionSuccessfulCheck
}

trait SubmissionSuccessfulServiceGuard {
  def check(implicit request: DataRequest[_]): Future[SubmissionSuccessfulCheck]
}

@Singleton
class SubmissionSuccessfulServiceGuardImpl extends SubmissionSuccessfulServiceGuard with Logging {
  def check(implicit request: DataRequest[_]): Future[SubmissionSuccessfulCheck] =
    val passed = request.userAnswers.get(SubmissionDetailsPage).exists { details =>
      details.status == "SUBMITTED" || details.status == "SUBMITTED_NO_RECEIPT" || details.irMark.nonEmpty
    }
    Future.successful(if (passed) GuardPassed else GuardFailed)
}
