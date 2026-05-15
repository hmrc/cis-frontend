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
import forms.amend.WhichSubcontractorsToAddFormProvider
import models.Mode
import models.amend.WhichSubcontractorsToAdd
import navigation.Navigator
import pages.amend.{AmendmentDetailsPage, WhichSubcontractorsToAddPage}
import pages.monthlyreturns.CisIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.WhichSubcontractorsToAddView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhichSubcontractorsToAddController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: WhichSubcontractorsToAddFormProvider,
  subcontractorService: SubcontractorService,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: WhichSubcontractorsToAddView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val requiredAnswers = for {
        cisId        <- request.userAnswers.get(CisIdPage)
        amendDetails <- request.userAnswers.get(AmendmentDetailsPage)
      } yield (cisId, amendDetails.taxMonth, amendDetails.taxYear)

      requiredAnswers
        .map { case (cisId, taxMonth, taxYear) =>
          subcontractorService
            .buildAmendWhichSubcontractorsPage(cisId, taxMonth, taxYear, Some(request.userAnswers))
            .map { model =>
              val form          = formProvider(model.subcontractors)
              val selectedIds   = request.userAnswers
                .get(WhichSubcontractorsToAddPage)
                .getOrElse(model.preSelectedIds)
              val checkboxItems = WhichSubcontractorsToAdd.checkboxItems(model.subcontractors, selectedIds)
              Ok(view(form, mode, checkboxItems))
            }
            .recover { case ex =>
              logger.error(s"[WhichSubcontractorsToAddController] Failed to load subcontractors: ${ex.getMessage}", ex)
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
        }
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val requiredAnswers = for {
        cisId        <- request.userAnswers.get(CisIdPage)
        amendDetails <- request.userAnswers.get(AmendmentDetailsPage)
      } yield (cisId, amendDetails.taxMonth, amendDetails.taxYear)

      requiredAnswers
        .map { case (cisId, taxMonth, taxYear) =>
          subcontractorService
            .buildAmendWhichSubcontractorsPage(cisId, taxMonth, taxYear, Some(request.userAnswers))
            .flatMap { model =>
              model.status match {
                case Some("STARTED") | Some("VALIDATED") =>
                  val form = formProvider(model.subcontractors)
                  form
                    .bindFromRequest()
                    .fold(
                      formWithErrors =>
                        Future.successful(
                          BadRequest(
                            view(formWithErrors, mode, WhichSubcontractorsToAdd.checkboxItems(model.subcontractors))
                          )
                        ),
                      value =>
                        for {
                          updatedAnswers <- Future.fromTry(request.userAnswers.set(WhichSubcontractorsToAddPage, value))
                          _              <- sessionRepository.set(updatedAnswers)
                          _              <- monthlyReturnService
                                              .syncMonthlyReturnItems(
                                                cisId,
                                                taxYear,
                                                taxMonth,
                                                value.toSeq.map(_.toLong),
                                                isAmendment = Some(true)
                                              )
                        } yield Redirect(navigator.nextPage(WhichSubcontractorsToAddPage, mode, updatedAnswers))
                    )
                case _                                   =>
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
            }
            .recover { case ex =>
              logger.error(s"[WhichSubcontractorsToAddController] Submit failed: ${ex.getMessage}", ex)
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
        }
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
  }
}
