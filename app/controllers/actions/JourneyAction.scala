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
import models.JourneyId
import models.requests.{JourneyRequest, SchemeRequest}
import play.api.mvc.{ActionRefiner, Result, Results}
import repositories.SessionRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[JourneyActionImpl])
abstract class JourneyAction {
  def apply(uuid: UUID): ActionRefiner[SchemeRequest, JourneyRequest]
}

@Singleton
final class JourneyActionImpl @Inject() (
  sessionRepository: SessionRepository
)(using ec: ExecutionContext)
    extends JourneyAction {

  def apply(uuid: UUID): ActionRefiner[SchemeRequest, JourneyRequest] =
    new ActionRefiner[SchemeRequest, JourneyRequest] with Results {
      val executionContext: ExecutionContext = ec

      def refine[B](req: SchemeRequest[B]): Future[Either[Result, JourneyRequest[B]]] = {
        val journeyId = JourneyId(req.identifier.userId, req.cisPath, uuid)

        sessionRepository.get(journeyId.asString).map {
          case Some(ua) => Right(new JourneyRequest(req, journeyId, ua))
          case None     => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      }
    }
}
