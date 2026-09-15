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
import pages.monthlyreturns.ContractorNamePage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class ManageCisReturnController @Inject() (
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
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
}
