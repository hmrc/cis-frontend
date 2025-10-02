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

package controllers.actions

import base.SpecBase
import controllers.Execution.trampoline
import models.requests.DataRequest
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.CisIdPage
import play.api.mvc.Result
import play.api.test.*
import play.api.test.Helpers.*
import play.api.test.FakeRequest

import scala.concurrent.Future

class CisIdRequiredActionSpec extends SpecBase with MockitoSugar {

  object Harness extends CisIdRequiredActionImpl {
    def callRefine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  "CisId Required Action" - {

    "when there is no Cis Id in UserAnswers" - {
      "must return Left and redirect to Unauthorised Organisation Affinity" in {

        val result = Harness.callRefine(DataRequest(FakeRequest(), "id", emptyUserAnswers))

        whenReady(result) { result =>

          result.isLeft mustBe true

          result match {
            case Left(res) =>
              redirectLocation(Future.successful(res)) mustBe Some(
                "/construction-industry-scheme/monthly-return/unauthorised/organisation"
              )

            case Right(_) =>
              fail("Expected a redirect Result but got DataRequest")
          }
        }
      }

    }

    "when there is a Cis Id in UserAnswers" - {
      "must return Right and get valid DataRequest" in {

        val result = Harness.callRefine(DataRequest(FakeRequest(), "id", userAnswersWithCisId))

        whenReady(result) { result =>

          result.isRight mustBe true

          result match {
            case Right(dataRequest) =>
              dataRequest.userAnswers.get(CisIdPage) mustBe Some("1")
              dataRequest.userId mustBe "id"

            case Left(_) =>
              fail("Expected a DataRequest in Right but got a redirect Result")
          }
        }
      }

    }

  }

}
