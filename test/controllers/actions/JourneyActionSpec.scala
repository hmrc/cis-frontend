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

package controllers.actions

import base.SpecBase
import models.JourneyScope
import models.monthlyreturns.CisTaxpayer
import models.requests.{IdentifierRequest, SchemeRequest}
import org.mockito.Mockito.{mock, when}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.{AnyContent, Results}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class JourneyActionSpec extends SpecBase {

  private given ExecutionContext = scala.concurrent.ExecutionContext.global

  private def schemeRequest: SchemeRequest[AnyContent] = {
    val identifier = mock(classOf[IdentifierRequest[AnyContent]])
    when(identifier.userId).thenReturn(userAnswersId)

    new SchemeRequest(identifier, mock(classOf[CisTaxpayer]))
  }

  "JourneyAction" - {

    "return JourneyRequest when the subjourney exists" in {
      val request     = schemeRequest
      val flowId      = UUID.randomUUID()
      val id          = JourneyScope.subJourneyId(request.cisPath, flowId)
      val userAnswers = emptyUserAnswers.initialiseSubJourney(id).success.value
      val repository  = mockSessionRepository(Some(userAnswers))
      val action      = new JourneyActionImpl(repository)

      val result = action(flowId)
        .invokeBlock(
          request,
          journeyRequest => {
            journeyRequest.subJourneyId mustBe id
            journeyRequest.userAnswers mustBe userAnswers
            Future.successful(Results.Ok)
          }
        )
        .futureValue

      result.header.status mustBe OK
    }

    "redirect to journey recovery when the subjourney does not exist" in {
      val request    = schemeRequest
      val repository = mockSessionRepository(Some(emptyUserAnswers))
      val action     = new JourneyActionImpl(repository)

      val result = action(UUID.randomUUID())
        .invokeBlock(
          request,
          _ => Future.successful(Results.Ok)
        )
        .futureValue

      result.header.status mustBe SEE_OTHER
      result.header.headers.get("Location") mustBe Some(
        controllers.routes.JourneyRecoveryController.onPageLoad().url
      )
    }
  }
}
