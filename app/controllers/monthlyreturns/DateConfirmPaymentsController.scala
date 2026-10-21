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
import models.ReturnType.MonthlyStandardReturn
import models.monthlyreturns.MonthlyReturnRequest
import models.requests.CisPath
import models.{Mode, ReturnType}
import navigation.Navigator
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, ReturnTypePage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TypeUtils.toFuture
import views.html.monthlyreturns.DateConfirmPaymentsView

import java.time.{Instant, LocalDateTime, ZoneId, ZonedDateTime}
import java.util.UUID
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
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  formProvider: DateConfirmPaymentsFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: DateConfirmPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode, returnType: Option[ReturnType] = None): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val userAnswers = request.userAnswers
      val form        = formProvider()

      // TODO: remove when timezone debugging is done
      logger.info(
        s"[DateConfirmPaymentsController] Current Server time: ${LocalDateTime.now} " +
          s"(${ZoneId.systemDefault}, ${ZonedDateTime.now().getOffset}) " +
          s"(${Instant.now()} in UTC)"
      )
      for {
        uaWithReturnType <-
          returnType.fold(Future.successful(userAnswers))(r => userAnswers.set(ReturnTypePage, r).toFuture)
        _                <- sessionRepository.set(uaWithReturnType)
      } yield uaWithReturnType
        .get(ReturnTypePage)
        .fold {
          logger.error("[DateConfirmPaymentsController][onPageLoad] - ReturnTypePage missing from user answers")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        } { storedReturnType =>
          val messagePrefix =
            if (storedReturnType == MonthlyStandardReturn) {
              "monthlyreturns.dateConfirmPayments"
            } else {
              "monthlyreturns.dateConfirmPayments.nilreturn"
            }

          val preparedForm = uaWithReturnType.get(DateConfirmPaymentsPage) match {
            case None        => form
            case Some(value) => form.fill(value)
          }

          Ok(
            view(
              preparedForm,
              mode,
              messagePrefix,
              storedReturnType,
              routes.DateConfirmPaymentsController.onSubmit(mode, storedReturnType)
            )
          )
        }
    }

  def onSubmit(mode: Mode, returnType: ReturnType): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val userAnswers   = request.userAnswers
      val form          = formProvider()
      val isStandard    = returnType == MonthlyStandardReturn
      val messagePrefix =
        if (isStandard) {
          "monthlyreturns.dateConfirmPayments"
        } else {
          "monthlyreturns.dateConfirmPayments.nilreturn"
        }
      val submitAction  = routes.DateConfirmPaymentsController.onSubmit(mode, returnType)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(BadRequest(view(formWithErrors, mode, messagePrefix, returnType, submitAction))),
          value => {
            val year  = value.getYear
            val month = value.getMonthValue

            (for {
              uaWithReturnType <- userAnswers.set(ReturnTypePage, returnType).toFuture
              cisId            <- uaWithReturnType.get(CisIdPage).toFuture
              isDup            <- monthlyReturnService.isDuplicate(cisId, year, month)
              updatedAnswers   <- Future.fromTry(uaWithReturnType.set(DateConfirmPaymentsPage, value))
              _                <- sessionRepository.set(updatedAnswers)
              result           <- if (isDup) {
                                    val dupForm =
                                      form
                                        .fill(value)
                                        .withError("value", "monthlyreturns.dateConfirmPayments.error.duplicate")
                                    Future.successful(BadRequest(view(dupForm, mode, messagePrefix, returnType, submitAction)))
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
            } yield result).recover { case NonFatal(ex) =>
              logger.error(
                s"[DateConfirmPaymentsController] Failed to process submission (CisIdPage, duplicate check, " +
                  s"create or save), returnType: $returnType: ${ex.getMessage}",
                ex
              )
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
          }
        )
    }

  // DTR-9431: Retrieve the UUID-scoped journey and its stored return type.
  def onPageLoadWithJourneyId(cisPath: CisPath, uuid: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)) { implicit request =>
      request.userAnswers.get(ReturnTypePage) match {
        case Some(returnType) =>
          val messagePrefix =
            if returnType == MonthlyStandardReturn then "monthlyreturns.dateConfirmPayments"
            else "monthlyreturns.dateConfirmPayments.nilreturn"

          val form         = formProvider()
          val preparedForm = request.userAnswers.get(DateConfirmPaymentsPage).fold(form)(form.fill)
          val submitAction = routes.DateConfirmPaymentsController.onSubmitWithJourneyId(cisPath, uuid, mode)

          Ok(view(preparedForm, mode, messagePrefix, returnType, submitAction))

        case None =>
          logger.error("[DateConfirmPaymentsController] ReturnTypePage missing from journey")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
    }

  // DTR-9431: Process period selection using the UUID-scoped journey.
  def onSubmitWithJourneyId(cisPath: CisPath, uuid: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(uuid)).async { implicit request =>
      request.userAnswers.get(ReturnTypePage) match {
        case Some(returnType) =>
          val form          = formProvider()
          val isStandard    = returnType == MonthlyStandardReturn
          val messagePrefix =
            if isStandard then "monthlyreturns.dateConfirmPayments"
            else "monthlyreturns.dateConfirmPayments.nilreturn"

          val submitAction = routes.DateConfirmPaymentsController.onSubmitWithJourneyId(cisPath, uuid, mode)

          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, messagePrefix, returnType, submitAction))),
              value =>
                (for {
                  isDup  <- monthlyReturnService.isDuplicate(request.cisId, value.getYear, value.getMonthValue)
                  result <- if (isDup) {
                              val dupForm = form
                                .fill(value)
                                .withError("value", "monthlyreturns.dateConfirmPayments.error.duplicate")

                              Future
                                .successful(BadRequest(view(dupForm, mode, messagePrefix, returnType, submitAction)))
                            } else {
                              for {
                                updatedAnswers <-
                                  Future.fromTry(request.userAnswers.set(DateConfirmPaymentsPage, value))
                                _              <- sessionRepository.set(updatedAnswers)
                                result         <- if (isStandard) {
                                                    monthlyReturnService
                                                      .createMonthlyReturn(
                                                        MonthlyReturnRequest(request.cisId, value.getYear, value.getMonthValue)
                                                      )
                                                      .map(_ =>
                                                        Redirect(
                                                          navigator.nextPage(
                                                            cisPath,
                                                            uuid,
                                                            DateConfirmPaymentsPage,
                                                            mode,
                                                            updatedAnswers
                                                          )
                                                        )
                                                      )
                                                  } else {
                                                    monthlyReturnService
                                                      .createNilMonthlyReturn(request.cisId, updatedAnswers)
                                                      .map(ua =>
                                                        Redirect(
                                                          navigator.nextPage(
                                                            cisPath,
                                                            uuid,
                                                            DateConfirmPaymentsPage,
                                                            mode,
                                                            ua
                                                          )
                                                        )
                                                      )
                                                  }
                              } yield result
                            }
                } yield result).recover { case NonFatal(ex) =>
                  logger.error("[DateConfirmPaymentsController] Failed to process scoped return", ex)
                  Redirect(controllers.routes.SystemErrorController.onPageLoad())
                }
            )

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }
}
