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
import play.api.mvc.Result
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
        }
      }

    }

    "when there is a Cis Id in UserAnswers" - {
      "must return Right and get valid DataRequest" in {

        val result = Harness.callRefine(DataRequest(FakeRequest(), "id", userAnswersWithCisId))

        whenReady(result) { result =>
          result.isRight mustBe true
        }
      }

    }

  }

}
