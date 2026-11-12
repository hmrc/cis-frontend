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

import config.FrontendAppConfig
import controllers.actions.*
import models.agent.AgentClientData
import models.finalvalidation.MonthlyFinalValidationSource
import models.{EmployerReference, NormalMode, UserAnswers}
import models.monthlyreturns.{CisTaxpayer, ContinueReturnJourneyQueryParams}
import models.requests.{GetMonthlyReturnForEditRequest, IdentifierRequest}
import pages.agent.AgentClientDataPage
import pages.finalvalidations.MonthlyFinalValidationSourcePage

import javax.inject.Inject
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, PRECONDITION_FAILED}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{CisTaxpayerService, FormpRdsReconcileService, MonthlyReturnService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.control.NonFatal

class ContinueAmendReturnJourneyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  monthlyReturnService: MonthlyReturnService,
  formpRdsReconcileService: FormpRdsReconcileService,
  cisTaxpayerService: CisTaxpayerService,
  appConfig: FrontendAppConfig,
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

  // Multi-tab: Resolve the CIS taxpayer for the selected CIS ID.
  private def resolveTaxpayer[A](cisId: String)(using request: IdentifierRequest[A]): Future[Option[CisTaxpayer]] =
    if request.isAgent then
      cisTaxpayerService
        .findForAgent(cisId)
        .map(_.filter(_.uniqueId == cisId))
    else
      cisTaxpayerService.findForOrganisation
        .map(taxpayer => Option.when(taxpayer.uniqueId == cisId)(taxpayer))

  // Multi-tab: Resolve and authorise the selected CIS taxpayer.
  private def withResolvedTaxpayer[A](cisId: String)(
    handle: CisTaxpayer => Future[Result]
  )(using request: IdentifierRequest[A]): Future[Result] =
    resolveTaxpayer(cisId).flatMap {
      case Some(taxpayer) if !appConfig.multiTabEnabled =>
        Future.successful(Redirect(appConfig.returnsLandingPageUrl(taxpayer.uniqueId, None)))

      case Some(taxpayer) =>
        handle(taxpayer)

      case None =>
        Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  // Multi-tab: Reconcile FormP/RDS before rendering the entry page.
  private def reconcileScopedReturn(taxpayer: CisTaxpayer, render: => Result)(using HeaderCarrier): Future[Result] =
    formpRdsReconcileService
      .reconcile(taxpayer.uniqueId, taxpayer.taxOfficeNumber, taxpayer.taxOfficeRef)
      .map(_ => render)
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
          logger.warn(s"Contractor known facts missing in RDS (status ${e.statusCode})")
          Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad())
        case NonFatal(e)                                                                                  =>
          logger.error(s"FormP/RDS reconciliation failed: ${e.getMessage}", e)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }

  // Multi-tab: CIS ID-scoped copy of continueAmendReturnJourney.
  def continueAmendReturnJourneyWithCisId(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] =
    identify.async { implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth,
        isAmendment = true
      )

      withResolvedTaxpayer(queryParams.instanceId) { taxpayer =>
        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        sessionRepository
          .get(request.userId)
          .flatMap { existing =>
            monthlyReturnService
              .populateUserAnswersForContinueAmendJourney(
                existing.getOrElse(UserAnswers(request.userId)),
                id,
                editRequest
              )
              .flatMap {
                case Left(error) =>
                  logger.warn(
                    s"[continueAmendReturnJourneyWithCisId] Failed to populate user answers: $error for request: $editRequest"
                  )
                  Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

                case Right(result) =>
                  // TODO: Temp redirects until SubcontractorDetailsAdded and WhatDoYouWantToAmendStandard have WithId routes.
                  val tempRedirect =
                    Redirect(controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(flowId))

                  val redirect =
                    (
                      queryParams.isOriginalNilReturn.getOrElse(false),
                      result.isNilReturn,
                      result.hasSubcontractors
                    ) match {
                      case (_, false, true)  => tempRedirect
                      case (_, false, false) => tempRedirect
                      case (true, true, _)   => tempRedirect // genuine WhatDoYouWantToAmendNil route
                      case _                 => tempRedirect
                    }

                  val withAgentData =
                    if (request.isAgent)
                      result.userAnswers.set(
                        AgentClientDataPage(id),
                        AgentClientData(
                          taxpayer.uniqueId,
                          taxpayer.taxOfficeNumber,
                          taxpayer.taxOfficeRef,
                          taxpayer.schemeName
                        )
                      )
                    else Success(result.userAnswers)

                  val answersWithSource =
                    withAgentData.flatMap { ua =>
                      if (!result.isNilReturn && result.hasSubcontractors) {
                        ua.set(
                          MonthlyFinalValidationSourcePage(id),
                          MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
                        )
                      } else {
                        Success(ua)
                      }
                    }

                  Future.fromTry(answersWithSource).flatMap { withSource =>
                    sessionRepository.set(withSource).flatMap { _ =>
                      reconcileScopedReturn(taxpayer, redirect)
                    }
                  }
              }
          }
      }
        .recover { case NonFatal(ex) =>
          logger.error(s"[continueAmendReturnJourneyWithCisId] Failed to start journey for request: $editRequest", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }
}
