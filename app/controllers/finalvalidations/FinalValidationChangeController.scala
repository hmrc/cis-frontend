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
import models.finalvalidation.{FinalValidationChangeTarget, FinalValidationField, FinalValidationHandoffPayload, FinalValidationReadiness}
import models.handoff.JourneyHandoffTypes.FinalValidation
import models.finalvalidation.FinalValidationHandoffPayload.given
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.finalvalidations.FinalValidationDraftIdPage
import play.api.Logging
import services.finalvalidation.FinalValidationDraftService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinalValidationChangeController @Inject() (
  connector: ConstructionIndustrySchemeConnector,
  finalValidationDraftService: FinalValidationDraftService,
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
      request.userAnswers.get(FinalValidationDraftIdPage) match {

        case Some(draftId) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .flatMap { draft =>
              val payloadOpt =
                for {
                  subcontractor <- draft
                                     .subcontractor(subcontractorId)
                                     .filter(
                                       _.readiness == FinalValidationReadiness.Incomplete
                                     )
                  issue         <- subcontractor.issues.find(_.fieldKey == fieldKey)
                  field         <- FinalValidationField.fromKey(issue.fieldKey)
                  target        <- FinalValidationChangeTarget.fromKey(targetKey)

                } yield FinalValidationHandoffPayload(
                  draftId = draftId,
                  instanceId = request.cisId,
                  subcontractorId = subcontractor.subcontractorId,
                  subbieResourceRef = subcontractor.subbieResourceRef,
                  field = field,
                  changeTarget = target
                )

              payloadOpt match {

                case Some(payload) =>
                  connector
                    .createJourneyHandoff(
                      FinalValidation,
                      Json.toJsObject[FinalValidationHandoffPayload](payload)
                    )
                    .map { handoffId =>
                      Redirect(appConfig.cisContractorFinalValidationHandoffUrl(handoffId))
                    }

                case None =>
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
            }

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
