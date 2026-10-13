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

import com.google.inject.{ImplementedBy, Inject, Singleton}
import config.FrontendAppConfig
import models.requests.CisPath.*
import models.requests.{CisPath, IdentifierRequest, SchemeRequest}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{ActionRefiner, Result, Results}
import services.CisTaxpayerService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import views.html.PageNotFoundView

import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[SchemeActionImpl])
abstract class SchemeAction {
  def apply(cisPath: CisPath): ActionRefiner[IdentifierRequest, SchemeRequest]
}

@Singleton
final class SchemeActionImpl @Inject() (
  val messagesApi: MessagesApi,
  config: FrontendAppConfig,
  cisTaxpayerService: CisTaxpayerService,
  notFoundView: PageNotFoundView
)(using ec: ExecutionContext)
    extends SchemeAction
    with Results
    with Logging
    with I18nSupport {

  def apply(cisPath: CisPath): ActionRefiner[IdentifierRequest, SchemeRequest] =
    new ActionRefiner with FrontendHeaderCarrierProvider {
      val executionContext: ExecutionContext = ec

      def refine[B](request: IdentifierRequest[B]): Future[Either[Result, SchemeRequest[B]]] =
        given IdentifierRequest[B] = request
        if request.isAgent then resolveSchemeForAgent else resolveSchemeForOrganisation

      private def resolveSchemeForAgent[B](using request: IdentifierRequest[B]) =
        cisPath match
          case CisOrg =>
            logger.info(s"${request.user} tried to access ${request.uri}; redirect to Agent landing page.")
            Future.successful(Left(Redirect(config.constructionIndustryAgentAccountUrl)))

          case CisId(cisId) =>
            cisTaxpayerService
              .findForAgent(cisId)
              .map {
                case Some(cisTaxpayer) => Right(new SchemeRequest(request, cisTaxpayer))
                case None              =>
                  logger.info(s"Could not find scheme <$cisId> for ${request.user}.")
                  Left(NotFound(notFoundView()))
              }
              .recover { ex =>
                logger.error(s"Failed to find scheme <$cisId> for ${request.user}:", ex)
                Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }

      private def resolveSchemeForOrganisation[B](using request: IdentifierRequest[B]) =
        cisPath match
          case CisOrg =>
            cisTaxpayerService.findForOrganisation
              .map(cisTaxPayer => Right(new SchemeRequest(request, cisTaxPayer)))

          case CisId(cisId) =>
            logger.info(s"${request.user} tried to access ${request.uri}; redirect to Org URI.")
            val orgUri = request.uri.replace(s"/$cisId/", s"/${CisOrg.toUrl}/")
            Future.successful(Left(Redirect(orgUri)))
    }
}
