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

package controllers

import config.FrontendAppConfig
import controllers.actions.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.AgentLostAccessView

import javax.inject.{Inject, Named}

class AgentLostAccessController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("AgentIdentifier") identify: IdentifierAction,
  val controllerComponents: MessagesControllerComponents,
  view: AgentLostAccessView
)(implicit appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    identify { implicit request =>
      request.agentCode match {
        case Some(agentCode) =>
          val authoriseClientRequestUrl = appConfig.authoriseClientRequestUrl(agentCode)

          Ok(view(authoriseClientRequestUrl))
        case None            =>
          logger.warn("[AgentLostAccessController] Auth returned no agentCode")
          Redirect(controllers.routes.UnauthorisedAgentAffinityController.onPageLoad())
      }
    }
}
