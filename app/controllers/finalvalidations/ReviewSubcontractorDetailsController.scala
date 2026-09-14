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
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import models.finalvalidation.{FinalValidationReadiness, MonthlyFinalValidationSource, ReviewSubcontractorDetailsPageModel, ReviewSubcontractorDetailsRow}
import navigation.Navigator
import pages.amend.WhichSubcontractorsToAddPage
import pages.finalvalidations.{FinalValidationDraftIdPage, FinalValidationVerificationRequiredPage, MonthlyFinalValidationSourcePage}

import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
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
  navigator: Navigator,
  finalValidationDraftService: FinalValidationDraftService,
  val controllerComponents: MessagesControllerComponents,
  view: ReviewSubcontractorDetailsView
)(using ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

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
                    subcontractor.displayName,
                    subcontractor.readiness == FinalValidationReadiness.Incomplete
                  )
                }

              val backUrl =
                request.userAnswers
                  .get(MonthlyFinalValidationSourcePage)
                  .map {
                    case MonthlyFinalValidationSource.SelectSubcontractors                =>
                      controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
                    case MonthlyFinalValidationSource.WhichSubcontractorsToAdd(modeValue) =>
                      modeFromString(modeValue)
                        .map(mode => controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(mode).url)
                        .getOrElse(controllers.routes.JourneyRecoveryController.onPageLoad().url)
                  }
                  .getOrElse(controllers.routes.JourneyRecoveryController.onPageLoad().url)

              Ok(
                view(
                  ReviewSubcontractorDetailsPageModel(rows, draft.allComplete, backUrl)
                )
              )
            }

        case None =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  def onSubmit: Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>

      val draftIdOpt = request.userAnswers.get(FinalValidationDraftIdPage)
      val sourceOpt  = request.userAnswers.get(MonthlyFinalValidationSourcePage)

      (draftIdOpt, sourceOpt) match {
        case (Some(draftId), Some(source)) =>
          finalValidationDraftService
            .get(request.cisId, draftId)
            .flatMap { draft =>
              if (!draft.allComplete) {
                Future.successful(Redirect(routes.ReviewSubcontractorDetailsController.onPageLoad()))
              } else {
                val verificationRequired = request.userAnswers.get(FinalValidationVerificationRequiredPage)

                for {
                  _              <- finalValidationDraftService.commit(request.cisId, draftId)
                  cleanedAnswers <- Future.fromTry(clearFinalValidationState(request.userAnswers))
                  _              <- sessionRepository.set(cleanedAnswers)
                } yield continueJourney(source, verificationRequired, cleanedAnswers)
              }
            }

        case _ =>
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def clearFinalValidationState(userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      withoutDraftId              <- userAnswers.remove(FinalValidationDraftIdPage)
      withoutSource               <- withoutDraftId.remove(MonthlyFinalValidationSourcePage)
      withoutVerificationRequired <- withoutSource.remove(FinalValidationVerificationRequiredPage)
    } yield withoutVerificationRequired

  private def continueJourney(
    source: MonthlyFinalValidationSource,
    verificationRequired: Option[Boolean],
    userAnswers: UserAnswers
  ): Result =
    source match {
      case MonthlyFinalValidationSource.SelectSubcontractors =>
        verificationRequired match {

          case Some(true) =>
            Redirect(controllers.monthlyreturns.routes.VerifySubcontractorsController.onPageLoad(NormalMode))

          case Some(false) =>
            Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))

          case None =>
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }

      case MonthlyFinalValidationSource.WhichSubcontractorsToAdd(modeValue) =>
        modeFromString(modeValue)
          .map { mode =>
            Redirect(navigator.nextPage(WhichSubcontractorsToAddPage, mode, userAnswers))
          }
          .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  private def modeFromString(value: String): Option[Mode] =
    value match {
      case "NormalMode" => Some(NormalMode)
      case "CheckMode"  => Some(CheckMode)
      case _            => None
    }

}
