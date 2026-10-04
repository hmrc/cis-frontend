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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.requests.{JourneyRequest, SchemeRequest}
import models.{JourneyId, UserAnswers}
import play.api.mvc.ActionTransformer
import repositories.SessionRepository

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[MonthlyReturnActionImpl])
abstract class MonthlyReturnAction extends ActionTransformer[SchemeRequest, JourneyRequest]

@Singleton
final class MonthlyReturnActionImpl @Inject() (
  sessionRepository: SessionRepository
)(using val executionContext: ExecutionContext)
    extends MonthlyReturnAction {

  private val JOURNEY_TYPE = "MonthlyReturn"

  def transform[B](req: SchemeRequest[B]): Future[JourneyRequest[B]] =
    val journeyId = JourneyId(req.identifier.userId, JOURNEY_TYPE, req.cisPath.toUrl).asString

    sessionRepository
      .get(journeyId)
      .map { uaOpt =>
        val ua = uaOpt getOrElse UserAnswers(journeyId)
        new JourneyRequest(req, ua)
      }
}
