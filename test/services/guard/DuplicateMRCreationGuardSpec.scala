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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import pages.monthlyreturns.NilReturnStatusPage
import play.api.test.FakeRequest
import services.guard.DuplicateMRCreationCheck.DuplicateFound

import scala.concurrent.Future

class DuplicateMRCreationGuardSpec extends AnyWordSpec with Matchers with ScalaFutures {
  private val guard = new DuplicateMRCreationGuardImpl

  private def emptyUserAnswers(userId: String = "uid"): UserAnswers =
    UserAnswers(userId)

  private def dataRequest(ua: UserAnswers): DataRequest[_] =
    DataRequest(
      request = FakeRequest(),
      userId = ua.id,
      userAnswers = ua,
      employerReference = None,
      isAgent = false
    )

  private val submittedStatus: String = "SUBMITTED"

  "DuplicateMRCreationGuardImpl.check" should {

    "return NoDuplicate when NilReturnStatusPage is present" in {
      val ua                               = emptyUserAnswers()
      implicit val request: DataRequest[_] = dataRequest(ua)

      val result: Future[DuplicateMRCreationCheck] = guard.check
      result.futureValue mustBe DuplicateMRCreationCheck.NoDuplicate
    }

    "return DuplicateFound when NilReturnStatusPage is present" in {
      val uaWithStatus                     = emptyUserAnswers().set(NilReturnStatusPage, submittedStatus).get
      implicit val request: DataRequest[_] = dataRequest(uaWithStatus)

      val result = guard.check
      result.futureValue mustBe DuplicateFound
    }

  }

}
