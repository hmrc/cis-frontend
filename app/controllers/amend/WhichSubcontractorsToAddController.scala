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
import models.requests.CisPath
import forms.amend.WhichSubcontractorsToAddFormProvider
import models.Mode
import models.amend.WhichSubcontractorsToAdd
import models.finalvalidation.MonthlyFinalValidationSource
import models.monthlyreturns.SelectedSubcontractor
import navigation.Navigator
import pages.amend.WhichSubcontractorsToAddPage
import pages.finalvalidations.{FinalValidationDraftIdPage, MonthlyFinalValidationContinuationPage, MonthlyFinalValidationSourcePage}
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, OriginalSubcontractorCountPage, ReturnTypePage, SelectedSubcontractorPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.WhichSubcontractorsToAddView

import javax.inject.Inject
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class WhichSubcontractorsToAddController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
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
        cisId      <- ua.get(CisIdPage)
        taxDate    <- ua.get(DateConfirmPaymentsPage)
        returnType <- ua.get(ReturnTypePage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers
        .map { case (cisId, taxMonth, taxYear, isAmendment) =>
          monthlyReturnService.isEditable(cisId, taxMonth, taxYear, isAmendment).flatMap {
            case true  =>
              subcontractorService
                .buildAmendWhichSubcontractorsPage(
                  cisId,
                  taxMonth,
                  taxYear,
                  Some(request.userAnswers),
                  isAmendment = isAmendment
                )
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
            case false =>
              logger.error(
                "[WhichSubcontractorsToAddController] Resource not editable"
              )
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
        .getOrElse {
          logger.error(
            s"[WhichSubcontractorsToAddController] Missing or invalid required answers: " +
              s"CisIdPage present=${ua.get(CisIdPage).isDefined}, " +
              s"DateConfirmPaymentsPage present=${ua.get(DateConfirmPaymentsPage).isDefined}, " +
              s"ReturnTypePage=${ua.get(ReturnTypePage)}"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
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

      requiredAnswers
        .map { case (taxMonth, taxYear, isAmendment) =>
          monthlyReturnService
            .isEditable(cisId, taxMonth, taxYear, isAmendment)
            .flatMap { // TODO DTR-9474: Check if this need updated with id
              case true  =>
                subcontractorService
                  .buildAmendWhichSubcontractorsPage( // TODO DTR-9474: Check if this need updated with id
                    cisId,
                    taxMonth,
                    taxYear,
                    Some(request.userAnswers),
                    isAmendment = isAmendment
                  )
                  .map { model =>
                    val form          = formProvider(model.subcontractors)
                    val selectedIds   = request.userAnswers
                      .get(WhichSubcontractorsToAddPage(subJourneyId))
                      .getOrElse(model.preSelectedIds)
                    val checkboxItems = WhichSubcontractorsToAdd.checkboxItems(model.subcontractors, selectedIds)
                    val formAction    =
                      Some(
                        controllers.amend.routes.WhichSubcontractorsToAddController
                          .onSubmitWithId(cisPath, flowId, mode)
                      )
                    Ok(view(form, mode, checkboxItems, formAction))
                  }
                  .recover { case ex =>
                    logger.error(
                      s"[WhichSubcontractorsToAddController] Failed to load subcontractors: ${ex.getMessage}",
                      ex
                    )
                    Redirect(controllers.routes.SystemErrorController.onPageLoad())
                  }
              case false =>
                logger.error(
                  "[WhichSubcontractorsToAddController] Resource not editable"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
        }
        .getOrElse {
          logger.error(
            s"[WhichSubcontractorsToAddController] Missing or invalid required answers: " +
              s"CisIdPage present=${ua.get(CisIdPage).isDefined}, " +
              s"DateConfirmPaymentsPage present=${ua.get(DateConfirmPaymentsPage(subJourneyId)).isDefined}, " +
              s"ReturnTypePage=${ua.get(ReturnTypePage(subJourneyId))}"
          )
          Future.successful(
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          )
        }

    }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>
      val requiredAnswers = for {
        cisId      <- request.userAnswers.get(CisIdPage)
        taxDate    <- request.userAnswers.get(DateConfirmPaymentsPage)
        returnType <- request.userAnswers.get(ReturnTypePage)
      } yield (cisId, taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers
        .map { case (cisId, taxMonth, taxYear, isAmendment) =>
          subcontractorService
            .buildAmendWhichSubcontractorsPage(
              cisId,
              taxMonth,
              taxYear,
              Some(request.userAnswers),
              isAmendment = isAmendment
            )
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
                        val selectedSubcontractors              = model.subcontractors
                          .filter(subcontractor => value.contains(subcontractor.id))
                          .map { subcontractor =>
                            val existingSelectedSubcontractor =
                              request.userAnswers
                                .get(SelectedSubcontractorPage.all)
                                .flatMap { subcontractors =>
                                  subcontractors.values.find(_.id == subcontractor.id.toLong)
                                }

                            SelectedSubcontractor(
                              id = subcontractor.id.toLong,
                              name = subcontractor.name,
                              totalPaymentsMade = existingSelectedSubcontractor.flatMap(_.totalPaymentsMade),
                              costOfMaterials = existingSelectedSubcontractor.flatMap(_.costOfMaterials),
                              totalTaxDeducted = existingSelectedSubcontractor.flatMap(_.totalTaxDeducted)
                            )
                          }
                        val selectedSubcontractorIds: Set[Long] = value.map(_.toLong)

                        for {
                          ua                  <- Future.fromTry(request.userAnswers.set(WhichSubcontractorsToAddPage, value))
                          ua2                 <- Future.fromTry {
                                                   val cleared = ua.remove(SelectedSubcontractorPage.all)
                                                   cleared
                                                     .flatMap { clearedAnswers =>
                                                       selectedSubcontractors.zipWithIndex.foldLeft(Try(clearedAnswers)) {
                                                         case (answersTry, (subcontractor, index)) =>
                                                           answersTry.flatMap(
                                                             _.set(SelectedSubcontractorPage(index + 1), subcontractor)
                                                           )
                                                       }
                                                     }
                                                     .flatMap(_.set(OriginalSubcontractorCountPage, model.subcontractors.size))
                                                 }
                          withoutDraft        <- Future.fromTry(ua2.remove(FinalValidationDraftIdPage))
                          withoutContinuation <-
                            Future.fromTry(withoutDraft.remove(MonthlyFinalValidationContinuationPage))
                          withSource          <- Future.fromTry(
                                                   withoutContinuation.set(
                                                     MonthlyFinalValidationSourcePage,
                                                     MonthlyFinalValidationSource.WhichSubcontractorsToAdd(mode.toString)
                                                   )
                                                 )
                          _                   <- sessionRepository.set(withSource)
                          _                   <- monthlyReturnService.syncMonthlyReturnItems(withSource, selectedSubcontractorIds.toSeq)
                        } yield Redirect(navigator.nextPage(WhichSubcontractorsToAddPage, mode, withSource))
                    )

                case unexpectedStatus =>
                  val statusText = unexpectedStatus.getOrElse("missing")
                  logger.error(
                    s"[WhichSubcontractorsToAddController] Unexpected return status: $statusText. " +
                      "Expected STARTED or VALIDATED."
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
            }
            .recover { case ex =>
              logger.error(s"[WhichSubcontractorsToAddController] Submit failed: ${ex.getMessage}", ex)
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
        }
        .getOrElse {
          logger.error(
            s"[WhichSubcontractorsToAddController] Missing or invalid required answers: " +
              s"CisIdPage present=${request.userAnswers.get(CisIdPage).isDefined}, " +
              s"DateConfirmPaymentsPage present=${request.userAnswers.get(DateConfirmPaymentsPage).isDefined}, " +
              s"ReturnTypePage=${request.userAnswers.get(ReturnTypePage)}"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
  }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val cisId        = request.cisId
      val subJourneyId = request.subJourneyId

      val requiredAnswers = for {
        taxDate    <- request.userAnswers.get(DateConfirmPaymentsPage)
        returnType <- request.userAnswers.get(ReturnTypePage)
      } yield (taxDate.getMonthValue, taxDate.getYear, returnType.amendmentFlag == "Y")

      requiredAnswers
        .map { case (taxMonth, taxYear, isAmendment) =>
          subcontractorService
            .buildAmendWhichSubcontractorsPage( // TODO DTR-9474: Check if this need updated with id
              cisId,
              taxMonth,
              taxYear,
              Some(request.userAnswers),
              isAmendment = isAmendment
            )
            .flatMap { model =>
              model.status match {
                case Some("STARTED") | Some("VALIDATED") =>
                  val form       = formProvider(model.subcontractors)
                  val formAction =
                    Some(
                      controllers.amend.routes.WhichSubcontractorsToAddController.onSubmitWithId(cisPath, flowId, mode)
                    )
                  form
                    .bindFromRequest()
                    .fold(
                      formWithErrors =>
                        Future.successful(
                          BadRequest(
                            view(
                              formWithErrors,
                              mode,
                              WhichSubcontractorsToAdd.checkboxItems(model.subcontractors),
                              formAction
                            )
                          )
                        ),
                      value =>
                        val selectedSubcontractors              = model.subcontractors
                          .filter(subcontractor => value.contains(subcontractor.id))
                          .map { subcontractor =>
                            val existingSelectedSubcontractor =
                              request.userAnswers
                                .get(SelectedSubcontractorPage.all(Some(subJourneyId)))
                                .flatMap { subcontractors =>
                                  subcontractors.values.find(_.id == subcontractor.id.toLong)
                                }

                            SelectedSubcontractor(
                              id = subcontractor.id.toLong,
                              name = subcontractor.name,
                              totalPaymentsMade = existingSelectedSubcontractor.flatMap(_.totalPaymentsMade),
                              costOfMaterials = existingSelectedSubcontractor.flatMap(_.costOfMaterials),
                              totalTaxDeducted = existingSelectedSubcontractor.flatMap(_.totalTaxDeducted)
                            )
                          }
                        val selectedSubcontractorIds: Set[Long] = value.map(_.toLong)

                        for {
                          ua                  <-
                            Future.fromTry(request.userAnswers.set(WhichSubcontractorsToAddPage(subJourneyId), value))
                          ua2                 <- Future.fromTry {
                                                   val cleared = ua.remove(SelectedSubcontractorPage.all(Some(subJourneyId)))
                                                   cleared
                                                     .flatMap { clearedAnswers =>
                                                       selectedSubcontractors.zipWithIndex.foldLeft(Try(clearedAnswers)) {
                                                         case (answersTry, (subcontractor, index)) =>
                                                           answersTry.flatMap(
                                                             _.set(
                                                               SelectedSubcontractorPage(index + 1, Some(subJourneyId)),
                                                               subcontractor
                                                             )
                                                           )
                                                       }
                                                     }
                                                     .flatMap(
                                                       _.set(OriginalSubcontractorCountPage(subJourneyId), model.subcontractors.size)
                                                     )
                                                 }
                          withoutDraft        <- Future.fromTry(ua2.remove(FinalValidationDraftIdPage(subJourneyId)))
                          withoutContinuation <-
                            Future.fromTry(withoutDraft.remove(MonthlyFinalValidationContinuationPage(subJourneyId)))
                          withSource          <- Future.fromTry(
                                                   withoutContinuation.set(
                                                     MonthlyFinalValidationSourcePage(subJourneyId),
                                                     MonthlyFinalValidationSource.WhichSubcontractorsToAdd(
                                                       mode.toString
                                                     ) // TODO DTR-9474: Final validation out of scope
                                                   )
                                                 )
                          _                   <- sessionRepository.set(withSource)
                          _                   <- monthlyReturnService.syncMonthlyReturnItems(withSource, selectedSubcontractorIds.toSeq)
                        } yield Redirect(
                          navigator.nextPage(WhichSubcontractorsToAddPage(subJourneyId), mode, withSource)
                        )
                    )

                case unexpectedStatus =>
                  val statusText = unexpectedStatus.getOrElse("missing")
                  logger.error(
                    s"[WhichSubcontractorsToAddController] Unexpected return status: $statusText. " +
                      "Expected STARTED or VALIDATED."
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
              }
            }
            .recover { case ex =>
              logger.error(s"[WhichSubcontractorsToAddController] Submit failed: ${ex.getMessage}", ex)
              Redirect(controllers.routes.SystemErrorController.onPageLoad())
            }
        }
        .getOrElse {
          logger.error(
            s"[WhichSubcontractorsToAddController] Missing or invalid required answers: " +
              s"CisIdPage present=${request.userAnswers.get(CisIdPage).isDefined}, " + // TODO DTR-9474: This page is in a different format to others, should it be converted to case object and then have the case class with id added?
              s"DateConfirmPaymentsPage present=${request.userAnswers.get(DateConfirmPaymentsPage(subJourneyId)).isDefined}, " +
              s"ReturnTypePage=${request.userAnswers.get(ReturnTypePage(subJourneyId))}"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }
}
