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
import models.ReturnType.MonthlyStandardReturn
import models.finalvalidation.MonthlyFinalValidationSource
import models.monthlyreturns.ContinueReturnJourneyQueryParams
import models.{EmployerReference, JourneyScope, NormalMode, UserAnswers}
import models.requests.{CisPath, GetMonthlyReturnForEditRequest, IdentifierRequest}

import javax.inject.Inject
import navigation.Navigator
import pages.agent.AgentClientDataPage
import pages.finalvalidations.MonthlyFinalValidationSourcePage
import pages.monthlyreturns.{DateConfirmPaymentsPage, IsResumedMonthlyReturnPage, ReturnTypePage, ScopedSelectedSubcontractorPage, SelectedSubcontractorPage}
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

class ContinueReturnJourneyController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
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

  def continueReturnJourney(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] =
    identify.async { implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth,
        isAmendment = false
      )

      monthlyReturnService
        .populateUserAnswersForContinueJourney(
          UserAnswers(request.userId),
          editRequest
        )
        .flatMap {
          case Left(error) =>
            logger.error(
              s"[continueReturnJourney] Failed to populate user answers: $error for request: $editRequest"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

          case Right(updatedUserAnswers) =>
            monthlyReturnService
              .populateAgentClientDataIfRequired(
                ua = updatedUserAnswers,
                userId = request.userId,
                isAgent = request.isAgent
              )
              .flatMap { finalUserAnswers =>
                val employerRef       =
                  if (request.isAgent)
                    finalUserAnswers
                      .get(AgentClientDataPage)
                      .map(d => EmployerReference(d.taxOfficeNumber, d.taxOfficeReference))
                  else
                    request.employerReference
                val answersWithSource =
                  if (
                    finalUserAnswers.get(ReturnTypePage).getOrElse(MonthlyStandardReturn) == MonthlyStandardReturn &&
                    finalUserAnswers.get(SelectedSubcontractorPage.all).exists(_.nonEmpty)
                  ) {
                    finalUserAnswers.set(
                      MonthlyFinalValidationSourcePage,
                      MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
                    )
                  } else {
                    Success(finalUserAnswers)
                  }

                val answersWithOrigin =
                  answersWithSource.flatMap(_.set(IsResumedMonthlyReturnPage, true))

                Future.fromTry(answersWithOrigin).flatMap { withSource =>
                  sessionRepository.set(withSource).flatMap { _ =>
                    reconcileFormpRds(
                      queryParams.instanceId,
                      employerRef,
                      Redirect(navigator.nextPage(DateConfirmPaymentsPage, NormalMode, withSource))
                    )
                  }
                }
              }
              .recover { case ex =>
                logger.error(
                  s"[continueReturnJourney] Failed to populate agent client data or save user answers, " +
                    s"request: $editRequest",
                  ex
                )
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
                s"[ContinueReturnJourneyController] Contractor known facts missing in RDS (status ${e.statusCode})"
              )
              Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad())
            case NonFatal(e)                                                                                  =>
              logger.error(
                s"[ContinueReturnJourneyController] FormP/RDS reconciliation failed: ${e.getMessage}",
                e
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case None      =>
        logger.warn("[ContinueReturnJourneyController] Missing employer reference for FormP/RDS reconciliation")
        Future.successful(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()))
    }

  def continueReturnJourneyWithCisPath(
    cisPath: CisPath,
    queryParams: ContinueReturnJourneyQueryParams
  ): Action[AnyContent] = (identify andThen resolveScheme(cisPath) andThen reconcileFormPAndRds).async {
    implicit request =>
      val editRequest = GetMonthlyReturnForEditRequest(
        instanceId = queryParams.instanceId,
        taxYear = queryParams.taxYear,
        taxMonth = queryParams.taxMonth,
        isAmendment = false
      )

      journeyService.create(request).flatMap { flowId =>
        val id     = JourneyScope.subJourneyId(request.cisPath, flowId)
        val userId = request.identifier.userId

        sessionRepository.get(userId).flatMap { existing =>
          monthlyReturnService
            .populateUserAnswersForContinueJourney(
              existing.getOrElse(UserAnswers(userId)),
              id,
              editRequest
            )
            .flatMap {
              case Left(error) =>
                logger.error(
                  s"[continueReturnJourneyWithCisPath] Failed to populate user answers: $error for request: $editRequest"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

              case Right(updatedUserAnswers) =>
                monthlyReturnService
                  .populateAgentClientDataIfRequired(
                    ua = updatedUserAnswers,
                    userId = userId,
                    isAgent = request.identifier.isAgent
                  )
                  .flatMap { finalUserAnswers =>
                    val answersWithSource =
                      if (
                        finalUserAnswers
                          .get(ReturnTypePage(id))
                          .getOrElse(MonthlyStandardReturn) == MonthlyStandardReturn &&
                        finalUserAnswers.get(ScopedSelectedSubcontractorPage.all(id)).exists(_.nonEmpty)
                      ) {
                        finalUserAnswers.set(
                          MonthlyFinalValidationSourcePage(id),
                          MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
                        )
                      } else {
                        Success(finalUserAnswers)
                      }

                    val answersWithOrigin =
                      answersWithSource.flatMap(_.set(IsResumedMonthlyReturnPage(id), true))

                    Future.fromTry(answersWithOrigin).flatMap { withSource =>
                      sessionRepository.set(withSource).map { _ =>
                        // TODO: Temp redirect until the page after DateConfirmPayments has an ID-scoped route.
                        Redirect(
                          routes.DateConfirmPaymentsController.onPageLoadWithId(request.cisPath, flowId, NormalMode)
                        )
                      }
                    }
                  }
                  .recover { case ex =>
                    logger.error(
                      s"[continueReturnJourneyWithCisPath] Failed to populate agent client data or save user answers, " +
                        s"request: $editRequest",
                      ex
                    )
                    Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                  }
            }
        }
      }
  }
}
