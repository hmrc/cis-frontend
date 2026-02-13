/*
 * Copyright 2025 HM Revenue & Customs
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

import controllers.actions.{DataRetrievalAction, IdentifierAction}
import models.UserAnswers
import models.agent.AgentClientData
import pages.monthlyreturns.CisIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.FileYourMonthlyCisReturnView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class FileYourMonthlyCisReturnController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  view: FileYourMonthlyCisReturnView,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData).async { implicit request =>

    val instanceIdOpt = request.getQueryString("instanceId")

    def storeInstanceId(instanceId: String): Future[Result] = {
      val ua0 = request.userAnswers.getOrElse(UserAnswers(request.userId))
      for {
        updated <- Future.fromTry(ua0.set(CisIdPage, instanceId))
        _       <- sessionRepository.set(updated)
      } yield Ok(view())
    }

    (request.isAgent, instanceIdOpt) match {
      case (true, Some(instanceId)) =>
        monthlyReturnService.getAgentClient(request.userId).flatMap {
          case Some(data) =>
            monthlyReturnService
              .hasClient(data.taxOfficeNumber, data.taxOfficeReference)
              .flatMap {
                case true  => storeInstanceId(instanceId)
                case false =>
                  logger.warn(
                    s"[FileYourMonthlyCisReturnController] hasClient = false for " +
                      s"taxOfficeNumber: $data.taxOfficeNumber, taxOfficeReference: $data.taxOfficeReference"
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
              .recover { case NonFatal(e) =>
                logger.error(s"[FileYourMonthlyCisReturnController] hasClient check failed ${e.getMessage}", e)
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }

          case _ =>
            logger.warn(
              s"[FileYourMonthlyCisReturnController] No agent client found or JSON invalid"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }

      case (false, Some(instanceId)) =>
        storeInstanceId(instanceId)

      case (false, None) =>
        Future.successful(Ok(view()))

      case (true, None) =>
        logger.warn(s"[FileYourMonthlyCisReturnController] Missing instanceId for agent request")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }
  }
}
