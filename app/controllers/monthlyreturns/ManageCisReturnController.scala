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

package controllers.monthlyreturns

import config.FrontendAppConfig
import controllers.actions.*
import pages.monthlyreturns.{CisIdPage, ContractorNamePage}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ManageCisReturnController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  multiTabAction: MultiTabAction,
  monthlyReturnService: MonthlyReturnService,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onExit: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val redirectUrl = appConfig.returnsLandingPageUrl(
        request.cisId,
        request.userAnswers.get(ContractorNamePage)
      )

      monthlyReturnService
        .clearSubmissionJourney(request.userAnswers)
        .map(_ => Redirect(redirectUrl))
        .recover { case NonFatal(exception) =>
          logger.error(
            "[ManageCisReturnController] Failed to clear monthly return journey",
            exception
          )
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  // Multi-tab: Exit and clear the ID-scoped monthly return journey.
  def onExitWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val id = flowId.toString

      val cisId = request.userAnswers.get(CisIdPage(id)).getOrElse {
        throw new IllegalStateException("CisIdPage missing from ID-scoped userAnswers")
      }

      val redirectUrl = appConfig.returnsLandingPageUrl(
        cisId,
        request.userAnswers.get(ContractorNamePage(id))
      )

      monthlyReturnService
        .clearSubmissionJourney(request.userAnswers, id)
        .map(_ => Redirect(redirectUrl))
        .recover { case NonFatal(ex) =>
          logger.error("[ManageCisReturnController] Failed to clear multi-tab monthly return journey", ex)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }
}
