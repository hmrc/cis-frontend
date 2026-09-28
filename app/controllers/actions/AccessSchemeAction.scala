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

import com.google.inject.{Inject, Singleton}
import models.UserAnswers
import models.requests.{DataRequest, IdentifierRequest}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionRefiner, Result, Results}
import repositories.SessionRepository
import services.CisTaxpayerService
import views.html.PageNotFoundView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessSchemeAction @Inject() (
  val messagesApi: MessagesApi,
  cisTaxpayerService: CisTaxpayerService,
  sessionRepository: SessionRepository,
  notFoundView: PageNotFoundView
)(using ec: ExecutionContext)
    extends Results
    with I18nSupport
    with Logging {
  private val WILDCARD = "-"

  /** @param schemeId
    *   A wildcard value indicates that the scheme ID is to be deduced from the user's CIS enrolment. This is for
    *   Organisation users only. An Agent would be redirected to a landing page to select a client. This pattern is used
    *   in the Fitbit API.
    */
  def apply(schemeId: String): ActionRefiner[IdentifierRequest, DataRequest] =
    new ActionRefiner[IdentifierRequest, DataRequest] {
      protected val executionContext: ExecutionContext = ec

      protected def refine[A](req: IdentifierRequest[A]): Future[Either[Result, DataRequest[A]]] =
        given IdentifierRequest[A] = req

        if req.isAgent then
          if schemeId == WILDCARD then
            logger.info(s"${req.agentInfo} tried to access wildcard URI ${req.uri}; redirecting to landing page.")
            Future.successful(Left(Redirect(controllers.routes.IndexController.onPageLoad())))
          else
            cisTaxpayerService
              .isClient(schemeId)
              .flatMap { isClient =>
                if isClient then buildDataRequest(userAnswersId = s"${req.userId}/$schemeId") map Right.apply
                else
                  logger.info(s"${req.agentInfo} tried to access ${req.uri}.")
                  Future.successful(Left(NotFound(notFoundView())))
              }
        else if schemeId == WILDCARD then buildDataRequest(userAnswersId = req.userId) map Right.apply
        else if schemeId.forall(_.isLetterOrDigit) then
          // The alphanumeric check above ensures we only do the string replacement below when no URL encoding is used
          logger.info(s"Organisation user tried to access ${req.uri}; replacing scheme ID with wildcard.")
          val wildcardUri = req.uri.replace(s"/$schemeId/", s"/$WILDCARD/")
          Future.successful(Left(Redirect(wildcardUri)))
        else
          logger.info(s"Organisation user tried to access ${req.uri} with invalid scheme ID; returning 404.")
          Future.successful(Left(NotFound(notFoundView())))
    }

  /** @param userAnswersId
    *   For an Organisation user, this is just the user ID because they only have access to 1 scheme. For an Agent user,
    *   this is the user ID and scheme ID separated by a forward slash, allowing the Agent to manage multiple clients in
    *   parallel.
    */
  private def buildDataRequest[A](userAnswersId: String)(using req: IdentifierRequest[A]) =
    for
      uaOpt <- sessionRepository.get(userAnswersId)
      ua     = uaOpt getOrElse UserAnswers(userAnswersId)
    yield DataRequest(req, req.userId, ua, req.employerReference, req.agentReference, req.isAgent, req.agentCode)
}
