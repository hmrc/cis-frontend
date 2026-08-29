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
import controllers.actions.{DataRequiredAction, DataRetrievalAction, IdentifierAction}
import scala.util.control.NonFatal

@Singleton
class FinalValidationReturnController @Inject() (
  connector: ConstructionIndustrySchemeConnector,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with Logging {

  def onPageLoad(handoffId: String): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      connector
        .getJourneyHandoff[FinalValidationHandoffPayload](JourneyHandoffTypes.FinalValidation, handoffId)
        .flatMap {

          case Some(payload) =>
            connector.deleteJourneyHandoff(JourneyHandoffTypes.FinalValidation, handoffId).map { _ =>
              Redirect(routes.UpdateSubcontractorDetailsController.onPageLoad(payload.subcontractorId))
            }

          case None =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        }
        .recover { case NonFatal(ex) =>
          logger.error(s"[FinalValidationReturnController] Error processing handoff data for handoffId: $handoffId", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
  }
}
