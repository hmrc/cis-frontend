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
import models.finalvalidation.{FinalValidationDraftRequestBuilder, MonthlyFinalValidationSource}
import models.monthlyreturns.SelectSubcontractorsFormData
import pages.finalvalidations.{FinalValidationDraftIdPage, FinalValidationVerificationRequiredPage, MonthlyFinalValidationSourcePage}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.finalvalidation.{FinalValidationDraftService, FinalValidationService}
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.UserAnswerUtils.*
import views.html.monthlyreturns.SelectSubcontractorsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SelectSubcontractorsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: SelectSubcontractorsView,
  formProvider: SelectSubcontractorsFormProvider,
  subcontractorService: SubcontractorService,
  monthlyReturnService: MonthlyReturnService,
  sessionRepository: SessionRepository,
  finalValidationService: FinalValidationService,
  finalValidationDraftService: FinalValidationDraftService,
  finalValidationDraftRequestBuilder: FinalValidationDraftRequestBuilder,
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
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
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

                    val selectedSubcontractorIds: Set[Long] =
                      formData.subcontractorsToInclude.map(_.toLong).toSet

                    val selectedFullSubcontractors =
                      model.fullSubcontractors.filter(sub => selectedSubcontractorIds.contains(sub.subcontractorId))

                    monthlyReturnService
                      .storeAndSyncSelectedSubcontractors(
                        ua = request.userAnswers,
                        selected = selectedSubcontractors
                      )
                      .flatMap { updatedAnswers =>
                        val validation =
                          finalValidationService.validate(
                            selectedSubcontractors = selectedFullSubcontractors,
                            allSubcontractors = model.fullSubcontractors
                          )

                        val verificationRequired =
                          selectedSubcontractors
                            .filter { subcontractor =>
                              updatedAnswers.incompleteSubcontractorIds
                                .contains(subcontractor.id)
                            }
                            .exists(_.verificationRequired == "Yes")

                        if (validation.hasErrors) {
                          for {
                            createRequest    <- Future.fromTry(
                                                  finalValidationDraftRequestBuilder.build(
                                                    instanceId = cisId,
                                                    selectedSubcontractors = selectedFullSubcontractors,
                                                    validation = validation
                                                  )
                                                )
                            draftId          <- finalValidationDraftService.create(createRequest)
                            withDraftId      <- Future.fromTry(
                                                  updatedAnswers.set(FinalValidationDraftIdPage, draftId)
                                                )
                            withSource       <- Future.fromTry(
                                                  withDraftId.set(
                                                    MonthlyFinalValidationSourcePage,
                                                    MonthlyFinalValidationSource.SelectSubcontractors
                                                  )
                                                )
                            withContinuation <- Future.fromTry(
                                                  withSource.set(
                                                    FinalValidationVerificationRequiredPage,
                                                    verificationRequired
                                                  )
                                                )
                            _                <- sessionRepository.set(withContinuation)
                          } yield Redirect(
                            controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onPageLoad()
                          )
                        } else if (verificationRequired) {
                          Future.successful(Redirect(routes.VerifySubcontractorsController.onPageLoad(NormalMode)))
                        } else {
                          Future.successful(Redirect(routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)))
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
        .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
    }
}
