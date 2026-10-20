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
import models.{JourneyId, UserAnswers}
import models.requests.SchemeRequest
import repositories.SessionRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

@Singleton
class JourneyService @Inject() (
  sessionRepository: SessionRepository
)(using ec: ExecutionContext) {

  def create[B](req: SchemeRequest[B]): Future[JourneyId] =
    create(req, ua => Success(ua))

  def create[B](
    req: SchemeRequest[B],
    initialise: UserAnswers => Try[UserAnswers]
  ): Future[JourneyId] = {
    val journeyId = JourneyId(req.identifier.userId, req.cisPath, UUID.randomUUID())

    Future.fromTry(initialise(UserAnswers(journeyId.asString))).flatMap { ua =>
      if ua.id != journeyId.asString then Future.failed(new IllegalArgumentException("Incorrect journey ID"))
      else sessionRepository.set(ua).map(_ => journeyId)
    }
  }
}
