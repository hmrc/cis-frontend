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
import models.NormalMode
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn}
import models.amend.AreYouSureYouWantToAmendYesNo.No
import models.amend.WhatDoYouWantToAmendNil
import models.amend.WhatDoYouWantToAmendNil.{AddPaymentOrSubcontractorDetails, AmendNilReturn}
import models.monthlyreturns.UpdateMonthlyReturnRequest
import pages.amend.{AreYouSureYouWantToAmendYesNoPage, WhatDoYouWantToAmendNilPage}
import pages.monthlyreturns.ReturnTypePage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.WhatDoYouWantToAmendNilView
import utils.TypeUtils.toFuture
import utils.UserAnswerUtils.*

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
  formProvider: WhatDoYouWantToAmendNilFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: WhatDoYouWantToAmendNilView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

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
                        case Left(_)  =>
                          Future.successful(
                            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                          )
                        case Right(_) =>
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
                      ua3           <- ua1.clearMonthlyStandardReturnJourney.toFuture
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
}
