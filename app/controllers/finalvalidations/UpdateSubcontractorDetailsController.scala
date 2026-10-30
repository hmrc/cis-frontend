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
import models.finalvalidation.{FinalValidationReadiness, UpdateSubcontractorDetailsPageModel, UpdateSubcontractorDetailsPageModelBuilder}
import pages.finalvalidations.FinalValidationDraftIdPage
import services.finalvalidation.{FinalValidationDraftService, FinalValidationService}

import javax.inject.{Inject, Singleton}
import java.util.UUID
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.finalvalidations.UpdateSubcontractorDetailsView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateSubcontractorDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  finalValidationService: FinalValidationService,
  finalValidationDraftService: FinalValidationDraftService,
  pageModelBuilder: UpdateSubcontractorDetailsPageModelBuilder,
  val controllerComponents: MessagesControllerComponents,
  view: UpdateSubcontractorDetailsView
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(subcontractorId: Long): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage) match {

        case Some(draftId) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .map { draft =>
              draft.subcontractor(subcontractorId) match {
                case Some(subcontractor)
                    if subcontractor.readiness ==
                      FinalValidationReadiness.Complete =>
                  Redirect(routes.ReviewSubcontractorDetailsController.onPageLoad())

                case Some(subcontractor) =>
                  val rows =
                    pageModelBuilder.build(
                      subcontractor,
                      (field, target) =>
                        routes.FinalValidationChangeController
                          .onPageLoad(subcontractorId, field.key, target.key)
                          .url
                    )

                  val model =
                    UpdateSubcontractorDetailsPageModel(
                      subcontractor.subcontractorId,
                      pageModelBuilder.displayName(subcontractor),
                      rows
                    )

                  Ok(view(model))

                case None =>
                  logger.error(
                    s"[UpdateSubcontractorDetailsController] Unable to load update subcontractor details page: " +
                      s"subcontractor $subcontractorId is missing from final validation draft"
                  )
                  Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

              }
            }

        case None =>
          logger.error(
            "[UpdateSubcontractorDetailsController] Unable to load update subcontractor details page: " +
              "FinalValidationDraftIdPage is missing from user answers"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )

      }
    }

  def onPageLoadWithId(cisPath: CisPath, flowId: UUID, subcontractorId: Long): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage(request.subJourneyId)) match {

        case Some(draftId) =>
          finalValidationDraftService // TODO DTR-9474: Need to investigate this
            .get(request.cisId, draftId)
            .map { draft =>
              draft.subcontractor(subcontractorId) match {
                case Some(subcontractor)
                    if subcontractor.readiness ==
                      FinalValidationReadiness.Complete =>
                  Redirect(routes.ReviewSubcontractorDetailsController.onPageLoadWithId(cisPath, flowId))

                case Some(subcontractor) =>
                  val rows =
                    pageModelBuilder.build( // TODO DTR-9474: Need to check, but I dont think this needs changed
                      subcontractor,
                      (field, target) =>
                        routes.FinalValidationChangeController // TODO DTR-9474: Final validation out of scope
                          .onPageLoad(subcontractorId, field.key, target.key)
                          .url
                    )

                  val model =
                    UpdateSubcontractorDetailsPageModel(
                      subcontractor.subcontractorId,
                      pageModelBuilder.displayName(subcontractor),
                      rows
                    )

                  Ok(view(model))

                case None =>
                  logger.error(
                    s"[UpdateSubcontractorDetailsController] Unable to load update subcontractor details page: " +
                      s"subcontractor $subcontractorId is missing from final validation draft"
                  )
                  Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

              }
            }

        case None =>
          logger.error(
            "[UpdateSubcontractorDetailsController] Unable to load update subcontractor details page: " +
              "FinalValidationDraftIdPage is missing from user answers"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )

      }
    }

  def onSubmit(subcontractorId: Long): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage) match {

        case Some(draftId) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .flatMap { draft =>
              draft.subcontractor(subcontractorId) match {
                case Some(subcontractor)
                    if subcontractor.readiness ==
                      FinalValidationReadiness.Complete =>
                  Future.successful(Redirect(routes.ReviewSubcontractorDetailsController.onPageLoad()))

                case Some(_) =>
                  for {
                    issues <- Future.fromTry(finalValidationService.validateDraftSubcontractor(draft, subcontractorId))
                    _      <- finalValidationDraftService.updateReadiness(request.cisId, draftId, subcontractorId, issues)
                  } yield Redirect(routes.ReviewSubcontractorDetailsController.onPageLoad())

                case None =>
                  logger.error(
                    s"[UpdateSubcontractorDetailsController] Unable to submit update subcontractor details: " +
                      s"subcontractor $subcontractorId is missing from final validation draft"
                  )
                  Future.successful(
                    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  )

              }
            }

        case None =>
          logger.error(
            "[UpdateSubcontractorDetailsController] Unable to submit update subcontractor details: " +
              "FinalValidationDraftIdPage is missing from user answers"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )

      }
    }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID, subcontractorId: Long): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage(request.subJourneyId)) match {

        case Some(draftId) =>
          finalValidationDraftService // TODO DTR-9474: Need to investigate this
            .get(request.cisId, draftId)
            .flatMap { draft =>
              draft.subcontractor(subcontractorId) match {
                case Some(subcontractor)
                    if subcontractor.readiness ==
                      FinalValidationReadiness.Complete =>
                  Future.successful(
                    Redirect(routes.ReviewSubcontractorDetailsController.onPageLoadWithId(cisPath, flowId))
                  )

                case Some(_) =>
                  for {
                    issues <- Future.fromTry(
                                finalValidationService.validateDraftSubcontractor(draft, subcontractorId)
                              ) // TODO DTR-9474: Final validation out of scope
                    _      <- finalValidationDraftService.updateReadiness(request.cisId, draftId, subcontractorId, issues)
                  } yield Redirect(routes.ReviewSubcontractorDetailsController.onPageLoadWithId(cisPath, flowId))

                case None =>
                  logger.error(
                    s"[UpdateSubcontractorDetailsController] Unable to submit update subcontractor details: " +
                      s"subcontractor $subcontractorId is missing from final validation draft"
                  )
                  Future.successful(
                    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  )

              }
            }

        case None =>
          logger.error(
            "[UpdateSubcontractorDetailsController] Unable to submit update subcontractor details: " +
              "FinalValidationDraftIdPage is missing from user answers"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )

      }
    }

  def onCancel(subcontractorId: Long): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage) match {

        case Some(draftId) =>
          finalValidationDraftService
            .resetSubcontractor(
              request.cisId,
              draftId,
              subcontractorId
            )
            .map { _ =>
              Redirect(routes.ReviewSubcontractorDetailsController.onPageLoad())
            }

        case None =>
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
      }
    }

  def onCancelWithId(cisPath: CisPath, flowId: UUID, subcontractorId: Long): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      request.userAnswers.get(FinalValidationDraftIdPage(request.subJourneyId)) match {

        case Some(draftId) =>
          finalValidationDraftService // TODO DTR-9474: Need to investigate this
            .resetSubcontractor(
              request.cisId,
              draftId,
              subcontractorId
            )
            .map { _ =>
              Redirect(routes.ReviewSubcontractorDetailsController.onPageLoadWithId(cisPath, flowId))
            }

        case None =>
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoadWithId(cisPath, flowId))
          )
      }
    }
}
