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
import play.api.Logging
import services.guard.SubmissionSuccessfulCheck.{GuardFailed, GuardPassed}

import javax.inject.Singleton
import scala.concurrent.Future

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

  def check(implicit request: DataRequest[_]): Future[SubmissionSuccessfulCheck] = {
    val result = request.userAnswers.get(SubmissionDetailsPage).exists { details =>
      val submittedOrAmendment = details.status == "SUBMITTED" || details.amendment.contains("Y")
      val irMarksValid      = details.irMark.nonEmpty &&
        details.hmrcMarkGgis.exists(g => g.nonEmpty && g == details.irMark)

      if (!submittedOrAmendment)
        logger.warn(
          s"[SubmissionSuccessfulServiceGuard] Guard failed: status=${details.status}, amendment=${details.amendment}"
        )
      if (!irMarksValid)
        logger.warn(s"[SubmissionSuccessfulServiceGuard] Guard failed: irMark empty or hmrcMarkGgis mismatch")

      submittedOrAmendment && irMarksValid
    }

    Future.successful(if (result) GuardPassed else GuardFailed)
  }
}
