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

import javax.inject.{Inject, Singleton}

import models.{JourneyScope, UserAnswers}
import models.requests.SchemeRequest
import repositories.SessionRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class JourneyService @Inject() (sessionRepository: SessionRepository)(using ec: ExecutionContext) {

  def create[B](req: SchemeRequest[B]): Future[UUID] =
    create(req, (ua, _) => Success(ua))

  def create[B](req: SchemeRequest[B], initialise: (UserAnswers, String) => Try[UserAnswers]): Future[UUID] = {
    val flowId     = UUID.randomUUID()
    val documentId = req.identifier.userId
    val id         = JourneyScope.subJourneyId(req.cisPath, flowId)

    sessionRepository.get(documentId).flatMap { existing =>
      val userAnswers =
        existing.getOrElse(UserAnswers(documentId))

      val initialisedAnswers = for {
        started <- userAnswers.initialiseSubJourney(id)
        updated <- initialise(started, id)
        result  <- if updated.id == documentId then Success(updated)
                   else Failure(new IllegalArgumentException("Incorrect session ID"))
      } yield result

      Future.fromTry(initialisedAnswers).flatMap { updatedAnswers =>
        sessionRepository
          .set(updatedAnswers)
          .map(_ => flowId)
      }
    }
  }
}
