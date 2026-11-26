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
import forms.monthlyreturns.ConfirmSubcontractorRemovalFormProvider
import models.monthlyreturns.{DeleteMonthlyReturnItemRequest, SelectedSubcontractor}
import models.requests.DataRequest
import models.ReturnType.MonthlyAmendedStandardReturn
import models.{CheckMode, Mode, NormalMode, UserAnswers}
import pages.monthlyreturns.*
import pages.amend.{AmendmentDetailsPage, WhichSubcontractorsToAddPage}
import pages.finalvalidations.{FinalValidationDraftIdPage, MonthlyFinalValidationContinuationPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.ConfirmSubcontractorRemovalView

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmSubcontractorRemovalController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  multiTabAction: MultiTabAction,
  formProvider: ConfirmSubcontractorRemovalFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmSubcontractorRemovalView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode, index: Int): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>
      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None =>
          logger.error(
            s"[ConfirmSubcontractorRemovalController][onPageLoad] - SelectedSubcontractorPage missing, index: $index"
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

        case Some(subcontractor) =>
          Ok(view(form, mode, subcontractor.name, index))
      }
  }

  def onSubmit(mode: Mode, index: Int): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      request.userAnswers.get(SelectedSubcontractorPage(index)) match {
        case None =>
          logger.error(
            s"[ConfirmSubcontractorRemovalController][onSubmit] - SelectedSubcontractorPage missing, index: $index"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(subcontractor) =>
          form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, subcontractor.name, index))),
              confirmRemove =>
                for {
                  updatedAnswers1 <-
                    Future.fromTry(request.userAnswers.set(ConfirmSubcontractorRemovalPage(index), confirmRemove))

                  result <-
                    if (!confirmRemove) {
                      sessionRepository.set(updatedAnswers1).map { _ =>
                        redirectAfterValidation(updatedAnswers1)
                      }
                    } else {
                      deleteFlow(updatedAnswers1, index)
                    }
                } yield result
            )
      }
  }

  private def deleteFlow(ua: UserAnswers, index: Int)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] =
    buildDeletePayload(ua, index) match {
      case None =>
        logger.error(
          s"[ConfirmSubcontractorRemovalController][deleteFlow] - payload build failed, CisIdPage, " +
            s"DateConfirmPaymentsPage, SelectedSubcontractorPage or ReturnTypePage missing, index: $index"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(payload) =>
        (for {
          _              <- monthlyReturnService.deleteMonthlyReturnItem(payload)
          selectedIds     = ua.get(WhichSubcontractorsToAddPage).getOrElse(Set.empty) - payload.subcontractorId.toString
          ua1            <- Future.fromTry(ua.set(WhichSubcontractorsToAddPage, selectedIds))
          updatedAnswers <- Future.fromTry(ua1.remove(SelectedSubcontractorPage(index)))
          _              <- sessionRepository.set(updatedAnswers)
        } yield redirectAfterDelete(updatedAnswers))
          .recover { case e =>
            logger.error(
              s"[ConfirmSubcontractorRemovalController][deleteFlow] - delete flow failed, index: $index, " +
                s"subcontractorId: ${payload.subcontractorId}",
              e
            )
            Redirect(controllers.routes.SystemErrorController.onPageLoad())
          }
    }

  private def redirectAfterDelete(ua: UserAnswers): Result = {
    val subs                = selectedSubcontractors(ua)
    val isStandardAmendment =
      ua.get(ReturnTypePage).contains(MonthlyAmendedStandardReturn) ||
        ua.get(AmendmentDetailsPage).exists(_.originalReturnType == MonthlyAmendedStandardReturn)
    if (subs.isEmpty && isStandardAmendment) {
      Redirect(controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad())
    } else if (subs.isEmpty) {
      Redirect(controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None))
    } else {
      redirectAfterValidation(ua)
    }
  }

  private def buildDeletePayload(ua: UserAnswers, index: Int): Option[DeleteMonthlyReturnItemRequest] =
    for {
      cisId         <- ua.get(CisIdPage)
      monthYear     <- ua.get(DateConfirmPaymentsPage)
      subcontractor <- ua.get(SelectedSubcontractorPage(index))
      returnType    <- ua.get(ReturnTypePage)
    } yield DeleteMonthlyReturnItemRequest(
      instanceId = cisId,
      taxYear = monthYear.getYear,
      taxMonth = monthYear.getMonthValue,
      subcontractorId = subcontractor.id,
      amendment = returnType.amendmentFlag
    )

  private def selectedSubcontractors(ua: UserAnswers): Map[Int, SelectedSubcontractor] =
    ua.get(SelectedSubcontractorPage.all).getOrElse(Map.empty)

  private def redirectAfterValidation(ua: UserAnswers): Result =
    ua.get(MonthlyFinalValidationContinuationPage) match {

      case Some("NormalMode") if ua.get(FinalValidationDraftIdPage).isEmpty =>
        Redirect(
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoadAfterFinalValidation(NormalMode)
        )

      case Some("CheckMode") if ua.get(FinalValidationDraftIdPage).isEmpty =>
        Redirect(
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoadAfterFinalValidation(CheckMode)
        )

      case _ =>
        logger.error("[ConfirmSubcontractorRemovalController] Missing or invalid Final Validation continuation")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  // Multi-tab
  def onPageLoadWithId(flowId: UUID, mode: Mode, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)) { implicit request =>
      val subJourneyId = flowId.toString

      request.userAnswers.get(SelectedSubcontractorForFlowPage(subJourneyId, index)) match {
        case None =>
          logger.error(
            s"[ConfirmSubcontractorRemovalController][onPageLoadWithId] - SelectedSubcontractorForFlowPage missing, index: $index"
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())

        case Some(subcontractor) =>
          val formAction =
            Some(
              controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController
                .onSubmitWithId(flowId, mode, index)
            )

          Ok(view(form, mode, subcontractor.name, index, formAction))
      }
    }

  def onSubmitWithId(flowId: UUID, mode: Mode, index: Int): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val subJourneyId = flowId.toString

      request.userAnswers.get(SelectedSubcontractorForFlowPage(subJourneyId, index)) match {
        case None =>
          logger.error(
            s"[ConfirmSubcontractorRemovalController][onSubmitWithId] - SelectedSubcontractorForFlowPage missing, index: $index"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

        case Some(subcontractor) =>
          val formAction =
            Some(
              controllers.monthlyreturns.routes.ConfirmSubcontractorRemovalController
                .onSubmitWithId(flowId, mode, index)
            )

          form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(BadRequest(view(formWithErrors, mode, subcontractor.name, index, formAction))),
              confirmRemove =>
                for {
                  updatedAnswers <-
                    Future.fromTry(
                      request.userAnswers
                        .set(ConfirmSubcontractorRemovalForFlowPage(subJourneyId, index), confirmRemove)
                    )

                  result <-
                    if (!confirmRemove) {
                      sessionRepository.set(updatedAnswers).map { _ =>
                        redirectAfterValidationWithId(updatedAnswers, flowId)
                      }
                    } else {
                      deleteFlowWithId(updatedAnswers, flowId, index)
                    }
                } yield result
            )
      }
    }

  private def deleteFlowWithId(ua: UserAnswers, flowId: UUID, index: Int)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val subJourneyId = flowId.toString

    buildDeletePayloadWithId(ua, subJourneyId, index) match {
      case None =>
        logger.error(
          s"[ConfirmSubcontractorRemovalController][deleteFlowWithId] - payload build failed, CisIdPage, " +
            s"DateConfirmPaymentsPage, SelectedSubcontractorForFlowPage or ReturnTypePage missing, index: $index"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case Some(payload) =>
        (for {
          _ <- monthlyReturnService.deleteMonthlyReturnItem(payload)

          // TODO DTR-9475/DTR-9476: Update scoped WhichSubcontractorsToAddPage for amendment journeys.
          updatedAnswers <- Future.fromTry(ua.remove(SelectedSubcontractorForFlowPage(subJourneyId, index)))

          _ <- sessionRepository.set(updatedAnswers)
        } yield redirectAfterDeleteWithId(updatedAnswers, flowId))
          .recover { case e =>
            logger.error(
              s"[ConfirmSubcontractorRemovalController][deleteFlowWithId] - delete flow failed, index: $index, " +
                s"subcontractorId: ${payload.subcontractorId}",
              e
            )
            Redirect(controllers.routes.SystemErrorController.onPageLoad())
          }
    }
  }

  private def redirectAfterDeleteWithId(ua: UserAnswers, flowId: UUID): Result = {
    val subJourneyId = flowId.toString
    val subs         = selectedSubcontractorsWithId(ua, subJourneyId)

    val isStandardAmendment =
      ua.get(ReturnTypePage(subJourneyId)).contains(MonthlyAmendedStandardReturn) ||
        ua.get(AmendmentDetailsPage(subJourneyId))
          .exists(_.originalReturnType == MonthlyAmendedStandardReturn)

    if (subs.isEmpty && isStandardAmendment) {
      // TODO DTR-9475/DTR-9476: Implement UUID-scoped amendment navigation.
      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    } else if (subs.isEmpty) {
      Redirect(controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoadWithId(flowId, None))
    } else {
      redirectAfterValidationWithId(ua, flowId)
    }
  }

  private def buildDeletePayloadWithId(
    ua: UserAnswers,
    subJourneyId: String,
    index: Int
  ): Option[DeleteMonthlyReturnItemRequest] =
    for {
      cisId         <- ua.get(CisIdPage(subJourneyId))
      monthYear     <- ua.get(DateConfirmPaymentsPage(subJourneyId))
      subcontractor <- ua.get(SelectedSubcontractorForFlowPage(subJourneyId, index))
      returnType    <- ua.get(ReturnTypePage(subJourneyId))
    } yield DeleteMonthlyReturnItemRequest(
      instanceId = cisId,
      taxYear = monthYear.getYear,
      taxMonth = monthYear.getMonthValue,
      subcontractorId = subcontractor.id,
      amendment = returnType.amendmentFlag
    )

  private def selectedSubcontractorsWithId(ua: UserAnswers, subJourneyId: String): Map[Int, SelectedSubcontractor] =
    ua.get(SelectedSubcontractorPage.all(subJourneyId)).getOrElse(Map.empty)

  private def redirectAfterValidationWithId(ua: UserAnswers, flowId: UUID): Result = {
    val subJourneyId = flowId.toString

    ua.get(MonthlyFinalValidationContinuationPage(subJourneyId)) match {

      case Some("NormalMode") if ua.get(FinalValidationDraftIdPage(subJourneyId)).isEmpty =>
        Redirect(
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoadAfterFinalValidationWithId(flowId, NormalMode)
        )

      case Some("CheckMode") if ua.get(FinalValidationDraftIdPage(subJourneyId)).isEmpty =>
        Redirect(
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoadAfterFinalValidationWithId(flowId, CheckMode)
        )

      case _ =>
        // TODO DTR-9474: Complete UUID-scoped final-validation continuation.
        logger.error(
          "[ConfirmSubcontractorRemovalController][redirectAfterValidationWithId] - Missing or invalid Final Validation continuation"
        )
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }
}
