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

import connectors.ConstructionIndustrySchemeConnector
import models.finalvalidation.FinalValidationHandoffPayload
import models.handoff.JourneyHandoffTypes

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}
import play.api.Logging
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import controllers.actions.*
import pages.finalvalidations.FinalValidationDraftIdPage
import pages.monthlyreturns.CisIdPage

import java.util.UUID
import scala.util.control.NonFatal

@Singleton
class FinalValidationReturnController @Inject() (
  connector: ConstructionIndustrySchemeConnector,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requiredCisId: CisIdRequiredAction,
  multiTabAction: MultiTabAction,
  val controllerComponents: MessagesControllerComponents
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with Logging {

  def onPageLoad(handoffId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requiredCisId).async { implicit request =>
      connector
        .getJourneyHandoff[FinalValidationHandoffPayload](JourneyHandoffTypes.FinalValidation, handoffId)
        .flatMap {

          case Some(payload) =>
            val draftIdOpt = request.userAnswers.get(FinalValidationDraftIdPage)

            val validHandoff =
              payload.instanceId ==
                request.cisId && draftIdOpt.contains(payload.draftId)

            if (validHandoff) {
              connector
                .deleteJourneyHandoff(JourneyHandoffTypes.FinalValidation, handoffId)
                .map { _ =>
                  Redirect(routes.UpdateSubcontractorDetailsController.onPageLoad(payload.subcontractorId))
                }

            } else {
              logger.error(
                s"[FinalValidationReturnController] Invalid Final Validation handoff correlation " +
                  s"for handoffId: $handoffId"
              )
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }

          case None =>
            logger.error(
              s"[FinalValidationReturnController] Final Validation handoff not found " +
                s"for handoffId: $handoffId"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        }
        .recover { case NonFatal(ex) =>
          logger.error(s"[FinalValidationReturnController] Error processing handoff data for handoffId: $handoffId", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  // Multi-tab
  def onPageLoadWithId(flowId: UUID, handoffId: String): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val subJourneyId = flowId.toString
      val cisId        = request.userAnswers.get(CisIdPage(subJourneyId)).getOrElse {
        throw new IllegalStateException("CisId missing after multitabAction validation")
      }

      connector
        .getJourneyHandoff[FinalValidationHandoffPayload](JourneyHandoffTypes.FinalValidation, handoffId)
        .flatMap {

          case Some(payload) =>
            val draftIdOpt = request.userAnswers.get(FinalValidationDraftIdPage(subJourneyId))

            val validHandoff =
              payload.instanceId == cisId &&
                draftIdOpt.contains(payload.draftId) &&
                payload.flowId.contains(subJourneyId)

            if (validHandoff) {
              connector
                .deleteJourneyHandoff(JourneyHandoffTypes.FinalValidation, handoffId)
                .map(_ =>
                  Redirect(
                    routes.UpdateSubcontractorDetailsController.onPageLoadWithId(flowId, payload.subcontractorId)
                  )
                )

            } else {
              logger.error(
                s"[FinalValidationReturnController][onPageLoadWithId] Invalid Final Validation handoff correlation " +
                  s"for handoffId: $handoffId"
              )
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }

          case None =>
            logger.error(
              s"[FinalValidationReturnController][onPageLoadWithId] Final Validation handoff not found " +
                s"for handoffId: $handoffId"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        }
        .recover { case NonFatal(ex) =>
          logger.error(
            s"[FinalValidationReturnController][onPageLoadWithId] Error processing handoff data for handoffId: $handoffId",
            ex
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }
}
