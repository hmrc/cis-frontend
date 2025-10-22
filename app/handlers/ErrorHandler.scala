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

package handlers

import config.FrontendAppConfig
import play.api.Logging
import utils.ReferenceGenerator
import javax.inject.{Inject, Singleton}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.RequestHeader
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import views.html.{ErrorTemplate, PageNotFoundView, SystemErrorView}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ErrorHandler @Inject() (
  val messagesApi: MessagesApi,
  view: ErrorTemplate,
  notFoundView: PageNotFoundView,
  systemErrorView: SystemErrorView,
  referenceGenerator: ReferenceGenerator
)(implicit val ec: ExecutionContext, appConfig: FrontendAppConfig)
    extends FrontendErrorHandler
    with I18nSupport
    with Logging {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: RequestHeader
  ): Future[Html] =
    Future.successful(view(pageTitle, heading, message))

  override def notFoundTemplate(implicit request: RequestHeader): Future[Html] =
    Future.successful(notFoundView())

  override def internalServerErrorTemplate(implicit request: RequestHeader): Future[Html] = {
    val referenceNumber = referenceGenerator.generateReference()
    logger.error(s"CIS internal server error. Reference number: $referenceNumber")
    Future.successful(systemErrorView(referenceNumber))
  }
}
