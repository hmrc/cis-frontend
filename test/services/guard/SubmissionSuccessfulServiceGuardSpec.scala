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

import models.UserAnswers
import models.requests.DataRequest
import models.submission.SubmissionDetails
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pages.submission.SubmissionDetailsPage
import play.api.test.FakeRequest
import services.guard.SubmissionSuccessfulCheck.{GuardFailed, GuardPassed}

import java.time.Instant

class SubmissionSuccessfulServiceGuardSpec extends AnyWordSpec with Matchers with ScalaFutures {

  private val guard = new SubmissionSuccessfulServiceGuardImpl

  private def emptyUserAnswers(userId: String = "uid"): UserAnswers =
    UserAnswers(userId)

  private def dataRequest(ua: UserAnswers): DataRequest[_] =
    DataRequest(
      request = FakeRequest(),
      userId = ua.id,
      userAnswers = ua,
      employerReference = None
    )

  private def submissionDetails(status: String, irMark: String = ""): SubmissionDetails =
    SubmissionDetails(id = "sub-123", status = status, irMark = irMark, submittedAt = Instant.now())

  "SubmissionSuccessfulServiceGuardImpl.check" should {

    "return GuardFailed when SubmissionDetailsPage is absent" in {
      implicit val request: DataRequest[_] = dataRequest(emptyUserAnswers())

      guard.check.futureValue mustBe GuardFailed
    }

    "return GuardPassed when status is SUBMITTED" in {
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, submissionDetails("SUBMITTED", irMark = "someIrMark")).get
      implicit val request: DataRequest[_] = dataRequest(ua)

      guard.check.futureValue mustBe GuardPassed
    }

    "return GuardPassed when status is SUBMITTED_NO_RECEIPT" in {
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, submissionDetails("SUBMITTED_NO_RECEIPT")).get
      implicit val request: DataRequest[_] = dataRequest(ua)

      guard.check.futureValue mustBe GuardPassed
    }

    "return GuardPassed when status is not a success status but irMark is non-empty" in {
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, submissionDetails("ACCEPTED", irMark = "someIrMark")).get
      implicit val request: DataRequest[_] = dataRequest(ua)

      guard.check.futureValue mustBe GuardPassed
    }

    "return GuardFailed when status is not a success status and irMark is empty" in {
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, submissionDetails("PENDING", irMark = "")).get
      implicit val request: DataRequest[_] = dataRequest(ua)

      guard.check.futureValue mustBe GuardFailed
    }
  }
}
