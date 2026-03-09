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

import controllers.actions.*
import forms.monthlyreturns.DateConfirmPaymentsFormProvider
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.agent.AgentClientData
import models.{Mode, ReturnType, UserAnswers}
import models.monthlyreturns.MonthlyReturnRequest
import models.requests.DataRequest
import navigation.Navigator
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, ReturnTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.TypeUtils.toFuture
import views.html.monthlyreturns.DateConfirmPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DateConfirmPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DateConfirmPaymentsFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: DateConfirmPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode, returnType: Option[ReturnType] = None): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val form                       = formProvider()

      (for {
        uaWithReturnType    <- returnType.fold(Future.successful(request.userAnswers))(r =>
                                 request.userAnswers.set(ReturnTypePage, r).toFuture
                               )
        _                   <- sessionRepository.set(uaWithReturnType)
        returnType          <- uaWithReturnType.get(ReturnTypePage).toFuture
        messagePrefix        = if (returnType == MonthlyStandardReturn) "monthlyreturns.dateConfirmPayments"
                               else "monthlyreturns.dateConfirmPayments.nilreturn"
        preparedUserAnswers <- prepareUserAnswers(uaWithReturnType, request)
        _                   <- monthlyReturnService.resolveAndStoreCisId(preparedUserAnswers, request.isAgent)
        preparedForm         = request.userAnswers.get(DateConfirmPaymentsPage) match {
                                 case None        => form
                                 case Some(value) => form.fill(value)
                               }
      } yield Ok(view(preparedForm, mode, messagePrefix))).recover {
        case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case NonFatal(ex)                                          =>
          logger.error(s"[DateConfirmPaymentsController] Failed to retrieve cisId: ${ex.getMessage}", ex)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val form          = formProvider()
      val isStandard    = request.userAnswers.get(ReturnTypePage).contains(MonthlyStandardReturn)
      val messagePrefix =
        if (isStandard)
          "monthlyreturns.dateConfirmPayments"
        else "monthlyreturns.dateConfirmPayments.nilreturn"
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, messagePrefix))),
          value => {
            val year  = value.getYear
            val month = value.getMonthValue

            (for {
              resolved            <- monthlyReturnService
                                       .resolveAndStoreCisId(request.userAnswers, request.isAgent)
              (cisId, uaWithCisId) = resolved
              isDup               <- monthlyReturnService.isDuplicate(cisId, year, month)
              updatedAnswers      <- Future.fromTry(uaWithCisId.set(DateConfirmPaymentsPage, value))
              _                   <- sessionRepository.set(updatedAnswers)
              result              <- if (isDup) {
                                       val dupForm =
                                         form
                                           .fill(value)
                                           .withError("value", "monthlyreturns.dateConfirmPayments.error.duplicate")
                                       Future.successful(BadRequest(view(dupForm, mode, messagePrefix)))
                                     } else if (isStandard) {
                                       val createRequest = MonthlyReturnRequest(cisId, year, month)
                                       monthlyReturnService
                                         .createMonthlyReturn(createRequest)
                                         .map { _ =>
                                           Redirect(navigator.nextPage(DateConfirmPaymentsPage, mode, updatedAnswers))
                                         }
                                     } else {
                                       for {
                                         uaWithStatus <- monthlyReturnService.createNilMonthlyReturn(updatedAnswers)
                                       } yield Redirect(navigator.nextPage(DateConfirmPaymentsPage, mode, uaWithStatus))
                                     }
            } yield result).recover { exception =>
              logger.error(
                s"[DateConfirmPaymentsController] duplicate check fails unexpectedly: ${exception.getMessage}",
                exception
              )
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
          }
        )
  }

  private def prepareUserAnswers(ua: UserAnswers, request: DataRequest[_])(implicit
    hc: HeaderCarrier
  ): Future[UserAnswers] =
    if request.isAgent then
      monthlyReturnService.getAgentClient(request.userId).flatMap {
        case Some(data) =>
          monthlyReturnService
            .hasClient(data.taxOfficeNumber, data.taxOfficeReference)
            .flatMap {
              case true  => storeAgentClientData(data, ua)
              case false => Future.failed(new RuntimeException("Agent has no access to this client"))
            }
        case _          => Future.failed(new RuntimeException("Missing agent client data"))
      }
    else Future.successful(ua)

  private def storeAgentClientData(data: AgentClientData, ua: UserAnswers): Future[UserAnswers] =
    for {
      updatedUaWithCisId           <- Future.fromTry(ua.set(CisIdPage, data.uniqueId))
      updatedUaWithAgentClientData <- Future.fromTry(updatedUaWithCisId.set(AgentClientDataPage, data))
      _                            <- sessionRepository.set(updatedUaWithAgentClientData)
    } yield updatedUaWithAgentClientData
}
