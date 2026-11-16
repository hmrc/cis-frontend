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
import models.{NormalMode, UserAnswers}
import models.amend.WhatDoYouWantToAmendStandard
import models.finalvalidation.MonthlyFinalValidationSource
import models.monthlyreturns.SelectedSubcontractor
import models.requests.GetMonthlyReturnForEditRequest
import pages.amend.{WhatDoYouWantToAmendStandardPage, WhichSubcontractorsToAddPage}
import pages.finalvalidations.{FinalValidationDraftIdPage, MonthlyFinalValidationContinuationPage, MonthlyFinalValidationSourcePage}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, SelectedSubcontractorPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import utils.TypeUtils.toFuture
import utils.Utils.toBigDecimal
import views.html.amend.WhatDoYouWantToAmendStandardView

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

class WhatDoYouWantToAmendStandardController @Inject() (
  override val messagesApi: MessagesApi,
  monthlyReturnService: MonthlyReturnService,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  multiTabAction: MultiTabAction,
  formProvider: WhatDoYouWantToAmendStandardFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: WhatDoYouWantToAmendStandardView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

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
            // Preselect from the items already on the return. An item is only preselected when it resolves to a
            // subcontractor that still exists; the backend rejects unknown subcontractor ids (e.g. deleted ones)
            subcontractorsByRef       = monthlyReturn.subcontractors
                                          .flatMap(sub => sub.subbieResourceRef.map(_ -> sub))
                                          .toMap
            subcontractorsById        = monthlyReturn.subcontractors.map(sub => sub.subcontractorId -> sub).toMap
            itemsWithSubcontractor    = monthlyReturn.monthlyReturnItems.map { item =>
                                          val sub = item.itemResourceReference
                                            .flatMap(subcontractorsByRef.get)
                                            .orElse(item.subcontractorId.flatMap(subcontractorsById.get))
                                          (item, sub)
                                        }
            preselectedSubcontractors = itemsWithSubcontractor
                                          .collect { case (item, Some(sub)) =>
                                            SelectedSubcontractor(
                                              id = sub.subcontractorId,
                                              name = sub.displayName
                                                .orElse(item.subcontractorName)
                                                .getOrElse("No name provided"),
                                              totalPaymentsMade = item.totalPayments.flatMap(toBigDecimal),
                                              costOfMaterials = item.costOfMaterials.flatMap(toBigDecimal),
                                              totalTaxDeducted = item.totalDeducted.flatMap(toBigDecimal)
                                            )
                                          }
                                          .distinctBy(_.id)
                                          .zipWithIndex
                                          .map(x => (x._2 + 1, x._1))
                                          .toMap
            _                         = {
              val unresolved = monthlyReturn.monthlyReturnItems.size - preselectedSubcontractors.size
              if (unresolved > 0)
                logger.warn(
                  s"[WhatDoYouWantToAmendStandard] cisId=$cisId, taxYear=$year, taxMonth=$month: " +
                    s"$unresolved of ${monthlyReturn.monthlyReturnItems.size} existing items could not be matched to a subcontractor for preselection"
                )
            }

            ua1    <- request.userAnswers.set(WhatDoYouWantToAmendStandardPage, value).toFuture
            ua2    <- ua1.set(SelectedSubcontractorPage.all, preselectedSubcontractors).toFuture
            _      <- sessionRepository.set(ua2)
            result <-
              value match {
                case WhatDoYouWantToAmendStandard.AmendToNilReturn =>
                  Future
                    .successful(Redirect(controllers.amend.routes.AreYouSureYouWantToAmendYesNoController.onPageLoad()))

                case WhatDoYouWantToAmendStandard.AmendPaymentOrSubcontractorDetails
                    if preselectedSubcontractors.isEmpty =>
                  Future.successful(
                    Redirect(controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode))
                  )

                case WhatDoYouWantToAmendStandard.AmendPaymentOrSubcontractorDetails =>
                  amendMonthlyReturnService.startStandardAmendment(ua2).flatMap {
                    case Left(error) =>
                      logger.error(
                        s"[WhatDoYouWantToAmendStandard] Failed to start standard amendment: $error"
                      )
                      Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

                    case Right(_) =>
                      val subcontractorIds = ua2
                        .get(SelectedSubcontractorPage.all)
                        .fold(Set.empty[String])(_.values.map(_.id.toString).toSet)

                      for {
                        ua3 <- Future.fromTry(
                                 ua2.set(WhichSubcontractorsToAddPage, subcontractorIds)
                               )

                        ua4 <- Future.fromTry(
                                 ua3
                                   .remove(FinalValidationDraftIdPage)
                                   .flatMap(_.remove(MonthlyFinalValidationContinuationPage))
                                   .flatMap(
                                     _.set(
                                       MonthlyFinalValidationSourcePage,
                                       MonthlyFinalValidationSource.WhichSubcontractorsToAdd(
                                         NormalMode.toString
                                       )
                                     )
                                   )
                               )

                        _ <- sessionRepository.set(ua4)
                      } yield Redirect(
                        controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
                      )
                  }
              }
          } yield result
      )
  }

  def onPageLoadWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)) { implicit request =>

      val id = flowId.toString

      val preparedForm = request.userAnswers.get(WhatDoYouWantToAmendStandardPage(id)) match {
        case None        => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, Some(routes.WhatDoYouWantToAmendStandardController.onSubmitWithId(flowId))))
    }

  def onSubmitWithId(flowId: UUID): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen multiTabAction.forJourney(flowId)).async { implicit request =>
      val id = flowId.toString

      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(formWithErrors, Some(routes.WhatDoYouWantToAmendStandardController.onSubmitWithId(flowId)))
              )
            ),
          value => {
            val requiredAnswers = for {
              cisId   <- request.userAnswers.get(CisIdPage(id))
              taxDate <- request.userAnswers.get(DateConfirmPaymentsPage(id))
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
              // Preselect from the items already on the return. An item is only preselected when it resolves to a
              // subcontractor that still exists; the backend rejects unknown subcontractor ids (e.g. deleted ones)
              subcontractorsByRef       = monthlyReturn.subcontractors
                                            .flatMap(sub => sub.subbieResourceRef.map(_ -> sub))
                                            .toMap
              subcontractorsById        = monthlyReturn.subcontractors.map(sub => sub.subcontractorId -> sub).toMap
              itemsWithSubcontractor    = monthlyReturn.monthlyReturnItems.map { item =>
                                            val sub = item.itemResourceReference
                                              .flatMap(subcontractorsByRef.get)
                                              .orElse(item.subcontractorId.flatMap(subcontractorsById.get))
                                            (item, sub)
                                          }
              preselectedSubcontractors = itemsWithSubcontractor
                                            .collect { case (item, Some(sub)) =>
                                              SelectedSubcontractor(
                                                id = sub.subcontractorId,
                                                name = sub.displayName
                                                  .orElse(item.subcontractorName)
                                                  .getOrElse("No name provided"),
                                                totalPaymentsMade = item.totalPayments.flatMap(toBigDecimal),
                                                costOfMaterials = item.costOfMaterials.flatMap(toBigDecimal),
                                                totalTaxDeducted = item.totalDeducted.flatMap(toBigDecimal)
                                              )
                                            }
                                            .distinctBy(_.id)
                                            .zipWithIndex
                                            .map(x => (x._2 + 1, x._1))
                                            .toMap
              _                         = {
                val unresolved = monthlyReturn.monthlyReturnItems.size - preselectedSubcontractors.size
                if (unresolved > 0) {
                  logger.warn(
                    s"[WhatDoYouWantToAmendStandard] cisId=$cisId, taxYear=$year, taxMonth=$month: " +
                      s"$unresolved of ${monthlyReturn.monthlyReturnItems.size} existing items could not be matched to a subcontractor for preselection"
                  )
                }
              }
              ua1                      <- request.userAnswers.set(WhatDoYouWantToAmendStandardPage(id), value).toFuture
              ua2                      <- ua1.set(SelectedSubcontractorPage.all(id), preselectedSubcontractors).toFuture
              saved                    <- sessionRepository.set(ua2)
              result                   <- if (saved) {
                                            continueAmendmentWithId(flowId, value, ua2, preselectedSubcontractors)
                                          } else {
                                            logger.error("[WhatDoYouWantToAmendStandard] Failed to save multi-tab user answers")
                                            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
                                          }
            } yield result
          }
        )
    }

  private def continueAmendmentWithId(
    flowId: UUID,
    value: WhatDoYouWantToAmendStandard,
    userAnswers: UserAnswers,
    preselectedSubcontractors: Map[Int, SelectedSubcontractor]
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val id = flowId.toString

    value match {
      case WhatDoYouWantToAmendStandard.AmendToNilReturn =>
        // TODO: Replace with AreYouSureYouWantToAmendYesNoController.onPageLoadWithId(flowId) when available.
        logger.warn(
          "[WhatDoYouWantToAmendStandardController] Temporary redirect to JourneyRecovery: multi-tab standard-to-nil amendment endpoint not yet implemented"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case WhatDoYouWantToAmendStandard.AmendPaymentOrSubcontractorDetails if preselectedSubcontractors.isEmpty =>
        // TODO: Replace with WhichSubcontractorsToAddController.onPageLoadWithId(flowId, NormalMode) when available.
        logger.warn(
          "[WhatDoYouWantToAmendStandardController] Temporary redirect to JourneyRecovery: multi-tab subcontractor selection endpoint not yet implemented"
        )
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

      case WhatDoYouWantToAmendStandard.AmendPaymentOrSubcontractorDetails =>
        amendMonthlyReturnService.startStandardAmendment(userAnswers, id).flatMap {
          case Left(error) =>
            logger.error(s"[WhatDoYouWantToAmendStandard] Failed to start standard amendment: $error")
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Right(_) =>
            val subcontractorIds = userAnswers
              .get(SelectedSubcontractorPage.all(id))
              .fold(Set.empty[String])(_.values.map(_.id.toString).toSet)

            for {
              ua3   <- Future.fromTry(userAnswers.set(WhichSubcontractorsToAddPage(id), subcontractorIds))
              // TODO: Add UUID-scoped Final Validation support when multi-tab Final Validation is implemented.
              ua4   <- Future.fromTry(
                         ua3
                           .remove(FinalValidationDraftIdPage)
                           .flatMap(_.remove(MonthlyFinalValidationContinuationPage))
                           .flatMap(
                             _.set(
                               MonthlyFinalValidationSourcePage,
                               MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
                             )
                           )
                       )
              saved <- sessionRepository.set(ua4)
            } yield
              if (saved) {
                // TODO: Replace with SubcontractorDetailsAddedController.onPageLoadWithId(flowId, NormalMode) when available.
                logger.warn(
                  "[WhatDoYouWantToAmendStandardController] Temporary redirect to JourneyRecovery: multi-tab subcontractor details added endpoint not yet implemented"
                )
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              } else {
                logger.error("[WhatDoYouWantToAmendStandard] Failed to save multi-tab amendment details")
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
        }
    }
  }
}
