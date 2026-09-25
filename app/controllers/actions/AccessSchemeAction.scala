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
import models.requests.IdentifierRequest
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionFilter, Result, Results}
import services.CisTaxpayerService
import views.html.PageNotFoundView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessSchemeAction @Inject() (
  val messagesApi: MessagesApi,
  cisTaxpayerService: CisTaxpayerService,
  notFoundView: PageNotFoundView
)(using ec: ExecutionContext)
    extends Results
    with I18nSupport
    with Logging {

  /** @param schemeId
    *   A value of "-" indicates that the scheme ID is to be deduced from the user's CIS enrolment. This is for
    *   Organisation users only. An Agent would be redirected to a landing page to select a client. This pattern was
    *   taken from the Fitbit API.
    */
  def apply(schemeId: String): ActionFilter[IdentifierRequest] = new ActionFilter[IdentifierRequest] {
    protected val executionContext: ExecutionContext = ec

    protected def filter[A](req: IdentifierRequest[A]): Future[Option[Result]] =
      given IdentifierRequest[?] = req

      if schemeId == "-" then
        if req.isAgent then
          logger.info(s"${req.agentInfo} tried to access ${req.uri}.")
          Future.successful(Some(Redirect(controllers.routes.IndexController.onPageLoad())))
        else Future.successful(None)
      else if req.isAgent then
        cisTaxpayerService
          .isClient(schemeId)
          .map { isClient =>
            if isClient then None
            else
              logger.info(s"${req.agentInfo} tried to access ${req.uri}.")
              Some(NotFound(notFoundView()))
          }
      else
        logger.info(s"Organisation user tried to access ${req.uri}.")
        Future.successful(Some(Redirect(controllers.routes.IndexController.onPageLoad())))
  }
}
