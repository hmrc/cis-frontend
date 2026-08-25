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

import models.agent.ClientListCheckPolicy
import models.agent.ClientListCheckPolicy.*
import play.api.Logging
import play.api.mvc.RequestHeader
import play.api.routing.Router

import javax.inject.{Inject, Singleton}

@Singleton
class ClientListCheckPolicyResolver @Inject() extends Logging {

  private type RouteKey = (String, String)

  private val groupARoutes: Set[RouteKey] =
    Set(
      "controllers.monthlyreturns.FileYourMonthlyCisReturnController" -> "startMonthlyReturn",
      "controllers.monthlyreturns.FileYourMonthlyCisReturnController" -> "startNilReturn",
      "controllers.monthlyreturns.ContinueReturnJourneyController"    -> "continueReturnJourney"
    )

  private val exemptControllers: Set[String] =
    Set(
      "controllers.SystemErrorController",
      "controllers.JourneyRecoveryController",
      "controllers.AccessDeniedController",
      "controllers.UnauthorisedAgentAffinityController",
      "controllers.UnauthorisedController",
      "controllers.UnauthorisedIndividualAffinityController",
      "controllers.UnauthorisedOrganisationAffinityController",
      "controllers.UnauthorisedWrongRoleController"
      // AgentLostAccessController when implemented
    )

  def resolve(request: RequestHeader): ClientListCheckPolicy = {

    val handlerDef = request.attrs.get(Router.Attrs.HandlerDef)

    val policy =
      if request.method != "GET" then Exempt
      else
        handlerDef match {

          case Some(handler) if groupARoutes.contains(handler.controller -> handler.method) =>
            GroupA

          case Some(handler) if exemptControllers.contains(handler.controller) =>
            Exempt

          case Some(handler) if handler.method == "onPageLoad" =>
            GroupA

          case _ =>
            Exempt
        }

    val handler =
      request.attrs
        .get(Router.Attrs.HandlerDef)
        .map(h => s"${h.controller}.${h.method}")
        .getOrElse("UnknownHandler")

    logger.info(
      s"[ClientListCheckPolicyResolver] method=${request.method} uri=${request.uri} handler=$handler policy=$policy "
    )

    policy
  }
}
