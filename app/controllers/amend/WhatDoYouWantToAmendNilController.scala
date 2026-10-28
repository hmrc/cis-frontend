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

package controllers.amend

import controllers.actions.*
import forms.amend.WhatDoYouWantToAmendNilFormProvider
import models.requests.CisPath
import models.NormalMode
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn}
import models.amend.WhatDoYouWantToAmendNil
import models.amend.WhatDoYouWantToAmendNil.{AddPaymentOrSubcontractorDetails, AmendNilReturn}
import models.monthlyreturns.UpdateMonthlyReturnRequest
import pages.amend.WhatDoYouWantToAmendNilPage
import pages.monthlyreturns.ReturnTypePage
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.TypeUtils.toFuture
import utils.UserAnswerUtils.*
import views.html.amend.WhatDoYouWantToAmendNilView

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToAmendNilController @Inject() (
  override val messagesApi: MessagesApi,
  monthlyReturnService: MonthlyReturnService,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  formProvider: WhatDoYouWantToAmendNilFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: WhatDoYouWantToAmendNilView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[WhatDoYouWantToAmendNil] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId) {
    implicit request =>

      val preparedForm = request.userAnswers.get(WhatDoYouWantToAmendNilPage) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          value =>
            for {
              ua1    <- Future.fromTry(request.userAnswers.set(WhatDoYouWantToAmendNilPage, value))
              _      <- sessionRepository.set(ua1)
              result <-
                value match {
                  case AddPaymentOrSubcontractorDetails =>
                    amendMonthlyReturnService
                      .startStandardAmendment(ua1)
                      .flatMap {
                        case Left(error) =>
                          logger.error(
                            s"[WhatDoYouWantToAmendNil] Failed to start nil amendment: $error"
                          )
                          Future.successful(
                            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                          )
                        case Right(_)    =>
                          for {
                            ua2 <- Future.fromTry(
                                     ua1.set(ReturnTypePage, MonthlyAmendedStandardReturn)
                                   )
                            _   <- sessionRepository.set(ua2)
                          } yield Redirect(
                            controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode)
                          )
                      }

                  case AmendNilReturn =>
                    for {
                      ua3           <- ua1.clearAmendedMonthlyStandardReturnJourney.toFuture
                      ua4           <- Future.fromTry(
                                         ua3.set(ReturnTypePage, MonthlyAmendedNilReturn)
                                       )
                      _             <- sessionRepository.set(ua4)
                      updateRequest <- UpdateMonthlyReturnRequest
                                         .fromUserAnswers(ua4)
                                         .fold(
                                           error => Future.failed(new RuntimeException(error)),
                                           request => Future.successful(request)
                                         )
                      _             <- monthlyReturnService.updateMonthlyReturn(updateRequest)
                    } yield Redirect(
                      controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoad(NormalMode)
                    )
                }
            } yield result
        )
  }

  def onPageLoadWithId(cisPath: CisPath, flowId: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)) { implicit request =>
      val preparedForm = request.userAnswers.get(WhatDoYouWantToAmendNilPage(request.subJourneyId)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      val formAction = Some(routes.WhatDoYouWantToAmendNilController.onSubmitWithId(cisPath, flowId))

      Ok(view(preparedForm, formAction))
    }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
      val formAction = Some(routes.WhatDoYouWantToAmendNilController.onSubmitWithId(cisPath, flowId))

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, formAction))),
          value =>
            for {
              ua1    <- Future.fromTry(request.userAnswers.set(WhatDoYouWantToAmendNilPage(request.subJourneyId), value))
              _      <- sessionRepository.set(ua1)
              result <-
                value match {
                  case AddPaymentOrSubcontractorDetails =>
                    amendMonthlyReturnService
                      .startStandardAmendment(ua1)
                      .flatMap {
                        case Left(error) =>
                          logger.error(
                            s"[WhatDoYouWantToAmendNil] Failed to start nil amendment: $error"
                          )
                          Future.successful(
                            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                          )
                        case Right(_)    =>
                          for {
                            ua2 <- Future.fromTry(
                                     ua1.set(ReturnTypePage(request.subJourneyId), MonthlyAmendedStandardReturn)
                                   )
                            _   <- sessionRepository.set(ua2)
                          } yield Redirect(
                            // TODO: Temp Redirect
                            controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(cisPath, flowId)
                            // controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoadWithId(cisPath,
                            // flowId,
                            // NormalMode)
                          )
                      }

                  case AmendNilReturn =>
                    for {
                      ua3           <- ua1.clearAmendedMonthlyStandardReturnJourney.toFuture
                      ua4           <- Future.fromTry(
                                         ua3.set(ReturnTypePage(request.subJourneyId), MonthlyAmendedNilReturn)
                                       )
                      _             <- sessionRepository.set(ua4)
                      updateRequest <- UpdateMonthlyReturnRequest
                                         .fromUserAnswers(ua4)
                                         .fold(
                                           error => Future.failed(new RuntimeException(error)),
                                           request => Future.successful(request)
                                         )
                      _             <- monthlyReturnService.updateMonthlyReturn(updateRequest)
                    } yield Redirect(
                      // TODO: Temp Redirect
                      controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(cisPath, flowId)
                      // controllers.monthlyreturns.routes.SubmitInactivityRequestController.onPageLoadWithId(cisPath,
                      // flowId,
                      // NormalMode)
                    )
                }
            } yield result
        )
    }

}
