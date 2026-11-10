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
import models.finalvalidation.MonthlyFinalValidationSource
import models.{EmployerReference, JourneyScope, NormalMode, UserAnswers}
import models.monthlyreturns.ContinueReturnJourneyQueryParams
import models.requests.{CisPath, GetMonthlyReturnForEditRequest, IdentifierRequest}
import pages.agent.AgentClientDataPage
import pages.finalvalidations.MonthlyFinalValidationSourcePage

import javax.inject.Inject
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, PRECONDITION_FAILED}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{FormpRdsReconcileService, JourneyService, MonthlyReturnService}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.control.NonFatal

class ContinueAmendReturnJourneyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  monthlyReturnService: MonthlyReturnService,
  formpRdsReconcileService: FormpRdsReconcileService,
  reconcileFormPAndRds: ReconcileFormPAndRdsAction,
  resolveScheme: SchemeAction,
  journeyService: JourneyService,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def continueAmendReturnJourney(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] =
    identify.async { implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth,
        isAmendment = true
      )

      monthlyReturnService
        .populateUserAnswersForContinueAmendJourney(
          UserAnswers(request.userId),
          editRequest
        )
        .flatMap {
          case Left(error) =>
            logger.warn(
              s"[continueAmendReturnJourney] Failed to populate user answers: $error for request: $editRequest"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Right(result) =>
            val redirect      =
              (queryParams.isOriginalNilReturn.getOrElse(false), result.isNilReturn, result.hasSubcontractors) match {
                case (_, false, true)  =>
                  Redirect(
                    controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode)
                  )
                case (_, false, false) =>
                  Redirect(controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad())
                case (true, true, _)   =>
                  Redirect(controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad())
                case _                 =>
                  Redirect(controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad())
              }
            val withAgentData =
              if (request.isAgent)
                monthlyReturnService.populateAgentClientDataIfRequired(
                  ua = result.userAnswers,
                  userId = request.userId,
                  isAgent = true
                )
              else Future.successful(result.userAnswers)

            withAgentData
              .flatMap { uaWithAgentData =>
                val employerRef =
                  if (request.isAgent)
                    uaWithAgentData
                      .get(AgentClientDataPage)
                      .map(d => EmployerReference(d.taxOfficeNumber, d.taxOfficeReference))
                  else
                    request.employerReference

                val answersWithSource =
                  if (!result.isNilReturn && result.hasSubcontractors) {
                    uaWithAgentData.set(
                      MonthlyFinalValidationSourcePage,
                      MonthlyFinalValidationSource.WhichSubcontractorsToAdd(
                        NormalMode.toString
                      )
                    )
                  } else {
                    Success(uaWithAgentData)
                  }

                Future
                  .fromTry(answersWithSource)
                  .flatMap { withSource =>
                    sessionRepository.set(withSource).flatMap { _ =>
                      reconcileFormpRds(queryParams.instanceId, employerRef, redirect)
                    }
                  }
              }
              .recover { case NonFatal(ex) =>
                logger.error("[ContinueAmendReturnJourneyController] Failed to save resumed amendment journey", ex)
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
              }
        }
    }

  private def reconcileFormpRds(
    instanceId: String,
    employerReference: Option[EmployerReference],
    redirect: => Result
  )(implicit request: IdentifierRequest[?]): Future[Result] =
    employerReference match {
      case Some(ref) =>
        formpRdsReconcileService
          .reconcile(instanceId, ref.taxOfficeNumber, ref.taxOfficeReference)
          .map(_ => redirect)
          .recover {
            case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
              logger.warn(
                s"[ContinueAmendReturnJourneyController] Contractor known facts missing in RDS (status ${e.statusCode})"
              )
              Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad())
            case NonFatal(e)                                                                                  =>
              logger.error(
                s"[ContinueAmendReturnJourneyController] FormP/RDS reconciliation failed: ${e.getMessage}",
                e
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case None      =>
        logger.warn("[ContinueAmendReturnJourneyController] Missing employer reference for FormP/RDS reconciliation")
        Future.successful(
          Redirect(
            if (request.isAgent) controllers.routes.UnauthorisedAgentAffinityController.onPageLoad()
            else controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()
          )
        )
    }

  def continueAmendReturnJourneyWithCisPath(
    cisPath: CisPath,
    queryParams: ContinueReturnJourneyQueryParams
  ): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen reconcileFormPAndRds).async { implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth,
        isAmendment = true
      )

      journeyService
        .create(request)
        .flatMap { flowId =>
          val id     = JourneyScope.subJourneyId(request.cisPath, flowId)
          val userId = request.identifier.userId

          sessionRepository.get(userId).flatMap { existing =>
            monthlyReturnService
              .populateUserAnswersForContinueAmendJourney(
                existing.getOrElse(UserAnswers(userId)),
                id,
                editRequest
              )
              .flatMap {
                case Left(error) =>
                  logger.warn(
                    s"[continueAmendReturnJourneyWithCisPath] Failed to populate user answers: $error for request: $editRequest"
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

                case Right(result) =>
                  // TODO: Temp redirect until the standard amend pages have ID-scoped routes. Real targets noted below.
                  val tempRedirect =
                    Redirect(
                      controllers.amend.routes.WhatDoYouWantToAmendNilController
                        .onPageLoadWithId(request.cisPath, flowId)
                    )

                  val redirect =
                    (
                      queryParams.isOriginalNilReturn.getOrElse(false),
                      result.isNilReturn,
                      result.hasSubcontractors
                    ) match {
                      case (_, false, true)  =>
                        // SubcontractorDetailsAddedController.onPageLoadWithId(request.cisPath, flowId, NormalMode)
                        tempRedirect
                      case (_, false, false) =>
                        // WhatDoYouWantToAmendStandardController.onPageLoadWithId(request.cisPath, flowId)
                        tempRedirect
                      case (true, true, _)   =>
                        Redirect(
                          controllers.amend.routes.WhatDoYouWantToAmendNilController
                            .onPageLoadWithId(request.cisPath, flowId)
                        )
                      case _                 =>
                        // WhatDoYouWantToAmendStandardController.onPageLoadWithId(request.cisPath, flowId)
                        tempRedirect
                    }

                  val answersWithSource =
                    if (!result.isNilReturn && result.hasSubcontractors) {
                      result.userAnswers.set(
                        MonthlyFinalValidationSourcePage(id),
                        MonthlyFinalValidationSource.WhichSubcontractorsToAdd(
                          NormalMode.toString
                        )
                      )
                    } else {
                      Success(result.userAnswers)
                    }

                  Future
                    .fromTry(answersWithSource)
                    .flatMap { withSource =>
                      sessionRepository.set(withSource).map(_ => redirect)
                    }
                    .recover { case NonFatal(ex) =>
                      logger.error(
                        "[ContinueAmendReturnJourneyController] Failed to save resumed amendment journey",
                        ex
                      )
                      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    }
              }
          }
        }
        .recover { case NonFatal(ex) =>
          logger.error("[ContinueAmendReturnJourneyController] Failed to start amend journey", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }
}
