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

package controllers.finalvalidations

import controllers.actions.*
import models.requests.CisPath
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import models.finalvalidation.*
import pages.finalvalidations.{FinalValidationDraftIdPage, MonthlyFinalValidationContinuationPage, MonthlyFinalValidationSourcePage}
import pages.monthlyreturns.SelectedSubcontractorPage

import javax.inject.Inject
import java.util.UUID
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import repositories.SessionRepository
import services.finalvalidation.FinalValidationDraftService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.finalvalidations.ReviewSubcontractorDetailsView

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ReviewSubcontractorDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  sessionRepository: SessionRepository,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  finalValidationDraftService: FinalValidationDraftService,
  pageModelBuilder: UpdateSubcontractorDetailsPageModelBuilder,
  val controllerComponents: MessagesControllerComponents,
  view: ReviewSubcontractorDetailsView
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage) match {

        case Some(draftId) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .map { draft =>
              val rows =
                draft.subcontractors.map { subcontractor =>
                  ReviewSubcontractorDetailsRow(
                    subcontractor.subcontractorId,
                    pageModelBuilder.displayName(subcontractor),
                    subcontractor.readiness == FinalValidationReadiness.Incomplete
                  )
                }

              val backUrl =
                request.userAnswers
                  .get(MonthlyFinalValidationSourcePage)
                  .map(backUrlFromSource)
                  .getOrElse {
                    logger.error(
                      "[ReviewSubcontractorDetailsController] Unable to determine back URL: " +
                        "MonthlyFinalValidationSourcePage is missing from user answers"
                    )
                    controllers.routes.JourneyRecoveryController.onPageLoad().url
                  }

              Ok(
                view(
                  ReviewSubcontractorDetailsPageModel(rows, draft.allComplete, backUrl)
                )
              )
            }

        case None =>
          logger.error(
            "[ReviewSubcontractorDetailsController] Unable to load review page: " +
              "FinalValidationDraftIdPage is missing from user answers"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  def onPageLoadWithId(cisPath: CisPath, flowId: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage(request.subJourneyId)) match {

        case Some(draftId) =>
          finalValidationDraftService // TODO DTR-9474: Need to investigate this
            .get(request.cisId, draftId)
            .map { draft =>
              val rows =
                draft.subcontractors.map { subcontractor =>
                  ReviewSubcontractorDetailsRow(
                    subcontractor.subcontractorId,
                    pageModelBuilder
                      .displayName(subcontractor), // TODO DTR-9474: Need to check, but I dont think this needs changed
                    subcontractor.readiness == FinalValidationReadiness.Incomplete
                  )
                }

              val backUrl =
                request.userAnswers
                  .get(MonthlyFinalValidationSourcePage(request.subJourneyId))
                  .map(source => backUrlFromSourceWithId(cisPath, flowId, source))
                  .getOrElse {
                    logger.error(
                      "[ReviewSubcontractorDetailsController] Unable to determine back URL: " +
                        "MonthlyFinalValidationSourcePage is missing from user answers"
                    )
                    controllers.routes.JourneyRecoveryController.onPageLoad().url
                  }

              val formAction = Some(
                controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onSubmitWithId(cisPath, flowId)
              )

//              def taskListAction(rowSubcontractorId: Long): Option[Call] = Some(
//                controllers.finalvalidations.routes.UpdateSubcontractorDetailsController
//                  .onPageLoadWithId(cisPath, flowId, rowSubcontractorId).url
//              )
              // TODO DTR-9474: Update to use future id scope, complication with use of row within view
              // TODO DTR-9474: Final validation out of scope

              Ok(
                view(
                  ReviewSubcontractorDetailsPageModel(rows, draft.allComplete, backUrl),
                  formAction
//                  taskListAction
                )
              )
            }

        case None =>
          logger.error(
            "[ReviewSubcontractorDetailsController] Unable to load review page: " +
              "FinalValidationDraftIdPage is missing from user answers"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  def onSubmit: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      val draftIdOpt = request.userAnswers.get(FinalValidationDraftIdPage)
      val sourceOpt  = request.userAnswers.get(MonthlyFinalValidationSourcePage)

      (draftIdOpt, sourceOpt.flatMap(modeFromSource)) match {
        case (Some(draftId), Some(mode)) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .flatMap { draft =>
              if (!draft.allComplete) {
                Future.successful(Redirect(routes.ReviewSubcontractorDetailsController.onPageLoad()))
              } else {
                for {
                  _                <- finalValidationDraftService.commit(request.cisId, draftId)
                  updatedAnswers   <- Future.fromTry(
                                        updateSelectedSubcontractorNames(request.userAnswers, draft)
                                      )
                  cleanedAnswers   <- Future.fromTry(clearFinalValidationState(updatedAnswers))
                  withContinuation <- Future.fromTry(
                                        cleanedAnswers.set(MonthlyFinalValidationContinuationPage, mode.toString)
                                      )
                  _                <- sessionRepository.set(withContinuation)
                } yield Redirect(
                  controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
                    .onPageLoadAfterFinalValidation(mode)
                )
              }
            }

        case _ =>
          logger.error(
            s"[ReviewSubcontractorDetailsController] Unable to submit final validation: " +
              s"required journey data missing - " +
              s"FinalValidationDraftIdPage present=${draftIdOpt.isDefined}, " +
              s"MonthlyFinalValidationSourcePage present=${sourceOpt.isDefined}"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )

      }
    }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val subJourneyId = request.subJourneyId
      val draftIdOpt   = request.userAnswers.get(FinalValidationDraftIdPage(subJourneyId))
      val sourceOpt    = request.userAnswers.get(MonthlyFinalValidationSourcePage(subJourneyId))

      (draftIdOpt, sourceOpt.flatMap(modeFromSource)) match {
        case (Some(draftId), Some(mode)) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .flatMap { draft =>
              if (!draft.allComplete) {
                Future.successful(
                  Redirect(routes.ReviewSubcontractorDetailsController.onPageLoadWithId(cisPath, flowId))
                )
              } else {
                for {
                  _                <-
                    finalValidationDraftService
                      .commit(request.cisId, draftId) // TODO DTR-9474: Final validation out of scope
                  updatedAnswers   <- Future.fromTry(
                                        updateSelectedSubcontractorNamesWithId(subJourneyId, request.userAnswers, draft)
                                      )
                  cleanedAnswers   <- Future.fromTry(clearFinalValidationStateWithId(subJourneyId, updatedAnswers))
                  withContinuation <-
                    Future.fromTry(
                      cleanedAnswers.set(MonthlyFinalValidationContinuationPage(subJourneyId), mode.toString)
                    )
                  _                <- sessionRepository.set(withContinuation)
                } yield Redirect(
                  controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
                    .onPageLoadAfterFinalValidationWithId(cisPath, flowId, mode)
                )
              }
            }

        case _ =>
          logger.error(
            s"[ReviewSubcontractorDetailsController] Unable to submit final validation: " +
              s"required journey data missing - " +
              s"FinalValidationDraftIdPage present=${draftIdOpt.isDefined}, " +
              s"MonthlyFinalValidationSourcePage present=${sourceOpt.isDefined}"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )

      }
    }

  private def clearFinalValidationState(userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      withoutDraftId <- userAnswers.remove(FinalValidationDraftIdPage)
      withoutSource  <- withoutDraftId.remove(MonthlyFinalValidationSourcePage)
    } yield withoutSource

  private def clearFinalValidationStateWithId(id: String, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      withoutDraftId <- userAnswers.remove(FinalValidationDraftIdPage(id))
      withoutSource  <- withoutDraftId.remove(MonthlyFinalValidationSourcePage(id))
    } yield withoutSource

  private def modeFromString(value: String): Option[Mode] =
    value match {
      case "NormalMode" => Some(NormalMode)
      case "CheckMode"  => Some(CheckMode)
      case _            => None
    }

  private def modeFromSource(
    source: MonthlyFinalValidationSource
  ): Option[Mode] = // TODO DTR-9474: Final validation out of scope
    source match {
      case MonthlyFinalValidationSource.SelectSubcontractors =>
        Some(NormalMode)

      case MonthlyFinalValidationSource.WhichSubcontractorsToAdd(modeValue) =>
        modeFromString(modeValue)
    }

  private def backUrlFromSource(source: MonthlyFinalValidationSource): String =
    source match {
      case MonthlyFinalValidationSource.SelectSubcontractors =>
        controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url

      case MonthlyFinalValidationSource.WhichSubcontractorsToAdd(modeValue) =>
        modeFromString(modeValue)
          .map(mode => controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(mode).url)
          .getOrElse {
            logger.error(
              "[ReviewSubcontractorDetailsController] Unable to determine back URL: " +
                s"invalid WhichSubcontractorsToAdd mode: $modeValue"
            )
            controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
    }

  private def backUrlFromSourceWithId(cisPath: CisPath, flowId: UUID, source: MonthlyFinalValidationSource): String =
    source match {
      case MonthlyFinalValidationSource.SelectSubcontractors => // TODO DTR-9474: Final validation out of scope
        controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadWithId(cisPath, flowId, None).url

      case MonthlyFinalValidationSource.WhichSubcontractorsToAdd(modeValue) =>
        modeFromString(modeValue)
          .map(mode =>
            controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoadWithId(cisPath, flowId, mode).url
          )
          .getOrElse {
            logger.error(
              "[ReviewSubcontractorDetailsController] Unable to determine back URL: " +
                s"invalid WhichSubcontractorsToAdd mode: $modeValue"
            )
            controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
    }

  private def updateSelectedSubcontractorNames(
    userAnswers: UserAnswers,
    draft: FinalValidationDraft
  ): Try[UserAnswers] = {

    val namesBySubcontractorId =
      draft.subcontractors.map { subcontractor =>
        subcontractor.subcontractorId ->
          pageModelBuilder.resolvedName(subcontractor)
      }.toMap

    val selectedSubcontractors =
      userAnswers
        .get(SelectedSubcontractorPage.all)
        .getOrElse(Map.empty)

    val updatedSubcontractors =
      selectedSubcontractors.map { case (index, subcontractor) =>
        val updated =
          namesBySubcontractorId
            .get(subcontractor.id)
            .map { name =>
              subcontractor.copy(
                name = name
              )
            }
            .getOrElse(subcontractor)

        index -> updated
      }

    userAnswers.set(
      SelectedSubcontractorPage.all,
      updatedSubcontractors
    )
  }

  private def updateSelectedSubcontractorNamesWithId(
    subJourneyId: String,
    userAnswers: UserAnswers,
    draft: FinalValidationDraft
  ): Try[UserAnswers] = {

    val namesBySubcontractorId =
      draft.subcontractors.map { subcontractor =>
        subcontractor.subcontractorId ->
          pageModelBuilder.resolvedName(subcontractor)
      }.toMap

    val selectedSubcontractors =
      userAnswers
        .get(SelectedSubcontractorPage.all(Some(subJourneyId)))
        .getOrElse(Map.empty)

    val updatedSubcontractors =
      selectedSubcontractors.map { case (index, subcontractor) =>
        val updated =
          namesBySubcontractorId
            .get(subcontractor.id)
            .map { name =>
              subcontractor.copy(
                name = name
              )
            }
            .getOrElse(subcontractor)

        index -> updated
      }

    userAnswers.set(
      SelectedSubcontractorPage.all(Some(subJourneyId)),
      updatedSubcontractors
    )
  }

}
