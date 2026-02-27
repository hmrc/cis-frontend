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
import forms.monthlyreturns.DateConfirmNilPaymentsFormProvider
import models.agent.AgentClientData
import models.requests.OptionalDataRequest
import models.{Mode, ReturnType, UserAnswers}
import navigation.Navigator
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, ReturnTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.DateConfirmNilPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class DateConfirmNilPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: DateConfirmNilPaymentsFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: DateConfirmNilPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>

      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val ua0 = request.userAnswers.getOrElse(UserAnswers(request.userId))

      val form = formProvider()

      (for {
        uaWithReturnType <- Future.fromTry(ua0.set(ReturnTypePage, ReturnType.MonthlyNilReturn))
        _                <- sessionRepository.set(uaWithReturnType)
        ua1              <- prepareUserAnswers(uaWithReturnType, request)
        preparedForm      = ua1.get(DateConfirmNilPaymentsPage) match {
                              case None        => form
                              case Some(value) => form.fill(value)
                            }
        _                <- monthlyReturnService.resolveAndStoreCisId(ua1, request.isAgent)
      } yield Ok(view(preparedForm, mode))).recover {
        case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case NonFatal(ex)                                          =>
          logger.error(s"[DateConfirmNilPaymentsController] Failed to retrieve cisId: ${ex.getMessage}", ex)
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>

    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val form = formProvider()

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value => {
          val year  = value.getYear
          val month = value.getMonthValue

          val ua0 = request.userAnswers.getOrElse(UserAnswers(request.userId))

          (for {
            ua1       <- prepareUserAnswers(ua0, request)
            resolved  <- monthlyReturnService.resolveAndStoreCisId(ua1, request.isAgent)
            (cisId, _) = resolved
            isDup     <- monthlyReturnService.isDuplicate(cisId, year, month)
            result    <-
              if (isDup) {
                val dupForm =
                  form
                    .fill(value)
                    .withError("value", "monthlyreturns.dateConfirmNilPayments.error.duplicate")
                Future.successful(BadRequest(view(dupForm, mode)))
              } else {
                for {
                  updatedAnswers <- Future.fromTry(ua1.set(DateConfirmNilPaymentsPage, value))
                  _              <- sessionRepository.set(updatedAnswers)
                  withStatus     <- monthlyReturnService.createNilMonthlyReturn(updatedAnswers)
                } yield Redirect(navigator.nextPage(DateConfirmNilPaymentsPage, mode, withStatus))
              }
          } yield result).recover {
            case e: UpstreamErrorResponse if e.statusCode == NOT_FOUND =>
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case NonFatal(ex)                                          =>
              logger
                .error(s"[DateConfirmNilPaymentsController] Duplicate check fails unexpectedly: ${ex.getMessage}", ex)
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
          }
        }
      )
  }

  private def prepareUserAnswers(ua: UserAnswers, request: OptionalDataRequest[_])(implicit
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
