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

import config.FrontendAppConfig
import controllers.actions.{CisIdRequiredAction, DataRequiredAction, DataRetrievalAction, IdentifierAction}
import models.ReturnType
import models.ReturnType.MonthlyStandardReturn
import pages.monthlyreturns.ReturnTypePage
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.AlreadySubmittedView

import javax.inject.Inject

class AlreadySubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  view: AlreadySubmittedView
)(implicit appConfig: FrontendAppConfig)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>
      val returnType    = request.userAnswers.get(ReturnTypePage).getOrElse {
        logger.error("[AlreadySubmittedController] ReturnTypePage missing from userAnswers")
        throw new IllegalStateException("ReturnTypePage missing from userAnswers")
      }
      val messagePrefix = if (returnType == MonthlyStandardReturn) {
        "monthlyreturns.alreadySubmitted"
      } else {
        "monthlyreturns.alreadySubmitted.nilreturn"
      }
      Ok(view(messagePrefix))
  }
}
