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
import models.monthlyreturns.SelectedSubcontractor
import models.requests.GetMonthlyReturnForEditRequest
import navigation.Navigator
import pages.amend.{AmendmentDetailsPage, WhichSubcontractorsToAddPage}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.WhichSubcontractorsToAddView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
      val ua = request.userAnswers

      val requiredAnswers = for {
        cisId   <- ua.get(CisIdPage)
        taxDate <- ua.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

      requiredAnswers
        .map { case (cisId, taxMonth, taxYear) =>
          val isAmendment = ua.get(AmendmentDetailsPage).isDefined

          monthlyReturnService
            .retrieveMonthlyReturnForEditDetails(
              GetMonthlyReturnForEditRequest(
                instanceId = cisId,
                taxMonth = taxMonth,
                taxYear = taxYear,
                isAmendment = isAmendment
              )
            )
            .flatMap { returns =>
              returns.monthlyReturn.headOption.flatMap(_.status) match {
                case Some("STARTED" | "VALIDATED") =>
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
                      logger.error(
                        s"[WhichSubcontractorsToAddController] Failed to load subcontractors: ${ex.getMessage}",
                        ex
                      )
                      Redirect(controllers.routes.SystemErrorController.onPageLoad())
                    }
                case _                             => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }

            }
        }
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val requiredAnswers = for {
        cisId   <- request.userAnswers.get(CisIdPage)
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

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
                        val selectedSubcontractors = model.subcontractors
                          .filter(subcontractor => value.contains(subcontractor.id))
                          .map { subcontractor =>
                            SelectedSubcontractor(
                              subcontractor.id.toLong,
                              subcontractor.name,
                              None,
                              None,
                              None
                            )
                          }
                        for {
                          ua  <- Future.fromTry(request.userAnswers.set(WhichSubcontractorsToAddPage, value))
                          ua2 <- Future.fromTry {
                                   val cleared = ua.remove(SelectedSubcontractorPage.all)
                                   cleared.flatMap { clearedAnswers =>
                                     selectedSubcontractors.zipWithIndex.foldLeft(Try(clearedAnswers)) {
                                       case (answersTry, (subcontractor, index)) =>
                                         answersTry.flatMap(
                                           _.set(SelectedSubcontractorPage(index + 1), subcontractor)
                                         )
                                     }
                                   }
                                 }
                          _   <- sessionRepository.set(ua2)
                          _   <- monthlyReturnService.syncMonthlyReturnItems(ua2, value.toSeq.map(_.toLong))
                        } yield Redirect(navigator.nextPage(WhichSubcontractorsToAddPage, mode, ua2))
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
