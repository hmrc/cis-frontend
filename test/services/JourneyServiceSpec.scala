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

package services

import base.SpecBase
import models.{JourneyScope, UserAnswers}
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.monthlyreturns.CisTaxpayer
import models.requests.{IdentifierRequest, SchemeRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.ReturnTypePage
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

class JourneyServiceSpec extends SpecBase with MockitoSugar {

  private given ExecutionContext = ExecutionContext.global

  private def schemeRequest = {
    val identifier = IdentifierRequest(
      FakeRequest(GET, "/"),
      userAnswersId,
      None,
      None
    )

    new SchemeRequest(identifier, mock[CisTaxpayer])
  }

  "JourneyService" - {

    "create a new UUID-scoped journey with initial answers" in {
      val repository = mock[SessionRepository]
      val service    = new JourneyService(repository)
      val request    = schemeRequest

      when(repository.get(userAnswersId))
        .thenReturn(Future.successful(None))

      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val flowId = service
        .create(
          request,
          (ua, id) => ua.set(ReturnTypePage(id), MonthlyStandardReturn)
        )
        .futureValue

      val id = JourneyScope.subJourneyId(request.cisPath, flowId)

      val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(repository).set(captor.capture())

      val savedAnswers = captor.getValue

      savedAnswers.id mustBe userAnswersId
      savedAnswers.hasSubJourney(id) mustBe true
      savedAnswers.get(ReturnTypePage(id)) mustBe Some(MonthlyStandardReturn)
      savedAnswers.get(ReturnTypePage) mustBe None
    }

    "preserve existing answers when creating another journey" in {
      val repository = mock[SessionRepository]
      val service    = new JourneyService(repository)
      val request    = schemeRequest

      val existingAnswers = emptyUserAnswers
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value

      when(repository.get(userAnswersId))
        .thenReturn(Future.successful(Some(existingAnswers)))

      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      val flowId = service
        .create(
          request,
          (ua, id) => ua.set(ReturnTypePage(id), MonthlyNilReturn)
        )
        .futureValue

      val id = JourneyScope.subJourneyId(request.cisPath, flowId)

      val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
      verify(repository).set(captor.capture())

      val savedAnswers = captor.getValue

      savedAnswers.get(ReturnTypePage) mustBe Some(MonthlyStandardReturn)
      savedAnswers.get(ReturnTypePage(id)) mustBe Some(MonthlyNilReturn)
    }

    "return a failed Future when saving the journey fails" in {
      val repository = mock[SessionRepository]
      val service    = new JourneyService(repository)

      when(repository.get(userAnswersId))
        .thenReturn(Future.successful(None))

      when(repository.set(any[UserAnswers]))
        .thenReturn(Future.failed(new RuntimeException("Database error")))

      val exception = service.create(schemeRequest).failed.futureValue

      exception.getMessage mustBe "Database error"
    }
  }
}
