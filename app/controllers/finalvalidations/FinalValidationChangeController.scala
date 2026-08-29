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

package controllers.finalvalidations

import config.FrontendAppConfig
import connectors.ConstructionIndustrySchemeConnector
import controllers.actions.{CisIdRequiredAction, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.finalvalidation.{FinalValidationChangeTarget, FinalValidationHandoffPayload}
import models.handoff.JourneyHandoffTypes.FinalValidation
import models.finalvalidation.FinalValidationHandoffPayload.given
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.finalvalidations.FinalValidationErrorPage
import play.api.Logging
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinalValidationChangeController @Inject() (
  connector: ConstructionIndustrySchemeConnector,
  appConfig: FrontendAppConfig,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with Logging {

  def onPageLoad(subcontractorId: Long, fieldKey: String, targetKey: String): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      val payloadOpt =
        for {
          failure           <- request.userAnswers
                                 .get(FinalValidationErrorPage)
                                 .flatMap(_.find(_.subcontractorId == subcontractorId))
          issue             <- failure.issues.find(_.field.key == fieldKey)
          target            <- FinalValidationChangeTarget.fromKey(targetKey)
          subbieResourceRef <- failure.subbieResourceRef
        } yield FinalValidationHandoffPayload(
          instanceId = request.cisId,
          subcontractorId = subcontractorId,
          subbieResourceRef = subbieResourceRef,
          field = issue.field,
          changeTarget = target
        )

      payloadOpt match {
        case Some(payload) =>
          connector
            .createJourneyHandoff(FinalValidation, Json.toJsObject[FinalValidationHandoffPayload](payload))
            .map { handoffId =>
              Redirect(appConfig.cisContractorFinalValidationHandoffUrl(handoffId))
            }
        case None          =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      }
    }
}
