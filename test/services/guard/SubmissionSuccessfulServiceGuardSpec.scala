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
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pages.submission.SubmissionDetailsPage
import play.api.test.FakeRequest

import java.time.Instant

class SubmissionSuccessfulServiceGuardSpec extends AnyWordSpec with Matchers {

  private val guard  = new SubmissionSuccessfulServiceGuardImpl
  private val irMark = "Pyy1LRJh053AE+nuyp0GJR7oESw="

  private def emptyUserAnswers(userId: String = "uid"): UserAnswers =
    UserAnswers(userId)

  private def dataRequest(ua: UserAnswers): DataRequest[_] =
    DataRequest(
      request = FakeRequest(),
      userId = ua.id,
      userAnswers = ua,
      employerReference = None
    )

  private def submissionDetails(
    status: String = "SUBMITTED",
    irMark: String = irMark,
    amendment: Option[String] = None,
    hmrcMarkGgis: Option[String] = Some(irMark)
  ): SubmissionDetails =
    SubmissionDetails(
      id = "sub-1",
      status = status,
      irMark = irMark,
      submittedAt = Instant.now(),
      amendment = amendment,
      hmrcMarkGgis = hmrcMarkGgis
    )

  "SubmissionSuccessfulServiceGuardImpl.check" should {

    "return false when SubmissionDetailsPage is absent" in {
      implicit val request: DataRequest[_] = dataRequest(emptyUserAnswers())
      guard.check mustBe false
    }

    "return true when status is SUBMITTED and IRMarks match" in {
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, submissionDetails()).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe true
    }

    "return true when amendment is Y and IRMarks match (even if status is not SUBMITTED)" in {
      val details                          = submissionDetails(status = "PENDING", amendment = Some("Y"))
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe true
    }

    "return false when status is not SUBMITTED and amendment is not Y" in {
      val details                          = submissionDetails(status = "PENDING", amendment = Some("N"))
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe false
    }

    "return false when status is not SUBMITTED and amendment is None" in {
      val details                          = submissionDetails(status = "ACCEPTED", amendment = None)
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe false
    }

    "return false when irMark is empty" in {
      val details                          = submissionDetails(irMark = "", hmrcMarkGgis = Some(""))
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe false
    }

    "return false when hmrcMarkGgis is None" in {
      val details                          = submissionDetails(hmrcMarkGgis = None)
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe false
    }

    "return false when hmrcMarkGgis does not match irMark" in {
      val details                          = submissionDetails(hmrcMarkGgis = Some("differentMark"))
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe false
    }

    "return false when hmrcMarkGgis is empty string" in {
      val details                          = submissionDetails(hmrcMarkGgis = Some(""))
      val ua                               = emptyUserAnswers().set(SubmissionDetailsPage, details).get
      implicit val request: DataRequest[_] = dataRequest(ua)
      guard.check mustBe false
    }
  }
}
