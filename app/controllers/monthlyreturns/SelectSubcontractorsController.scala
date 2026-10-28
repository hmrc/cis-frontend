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

import config.FrontendAppConfig
import controllers.actions.*
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.NormalMode
import models.requests.CisPath
import models.finalvalidation.MonthlyFinalValidationSource
import models.monthlyreturns.SelectSubcontractorsFormData
import pages.finalvalidations.{FinalValidationDraftIdPage, MonthlyFinalValidationContinuationPage, MonthlyFinalValidationSourcePage}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, IsResumedMonthlyReturnPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswerUtils.*
import views.html.monthlyreturns.SelectSubcontractorsView

import javax.inject.Inject
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class SelectSubcontractorsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  val controllerComponents: MessagesControllerComponents,
  view: SelectSubcontractorsView,
  formProvider: SelectSubcontractorsFormProvider,
  subcontractorService: SubcontractorService,
  monthlyReturnService: MonthlyReturnService,
  sessionRepository: SessionRepository,
  appConfig: FrontendAppConfig
)(using ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  private val form = formProvider()

  def onPageLoad(defaultSelection: Option[Boolean] = None): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      val requiredAnswers = for {
        cisId   <- request.userAnswers.get(CisIdPage)
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

      requiredAnswers
        .map { (cisId, taxMonth, taxYear) =>
          subcontractorService
            .buildSelectSubcontractorPage(cisId, taxMonth, taxYear, defaultSelection, Some(request.userAnswers))
            .map { model =>

              val filledForm =
                if (model.initiallySelectedIds.nonEmpty) {
                  form.fill(
                    SelectSubcontractorsFormData(
                      subcontractorsToInclude = model.initiallySelectedIds
                    )
                  )
                } else {
                  form
                }

              Ok(view(filledForm, model.subcontractors, appConfig.yourSubcontractorsUrl))
            }
        }
        .getOrElse {
          logger.error("[SelectSubcontractorsController][onPageLoad] - CisIdPage or DateConfirmPaymentsPage missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def onSubmit(): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val requiredAnswers = for {
        cisId   <- request.userAnswers.get(CisIdPage)
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear)
      requiredAnswers
        .map { (cisId, taxMonth, taxYear) =>
          subcontractorService
            .buildSelectSubcontractorPage(cisId, taxMonth, taxYear, None, Some(request.userAnswers))
            .flatMap { model =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(view(formWithErrors, model.subcontractors, appConfig.yourSubcontractorsUrl))
                    ),
                  formData => {
                    val selectedSubcontractors =
                      model.subcontractors.filter(x => formData.subcontractorsToInclude.contains(x.id))

                    monthlyReturnService
                      .storeAndSyncSelectedSubcontractors(
                        ua = request.userAnswers,
                        selected = selectedSubcontractors,
                        originalSubcontractorCount = model.subcontractors.size
                      )
                      .flatMap { updatedAnswers =>
                        val verificationRequired =
                          selectedSubcontractors
                            .filter { subcontractor =>
                              updatedAnswers.incompleteSubcontractorIds
                                .contains(subcontractor.id)
                            }
                            .exists(_.verificationRequired)

                        for {
                          withoutDraft        <- Future.fromTry(updatedAnswers.remove(FinalValidationDraftIdPage))
                          withoutContinuation <- Future.fromTry(
                                                   withoutDraft.remove(MonthlyFinalValidationContinuationPage)
                                                 )
                          withSource          <- Future.fromTry(
                                                   withoutContinuation.set(
                                                     MonthlyFinalValidationSourcePage,
                                                     MonthlyFinalValidationSource.SelectSubcontractors
                                                   )
                                                 )
                          withOrigin          <- Future.fromTry(
                                                   withSource.set(
                                                     IsResumedMonthlyReturnPage,
                                                     withSource.get(IsResumedMonthlyReturnPage).getOrElse(false)
                                                   )
                                                 )
                          _                   <- sessionRepository.set(withOrigin)
                        } yield
                          if (verificationRequired) {
                            Redirect(routes.VerifySubcontractorsController.onPageLoad(NormalMode))
                          } else {
                            Redirect(routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))
                          }
                      }
                      .recover { error =>
                        logger.error(
                          s"[SelectSubcontractorsController] Failed storing/syncing selected subcontractors: ${error.toString}",
                          error
                        )
                        Redirect(controllers.routes.SystemErrorController.onPageLoad())
                      }
                  }
                )
            }
        }
        .getOrElse {
          logger.error("[SelectSubcontractorsController][onSubmit] - CisIdPage or DateConfirmPaymentsPage missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def onPageLoadWithId(cisPath: CisPath, flowId: UUID, defaultSelection: Option[Boolean] = None): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>

      val requiredAnswers = for {
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage(request.subJourneyId))
      } yield (taxDate.getMonthValue, taxDate.getYear)

      requiredAnswers
        .map { (taxMonth, taxYear) =>
          subcontractorService
            .buildSelectSubcontractorPage(request.cisId, taxMonth, taxYear, defaultSelection, Some(request.userAnswers))
            .map { model =>

              val filledForm =
                if (model.initiallySelectedIds.nonEmpty) {
                  form.fill(
                    SelectSubcontractorsFormData(
                      subcontractorsToInclude = model.initiallySelectedIds
                    )
                  )
                } else {
                  form
                }

              val formAction   = Some(routes.SelectSubcontractorsController.onSubmitWithId(cisPath, flowId))
              val preSelectedAction   = Some(routes.SelectSubcontractorsController.onPageLoadWithId(cisPath, flowId, Some(true)))
              val noneSelectedAction   = Some(routes.SelectSubcontractorsController.onPageLoadWithId(cisPath, flowId, Some(false)))

              Ok(view(filledForm, model.subcontractors, appConfig.yourSubcontractorsUrl, formActions, preSelectedAction, noneSelectedAction))
            }
        }
        .getOrElse {
          logger.error("[SelectSubcontractorsController][onPageLoad] - CisIdPage or DateConfirmPaymentsPage missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val subJourneyId = request.subJourneyId
      val requiredAnswers = for {
        taxDate <- request.userAnswers.get(DateConfirmPaymentsPage(subJourneyId))
      } yield (taxDate.getMonthValue, taxDate.getYear)
      requiredAnswers
        .map { (taxMonth, taxYear) =>
          subcontractorService
            .buildSelectSubcontractorPage(request.cisId, taxMonth, taxYear, None, Some(request.userAnswers))
            .flatMap { model =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(view(formWithErrors, model.subcontractors, appConfig.yourSubcontractorsUrl))
                    ),
                  formData => {
                    val selectedSubcontractors =
                      model.subcontractors.filter(x => formData.subcontractorsToInclude.contains(x.id))

                    monthlyReturnService
                      .storeAndSyncSelectedSubcontractors(
                        ua = request.userAnswers,
                        selected = selectedSubcontractors,
                        originalSubcontractorCount = model.subcontractors.size
                      )
                      .flatMap { updatedAnswers =>
                        val verificationRequired =
                          selectedSubcontractors
                            .filter { subcontractor =>
                              updatedAnswers.incompleteSubcontractorIds
                                .contains(subcontractor.id)
                            }
                            .exists(_.verificationRequired)

                        for {
                          withoutDraft        <- Future.fromTry(updatedAnswers.remove(FinalValidationDraftIdPage(subJourneyId)))
                          withoutContinuation <- Future.fromTry(
                                                   withoutDraft.remove(MonthlyFinalValidationContinuationPage(subJourneyId))
                                                 )
                          withSource          <- Future.fromTry(
                                                   withoutContinuation.set(
                                                     MonthlyFinalValidationSourcePage(subJourneyId),
                                                     MonthlyFinalValidationSource.SelectSubcontractors
                                                   )
                                                 )
                          withOrigin          <- Future.fromTry(
                                                   withSource.set(
                                                     IsResumedMonthlyReturnPage(subJourneyId),
                                                     withSource.get(IsResumedMonthlyReturnPage(subJourneyId)).getOrElse(false)
                                                   )
                                                 )
                          _                   <- sessionRepository.set(withOrigin)
                        } yield
                          if (verificationRequired) {
                            Redirect(routes.VerifySubcontractorsController.onPageLoad(NormalMode))
                          } else {
                            Redirect(routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))
                          }
                      }
                      .recover { error =>
                        logger.error(
                          s"[SelectSubcontractorsController] Failed storing/syncing selected subcontractors: ${error.toString}",
                          error
                        )
                        Redirect(controllers.routes.SystemErrorController.onPageLoad())
                      }
                  }
                )
            }
        }
        .getOrElse {
          logger.error("[SelectSubcontractorsController][onSubmit] - CisIdPage or DateConfirmPaymentsPage missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}
