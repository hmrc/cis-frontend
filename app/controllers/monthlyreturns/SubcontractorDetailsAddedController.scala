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

package controllers.monthlyreturns

import controllers.actions.*
import forms.monthlyreturns.SubcontractorDetailsAddedFormProvider
import models.requests.CisPath
import models.ReturnType.MonthlyStandardReturn
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import models.finalvalidation.{FinalValidationDraftRequestBuilder, MonthlyFinalValidationSource}
import pages.finalvalidations.{FinalValidationDraftIdPage, MonthlyFinalValidationContinuationPage, MonthlyFinalValidationSourcePage}
import pages.monthlyreturns.{AllSubcontractorDetailsAdded, CisIdPage, DateConfirmPaymentsPage, IsResumedMonthlyReturnPage, ReturnTypePage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import repositories.SessionRepository
import services.{MonthlyReturnService, SubcontractorService}
import services.finalvalidation.{FinalValidationDraftService, FinalValidationService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.SubcontractorDetailsAddedBuilder
import views.html.monthlyreturns.SubcontractorDetailsAddedView

import javax.inject.Inject
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class SubcontractorDetailsAddedController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  formProvider: SubcontractorDetailsAddedFormProvider,
  sessionRepository: SessionRepository,
  val controllerComponents: MessagesControllerComponents,
  view: SubcontractorDetailsAddedView,
  monthlyReturnService: MonthlyReturnService,
  subcontractorService: SubcontractorService,
  finalValidationService: FinalValidationService,
  finalValidationDraftService: FinalValidationDraftService,
  finalValidationDraftRequestBuilder: FinalValidationDraftRequestBuilder
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val ua = request.userAnswers

      val requiredAnswers = for {
        cisId      <- ua.get(CisIdPage)
        taxDate    <- ua.get(DateConfirmPaymentsPage)
        returnType <- ua.get(ReturnTypePage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers match {
        case Some((cisId, month, year, isAmendment)) =>
          monthlyReturnService.isEditable(cisId, month, year, isAmendment).flatMap {
            case true =>
              if (ua.get(FinalValidationDraftIdPage).isDefined) {
                Future.successful(
                  Redirect(controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onPageLoad())
                )
              } else if (ua.get(MonthlyFinalValidationContinuationPage).contains(mode.toString)) {
                Future.successful(renderPage(mode, ua))
              } else {
                ua.get(MonthlyFinalValidationSourcePage) match {
                  case Some(source) if sourceMode(source).contains(mode) =>
                    runFinalValidation(cisId, month, year, mode, ua)

                  case _ =>
                    logger.error(
                      "[SubcontractorDetailsAddedController][onPageLoad] - Missing or invalid MonthlyFinalValidationSourcePage"
                    )
                    Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                }
              }

            case false =>
              logger.error(
                s"[SubcontractorDetailsAddedController][onPageLoad] - isEditable returned false, cisId=$cisId"
              )
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }

        case None =>
          logger.error(
            "[SubcontractorDetailsAddedController][onPageLoad] - CisIdPage or DateConfirmPaymentsPage missing"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onPageLoadWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val ua           = request.userAnswers
      val cisId        = request.cisId
      val subJourneyId = request.subJourneyId

      val requiredAnswers = for {
        taxDate    <- ua.get(DateConfirmPaymentsPage(subJourneyId))
        returnType <- ua.get(ReturnTypePage(subJourneyId))
      } yield (taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers match {
        case Some((month, year, isAmendment)) =>
          monthlyReturnService.isEditable(cisId, month, year, isAmendment).flatMap {
            case true =>
              if (ua.get(FinalValidationDraftIdPage(subJourneyId)).isDefined) {
                Future.successful(
                  Redirect(
                    controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onPageLoad()
                  )
                )
              } else if (ua.get(MonthlyFinalValidationContinuationPage(subJourneyId)).contains(mode.toString)) {
                Future.successful(
                  renderPageWithId(subJourneyId, cisPath, flowId, mode, ua)
                )
              } else {
                ua.get(MonthlyFinalValidationSourcePage(subJourneyId)) match {
                  case Some(source) if sourceMode(source).contains(mode) =>
                    runFinalValidationWithId(subJourneyId, cisPath, flowId, cisId, month, year, mode, ua)

                  case _ =>
                    logger.error(
                      "[SubcontractorDetailsAddedController][onPageLoadWithId] - Missing or invalid MonthlyFinalValidationSourcePage"
                    )
                    Future.successful(
                      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    )
                }
              }

            case false =>
              logger.error(
                s"[SubcontractorDetailsAddedController][onPageLoadWithId] - isEditable returned false, cisId=$cisId"
              )
              Future.successful(
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              )
          }

        case None =>
          logger.error(
            "[SubcontractorDetailsAddedController][onPageLoadWithId] - CisIdPage or DateConfirmPaymentsPage missing"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  def onPageLoadAfterFinalValidation(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val ua = request.userAnswers

      val requiredAnswers = for {
        cisId      <- ua.get(CisIdPage)
        taxDate    <- ua.get(DateConfirmPaymentsPage)
        returnType <- ua.get(ReturnTypePage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers match {
        case Some((cisId, month, year, isAmendment)) =>
          monthlyReturnService.isEditable(cisId, month, year, isAmendment).map {
            case true
                if ua.get(MonthlyFinalValidationContinuationPage).contains(mode.toString) &&
                  ua.get(FinalValidationDraftIdPage).isEmpty =>
              renderPage(mode, ua)

            case true =>
              logger.error(
                "[SubcontractorDetailsAddedController][onPageLoadAfterFinalValidation] - Final Validation continuation missing or invalid"
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

            case false =>
              logger.error(
                s"[SubcontractorDetailsAddedController][onPageLoadAfterFinalValidation] - isEditable returned false, cisId=$cisId"
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

        case None =>
          logger.error(
            "[SubcontractorDetailsAddedController][onPageLoadAfterFinalValidation] - CisIdPage or DateConfirmPaymentsPage missing"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onPageLoadAfterFinalValidationWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val ua           = request.userAnswers
      val cisId        = request.cisId
      val subJourneyId = request.subJourneyId

      val requiredAnswers = for {
        taxDate    <- ua.get(DateConfirmPaymentsPage(subJourneyId))
        returnType <- ua.get(ReturnTypePage(subJourneyId))
      } yield (taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers match {
        case Some((month, year, isAmendment)) =>
          monthlyReturnService.isEditable(cisId, month, year, isAmendment).map {
            case true
                if ua.get(MonthlyFinalValidationContinuationPage(subJourneyId)).contains(mode.toString) &&
                  ua.get(FinalValidationDraftIdPage(subJourneyId)).isEmpty =>
              renderPageWithId(subJourneyId, cisPath, flowId, mode, ua)

            case true =>
              logger.error(
                "[SubcontractorDetailsAddedController][onPageLoadAfterFinalValidationWithId] - Final Validation continuation missing or invalid"
              )
              Redirect(
                controllers.routes.JourneyRecoveryController.onPageLoad()
              )

            case false =>
              logger.error(
                s"[SubcontractorDetailsAddedController][onPageLoadAfterFinalValidationWithId] - isEditable returned false, cisId=$cisId"
              )
              Redirect(
                controllers.routes.JourneyRecoveryController.onPageLoad()
              )
          }

        case None =>
          logger.error(
            "[SubcontractorDetailsAddedController][onPageLoadAfterFinalValidationWithId] - CisIdPage or DateConfirmPaymentsPage missing"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      val hasPassedFinalValidation =
        request.userAnswers.get(MonthlyFinalValidationContinuationPage).contains(mode.toString) &&
          request.userAnswers.get(FinalValidationDraftIdPage).isEmpty

      if (request.userAnswers.get(ReturnTypePage).isEmpty) {
        logger.error("[SubcontractorDetailsAddedController][onSubmit] - ReturnTypePage missing")
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      } else if (!hasPassedFinalValidation) {
        Future.successful(
          Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(mode))
        )
      } else {
        val isAmendmentOnSubmit = request.userAnswers.get(ReturnTypePage).exists(_.amendmentFlag == "Y")

        SubcontractorDetailsAddedBuilder.build(request.userAnswers) match {
          case None if isAmendmentOnSubmit =>
            Future.successful(
              Redirect(controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad())
            )
          case None                        =>
            logger.error("[SubcontractorDetailsAddedController][onSubmit] - view model build returned None")
            Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))

          case Some(viewModel) =>
            if (!viewModel.showYesNo) {
              if (viewModel.hasIncomplete) {
                Future.successful(
                  BadRequest(
                    view(
                      form
                        .fill(false)
                        .withError("summaryList", "monthlyreturns.subcontractorDetailsAdded.error.incomplete"),
                      mode,
                      viewModel
                    )
                  )
                )
              } else {
                val updatedUa =
                  request.userAnswers.set(AllSubcontractorDetailsAdded, true).getOrElse(request.userAnswers)
                sessionRepository.set(updatedUa).map { _ =>
                  Redirect(controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController.onPageLoad())
                }
              }
            } else {
              form
                .bindFromRequest()
                .fold(
                  formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, viewModel))),
                  isAddingMoreSubcontractors =>
                    val allSubcontractorDetailsAdded = !isAddingMoreSubcontractors

                    val updatedUa =
                      request.userAnswers
                        .set(AllSubcontractorDetailsAdded, allSubcontractorDetailsAdded)
                        .flatMap { answers =>
                          if (isAddingMoreSubcontractors) {
                            answers.remove(MonthlyFinalValidationContinuationPage)
                          } else {
                            Success(answers)
                          }
                        }
                        .getOrElse(request.userAnswers)

                    sessionRepository.set(updatedUa).map { _ =>
                      if (allSubcontractorDetailsAdded && viewModel.hasIncomplete) {
                        val withError =
                          form
                            .fill(isAddingMoreSubcontractors)
                            .withError(
                              "summaryList",
                              "monthlyreturns.subcontractorDetailsAdded.error.incomplete"
                            )
                        BadRequest(view(withError, mode, viewModel))
                      } else if (allSubcontractorDetailsAdded) {
                        Redirect(controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController.onPageLoad())
                      } else {
                        (
                          updatedUa.get(ReturnTypePage),
                          updatedUa.get(IsResumedMonthlyReturnPage)
                        ) match {
                          case (Some(MonthlyStandardReturn), Some(false)) =>
                            Redirect(
                              controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None)
                            )

                          case (Some(MonthlyStandardReturn), Some(true)) =>
                            Redirect(
                              controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(mode)
                            )

                          case (Some(returnType), _) if returnType.amendmentFlag == "Y" =>
                            Redirect(
                              controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(mode)
                            )

                          case _ =>
                            logger.error(
                              "[SubcontractorDetailsAddedController][onSubmit] - Missing or invalid monthly return journey origin"
                            )
                            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                        }
                      }
                    }
                )
            }
        }
      }
    }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val subJourneyId = request.subJourneyId

      val hasPassedFinalValidation =
        request.userAnswers.get(MonthlyFinalValidationContinuationPage(subJourneyId)).contains(mode.toString) &&
          request.userAnswers.get(FinalValidationDraftIdPage(subJourneyId)).isEmpty

      if (request.userAnswers.get(ReturnTypePage(subJourneyId)).isEmpty) {
        logger.error("[SubcontractorDetailsAddedController][onSubmit] - ReturnTypePage missing")
        Future.successful(
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        )
      } else if (!hasPassedFinalValidation) {
        Future.successful(
          Redirect(
            controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
              .onPageLoadWithId(cisPath, flowId, mode)
          )
        )
      } else {
        val isAmendmentOnSubmit = request.userAnswers.get(ReturnTypePage(subJourneyId)).exists(_.amendmentFlag == "Y")
        val formAction          = Some(routes.SubcontractorDetailsAddedController.onSubmitWithId(cisPath, flowId, mode))
        val cancelAction        = Some(
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onCancelAmendmentWithId(
              cisPath,
              flowId
            )
            .url
        )

        SubcontractorDetailsAddedBuilder.build(request.userAnswers, Option(subJourneyId)) match {
          case None if isAmendmentOnSubmit =>
            Future.successful(
              Redirect(
                controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad()
              )
            )
          case None                        =>
            logger.error("[SubcontractorDetailsAddedController][onSubmit] - view model build returned None")
            Future.successful(
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            ) // TODO DTR-9474: Do we need to update to use id scope?

          case Some(viewModel) =>
            if (!viewModel.showYesNo) {
              if (viewModel.hasIncomplete) {
                Future.successful(
                  BadRequest(
                    view(
                      form
                        .fill(false)
                        .withError("summaryList", "monthlyreturns.subcontractorDetailsAdded.error.incomplete"),
                      mode,
                      viewModel,
                      formAction,
                      cancelAction
                    )
                  )
                )
              } else {
                val updatedUa =
                  request.userAnswers
                    .set(AllSubcontractorDetailsAdded(subJourneyId), true)
                    .getOrElse(request.userAnswers)
                sessionRepository.set(updatedUa).map { _ =>
                  Redirect(
                    controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController
                      .onPageLoadWithId(cisPath, flowId)
                  )
                }
              }
            } else {
              form
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(
                        view(
                          formWithErrors,
                          mode,
                          viewModel,
                          formAction,
                          cancelAction
                        )
                      )
                    ),
                  isAddingMoreSubcontractors =>
                    val allSubcontractorDetailsAdded = !isAddingMoreSubcontractors

                    val updatedUa =
                      request.userAnswers
                        .set(AllSubcontractorDetailsAdded, allSubcontractorDetailsAdded)
                        .flatMap { answers =>
                          if (isAddingMoreSubcontractors) {
                            answers.remove(MonthlyFinalValidationContinuationPage(subJourneyId))
                          } else {
                            Success(answers)
                          }
                        }
                        .getOrElse(request.userAnswers)

                    sessionRepository.set(updatedUa).map { _ =>
                      if (allSubcontractorDetailsAdded && viewModel.hasIncomplete) {
                        val withError =
                          form
                            .fill(isAddingMoreSubcontractors)
                            .withError(
                              "summaryList",
                              "monthlyreturns.subcontractorDetailsAdded.error.incomplete"
                            )
                        BadRequest(
                          view(
                            withError,
                            mode,
                            viewModel,
                            formAction,
                            cancelAction
                          )
                        )
                      } else if (allSubcontractorDetailsAdded) {
                        Redirect(
                          controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController
                            .onPageLoadWithId(cisPath, flowId)
                        )
                      } else {
                        (
                          updatedUa.get(ReturnTypePage(subJourneyId)),
                          updatedUa.get(IsResumedMonthlyReturnPage(subJourneyId))
                        ) match {
                          case (Some(MonthlyStandardReturn), Some(false)) =>
                            Redirect(
                              controllers.monthlyreturns.routes.SelectSubcontractorsController
                                .onPageLoadWithId(cisPath, flowId, None)
                            )

                          case (Some(MonthlyStandardReturn), Some(true)) =>
                            Redirect(
                              controllers.amend.routes.WhichSubcontractorsToAddController
                                .onPageLoadWithId(cisPath, flowId, mode)
                            )

                          case (Some(returnType), _) if returnType.amendmentFlag == "Y" =>
                            Redirect(
                              controllers.amend.routes.WhichSubcontractorsToAddController
                                .onPageLoadWithId(cisPath, flowId, mode)
                            )

                          case _ =>
                            logger.error(
                              "[SubcontractorDetailsAddedController][onSubmitWithId] - Missing or invalid monthly return journey origin"
                            )
                            Redirect(
                              controllers.routes.JourneyRecoveryController.onPageLoad()
                            )
                        }
                      }
                    }
                )
            }
        }
      }
    }

  def onCancelAmendment(): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId) { implicit request =>
      Redirect(controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoad())
    }

  def onCancelAmendmentWithId(cisPath: CisPath, flowId: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)) { implicit request =>
      Redirect(
        controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoad()
      ) // TODO DTR-9474: Update with future id scope
    }

  private def runFinalValidation(
    cisId: String,
    month: Int,
    year: Int,
    mode: Mode,
    ua: UserAnswers
  )(implicit request: Request[?]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    subcontractorService
      .getSelectedSubcontractorsForFinalValidation(cisId, month, year, ua)
      .flatMap { selectedFullSubcontractors =>
        val validation = finalValidationService.validate(subcontractors = selectedFullSubcontractors)
        logger.info(
          s"[SubcontractorDetailsAddedController][runFinalValidation] " +
            s"selectedCount=${selectedFullSubcontractors.size}, " +
            s"hasErrors=${validation.hasErrors}"
        )

        if (validation.hasErrors) {
          for {
            createRequest <- Future.fromTry(
                               finalValidationDraftRequestBuilder.build(
                                 instanceId = cisId,
                                 selectedSubcontractors = selectedFullSubcontractors,
                                 validation = validation
                               )
                             )
            draftId       <- finalValidationDraftService.create(createRequest)
            withDraftId   <- Future.fromTry(ua.set(FinalValidationDraftIdPage, draftId))
            _             <- sessionRepository.set(withDraftId)
          } yield Redirect(controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onPageLoad())
        } else {
          for {
            withContinuation <- Future.fromTry(ua.set(MonthlyFinalValidationContinuationPage, mode.toString))
            _                <- sessionRepository.set(withContinuation)
          } yield renderPage(mode, withContinuation)
        }
      }
      .recover { case ex =>
        logger.error(s"[SubcontractorDetailsAddedController] Final validation failed: ${ex.getMessage}", ex)
        Redirect(controllers.routes.SystemErrorController.onPageLoad())
      }
  }

  private def runFinalValidationWithId(
    subJourneyId: String,
    cisPath: CisPath,
    flowId: UUID,
    cisId: String,
    month: Int,
    year: Int,
    mode: Mode,
    ua: UserAnswers
  )(implicit request: Request[?]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(
      request,
      request.session
    ) // TODO DTR-9474: Update with future id scope

    subcontractorService
      .getSelectedSubcontractorsForFinalValidation(cisId, month, year, ua) // TODO DTR-9474: Update with future id scope
      .flatMap { selectedFullSubcontractors =>
        val validation = finalValidationService.validate(subcontractors = selectedFullSubcontractors)
        logger.info(
          s"[SubcontractorDetailsAddedController][runFinalValidationWithId] " +
            s"selectedCount=${selectedFullSubcontractors.size}, " +
            s"hasErrors=${validation.hasErrors}"
        )

        if (validation.hasErrors) {
          for {
            createRequest <- Future.fromTry(
                               finalValidationDraftRequestBuilder.build( // TODO DTR-9474: Update with future id scope
                                 instanceId = cisId,
                                 selectedSubcontractors = selectedFullSubcontractors,
                                 validation = validation
                               )
                             )
            draftId       <- finalValidationDraftService.create(createRequest)
            withDraftId   <- Future.fromTry(ua.set(FinalValidationDraftIdPage, draftId))
            _             <- sessionRepository.set(withDraftId)
          } yield Redirect(
            controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onPageLoad()
          ) // TODO DTR-9474: Update with future id scope
        } else {
          for {
            withContinuation <-
              Future.fromTry(ua.set(MonthlyFinalValidationContinuationPage(subJourneyId), mode.toString))
            _                <- sessionRepository.set(withContinuation)
          } yield renderPageWithId(subJourneyId, cisPath, flowId, mode, withContinuation)
        }
      }
      .recover { case ex =>
        logger.error(s"[SubcontractorDetailsAddedController] Final validation failed: ${ex.getMessage}", ex)
        Redirect(
          controllers.routes.SystemErrorController.onPageLoad()
        ) // TODO DTR-9474: Do we need to update to use id scope?
      }
  }

  private def renderPage(
    mode: Mode,
    ua: UserAnswers
  )(implicit request: Request[?]): Result =
    ua.get(ReturnTypePage) match {

      case None =>
        logger.error("[SubcontractorDetailsAddedController][renderPage] - ReturnTypePage missing")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

      case Some(returnType) =>
        val preparedForm =
          ua.get(AllSubcontractorDetailsAdded)
            .fold(form)(allSubcontractorDetailsAdded => form.fill(!allSubcontractorDetailsAdded))

        SubcontractorDetailsAddedBuilder.build(ua) match {

          case Some(viewModel) =>
            Ok(view(preparedForm, mode, viewModel))

          case None if returnType.amendmentFlag == "Y" =>
            Redirect(
              controllers.amend.routes.WhatDoYouWantToAmendStandardController
                .onPageLoad()
            )

          case None =>
            logger.error("[SubcontractorDetailsAddedController][renderPage] - view model build returned None")
            Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  private def renderPageWithId(
    subJourneyId: String,
    cisPath: CisPath,
    flowId: UUID,
    mode: Mode,
    ua: UserAnswers
  )(implicit request: Request[?]): Result =
    ua.get(ReturnTypePage(subJourneyId)) match {

      case None =>
        logger.error("[SubcontractorDetailsAddedController][renderPageWithId] - ReturnTypePage missing")
        Redirect(
          controllers.routes.JourneyRecoveryController.onPageLoad()
        ) // TODO DTR-9474: Do we need to update to use id scope?

      case Some(returnType) =>
        val preparedForm =
          ua.get(AllSubcontractorDetailsAdded(subJourneyId))
            .fold(form)(allSubcontractorDetailsAdded => form.fill(!allSubcontractorDetailsAdded))
        val formAction   = Some(routes.SubcontractorDetailsAddedController.onSubmitWithId(cisPath, flowId, mode))
        val cancelAction = Some(
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onCancelAmendmentWithId(cisPath, flowId)
            .url
        )

        SubcontractorDetailsAddedBuilder.build(ua, Option(subJourneyId)) match {

          case Some(viewModel) =>
            Ok(
              view(
                preparedForm,
                mode,
                viewModel,
                formAction,
                cancelAction
              )
            )

          case None if returnType.amendmentFlag == "Y" =>
            Redirect(
              controllers.amend.routes.WhatDoYouWantToAmendStandardController // TODO DTR-9474: Update with future id scope
                .onPageLoad()
            )

          case None =>
            logger.error("[SubcontractorDetailsAddedController][renderPageWithId] - view model build returned None")
            Redirect(
              controllers.routes.SystemErrorController.onPageLoad()
            ) // TODO DTR-9474: Do we need to update to use id scope?
        }
    }

  private def modeFromString(value: String): Option[Mode] =
    value match {
      case "NormalMode" => Some(NormalMode)
      case "CheckMode"  => Some(CheckMode)
      case _            => None
    }

  private def sourceMode(source: MonthlyFinalValidationSource): Option[Mode] =
    source match {
      case MonthlyFinalValidationSource.SelectSubcontractors =>
        Some(NormalMode)

      case MonthlyFinalValidationSource.WhichSubcontractorsToAdd(modeValue) =>
        modeFromString(modeValue)
    }
}
