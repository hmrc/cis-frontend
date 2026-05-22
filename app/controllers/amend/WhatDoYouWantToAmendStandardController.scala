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
import forms.amend.WhatDoYouWantToAmendStandardFormProvider

import javax.inject.Inject
import models.NormalMode
import models.amend.WhatDoYouWantToAmendStandard
import models.monthlyreturns.SelectedSubcontractor
import models.requests.GetMonthlyReturnForEditRequest
import pages.amend.WhatDoYouWantToAmendStandardPage
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.TypeUtils.toFuture
import utils.Utils.toBigDecimal
import views.html.amend.WhatDoYouWantToAmendStandardView

import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToAmendStandardController @Inject() (
  override val messagesApi: MessagesApi,
  monthlyReturnService: MonthlyReturnService,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: WhatDoYouWantToAmendStandardFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: WhatDoYouWantToAmendStandardView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val preparedForm = request.userAnswers.get(WhatDoYouWantToAmendStandardPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        value =>
          val requiredAnswers = for {
            cisId   <- request.userAnswers.get(CisIdPage)
            taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
          } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

          for {
            (cisId, month, year)     <- requiredAnswers.toFuture
            monthlyReturn            <- monthlyReturnService.retrieveMonthlyReturnForEditDetails(
                                          GetMonthlyReturnForEditRequest(
                                            cisId,
                                            taxMonth = month,
                                            taxYear = year,
                                            isAmendment = true
                                          )
                                        )
            itemsAndSubcontractors    = monthlyReturn.subcontractors
                                          .map { subcontractor =>
                                            val item = monthlyReturn.monthlyReturnItems
                                              .find(_.itemResourceReference == subcontractor.subbieResourceRef)
                                            (subcontractor, item)
                                          }
                                          .collect { case (subcontractor, Some(item)) =>
                                            (subcontractor, item)
                                          }
            preselectedSubcontractors = itemsAndSubcontractors
                                          .map((sub, item) =>
                                            SelectedSubcontractor(
                                              id = sub.subcontractorId,
                                              name = sub.displayName.getOrElse("No name provided"),
                                              totalPaymentsMade = item.totalPayments.flatMap(toBigDecimal),
                                              costOfMaterials = item.costOfMaterials.flatMap(toBigDecimal),
                                              totalTaxDeducted = item.totalDeducted.flatMap(toBigDecimal)
                                            )
                                          )
                                          .zipWithIndex
                                          .map(x => (x._2 + 1, x._1))
                                          .toMap

            ua1    <- request.userAnswers.set(WhatDoYouWantToAmendStandardPage, value).toFuture
            ua2    <- ua1.set(SelectedSubcontractorPage.all, preselectedSubcontractors).toFuture
            _      <- sessionRepository.set(ua2)
            result <-
              value match {
                case WhatDoYouWantToAmendStandard.AmendToNilReturn =>
                  Future
                    .successful(Redirect(controllers.amend.routes.AreYouSureYouWantToAmendYesNoController.onPageLoad()))

                case WhatDoYouWantToAmendStandard.AmendPaymentOrSubcontractorDetails =>
                  amendMonthlyReturnService.startStandardAmendment(ua2).map {
                    case Left(_) =>
                      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

                    case Right(_) =>
                      Redirect(
                        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
                      )
                  }
              }
          } yield result
      )
  }
}
