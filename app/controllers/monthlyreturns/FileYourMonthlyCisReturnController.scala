/*
 * Copyright 2025 HM Revenue & Customs
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

import config.FrontendAppConfig
import controllers.actions.*
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.{EmployerReference, NormalMode, ReturnType, UserAnswers}
import models.agent.AgentClientData
import models.monthlyreturns.CisTaxpayer
import models.requests.{IdentifierRequest, OptionalDataRequest}
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{CisIdPage, ContractorNamePage, ReturnTypePage}
import play.api.Logging
import play.api.http.Status.{NOT_FOUND, PRECONDITION_FAILED}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.twirl.api.Html
import repositories.SessionRepository
import services.{CisTaxpayerService, FormpRdsReconcileService, MonthlyReturnService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.TypeUtils.toFuture
import views.html.monthlyreturns.FileYourMonthlyCisReturnView
import views.html.monthlyreturns.FileYourNilReturnView
import utils.UserAnswerUtils.*

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success
import scala.util.control.NonFatal

class FileYourMonthlyCisReturnController @Inject() (
  override val messagesApi: MessagesApi,
  val controllerComponents: MessagesControllerComponents,
  monthlyReturnView: FileYourMonthlyCisReturnView,
  nilReturnView: FileYourNilReturnView,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  sessionRepository: SessionRepository,
  monthlyReturnService: MonthlyReturnService,
  formpRdsReconcileService: FormpRdsReconcileService,
  cisTaxpayerService: CisTaxpayerService,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def startMonthlyReturn(): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      startReturn(ReturnType.MonthlyStandardReturn)(monthlyReturnView())
    }

  def startNilReturn(): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      startReturn(ReturnType.MonthlyNilReturn)(nilReturnView())
    }

  def onSubmit(returnType: ReturnType): Action[AnyContent] =
    (identify andThen getData andThen requireData).async { implicit request =>
      (for {
        cleanAnswers <- request.userAnswers.clearMonthlyReturnJourney.toFuture
        _            <- sessionRepository.set(cleanAnswers)
      } yield Redirect(routes.DateConfirmPaymentsController.onPageLoad(NormalMode, Some(returnType))))
        .recover { case ex =>
          logger.error(
            "[FileYourMonthlyCisReturnController][onSubmit] - failed clearing journey or storing answers",
            ex
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  private def startReturn(
    returnType: ReturnType
  )(render: => Html)(implicit request: OptionalDataRequest[AnyContent]): Future[Result] = {
    val instanceIdOpt = request.getQueryString("instanceId")
    val userAnswer    = request.userAnswers.getOrElse(UserAnswers(request.userId))
    for {
      updatedAnswers <- Future.fromTry(userAnswer.set(ReturnTypePage, returnType))
      _              <- sessionRepository.set(updatedAnswers)
      agentData      <- getAgentClient(request)
      result         <- handleRequest(instanceIdOpt, agentData, updatedAnswers, render)
    } yield result
  }

  private def handleRequest(
    instanceIdOpt: Option[String],
    agentDataOpt: Option[AgentClientData],
    userAnswers: UserAnswers,
    render: => Html
  )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    if (!request.isAgent) {
      logger.info(
        s"[FileYourMonthlyCisReturnController] isAgent=${request.isAgent}, " +
          s"instanceIdOpt=$instanceIdOpt, " +
          s"hasContractorName=${userAnswers.get(ContractorNamePage).isDefined}"
      )
      instanceIdOpt match {
        case Some(instanceId) =>
          storeInstanceId(instanceId, userAnswers).flatMap(_ =>
            reconcileFormpRds(instanceId, request.employerReference, render)
          )
        case None             =>
          monthlyReturnService
            .resolveAndStoreCisId(userAnswers, false)
            .flatMap { case (cisId, _) =>
              reconcileFormpRds(cisId, request.employerReference, render)
            }
            .recover { case NonFatal(ex) =>
              logger.error(
                s"[FileYourMonthlyCisReturnController] Failed to resolve CIS ID: ${ex.getMessage}",
                ex
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
      }
    } else {
      (instanceIdOpt, agentDataOpt) match {
        case (maybeInstanceId, Some(agentData)) =>
          val instanceId = maybeInstanceId.getOrElse(agentData.uniqueId)
          handleAgentFlow(instanceId, agentData, userAnswers, render)
        case (Some(_), None)                    =>
          logger.error(s"[FileYourMonthlyCisReturnController] Missing AgentClientData")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case (None, None)                       =>
          logger.error(
            s"[FileYourMonthlyCisReturnController] Missing instanceId and AgentClientData"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def handleAgentFlow(
    instanceId: String,
    agentData: AgentClientData,
    userAnswers: UserAnswers,
    render: => Html
  )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    monthlyReturnService
      .hasClient(agentData.taxOfficeNumber, agentData.taxOfficeReference)
      .flatMap {
        case true  =>
          for {
            uaWithAgent <- storeAgentClientData(agentData, userAnswers)
            _           <- storeInstanceId(instanceId, uaWithAgent)
            result      <- reconcileFormpRds(
                             instanceId,
                             Some(EmployerReference(agentData.taxOfficeNumber, agentData.taxOfficeReference)),
                             render
                           )
          } yield result
        case false =>
          logger.error(
            s"[FileYourMonthlyCisReturnController] hasClient = false for " +
              s"taxOfficeNumber: ${agentData.taxOfficeNumber}, taxOfficeReference: ${agentData.taxOfficeReference}"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
      .recover { case NonFatal(e) =>
        logger.error(s"[FileYourMonthlyCisReturnController] hasClient check failed ${e.getMessage}", e)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }

  private def getAgentClient(implicit
    request: OptionalDataRequest[_],
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Option[AgentClientData]] =
    if (request.isAgent) {
      monthlyReturnService.getAgentClient(request.userId)
    } else {
      Future.successful(None)
    }

  private def reconcileFormpRds(
    instanceId: String,
    employerReference: Option[EmployerReference],
    render: => Html
  )(implicit request: OptionalDataRequest[AnyContent]): Future[Result] =
    employerReference match {
      case Some(ref) =>
        formpRdsReconcileService
          .reconcile(instanceId, ref.taxOfficeNumber, ref.taxOfficeReference)
          .map(_ => Ok(render))
          .recover {
            case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
              logger.warn(
                s"[FileYourMonthlyCisReturnController] Contractor known facts missing in RDS (status ${e.statusCode})"
              )
              Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad())
            case NonFatal(e)                                                                                  =>
              logger.error(
                s"[FileYourMonthlyCisReturnController] FormP/RDS reconciliation failed: ${e.getMessage}",
                e
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }
      case None      =>
        logger.warn("[FileYourMonthlyCisReturnController] Missing tax office reference for FormP/RDS reconciliation")
        Future.successful(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()))
    }

  private def storeInstanceId(instanceId: String, userAnswers: UserAnswers): Future[Unit] =
    for {
      updated <- Future.fromTry(userAnswers.set(CisIdPage, instanceId))
      _       <- sessionRepository.set(updated)
    } yield ()

  private def storeAgentClientData(data: AgentClientData, ua: UserAnswers): Future[UserAnswers] =
    for {
      uaWithAgentClientData <- Future.fromTry(ua.set(AgentClientDataPage, data))
      _                     <- sessionRepository.set(uaWithAgentClientData)
    } yield uaWithAgentClientData

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

  // Multi-tab: New CIS ID-scoped entry points.
  def startNilReturnWithCisId(cisId: String): Action[AnyContent] =
    identify.async { implicit request =>
      withResolvedTaxpayer(cisId) { taxpayer =>
        reconcileScopedReturn(
          taxpayer,
          Ok(nilReturnView(Some(routes.FileYourMonthlyCisReturnController.onSubmitWithCisId(cisId, MonthlyNilReturn))))
        )
      }
        .recover { case NonFatal(ex) =>
          logger.error(s"[FileYourMonthlyCisReturnController][startNilReturnWithCisId] Failed to start journey", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  def startMonthlyReturnWithCisId(cisId: String): Action[AnyContent] =
    identify.async { implicit request =>
      withResolvedTaxpayer(cisId) { taxpayer =>
        reconcileScopedReturn(
          taxpayer,
          Ok(
            monthlyReturnView(
              Some(routes.FileYourMonthlyCisReturnController.onSubmitWithCisId(cisId, MonthlyStandardReturn))
            )
          )
        )
      }
        .recover { case NonFatal(ex) =>
          logger.error(s"[FileYourMonthlyCisReturnController][startMonthlyReturnWithCisId] Failed to start journey", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  // Multi-tab: Create a new UUID-scoped journey on submission.
  def onSubmitWithCisId(cisId: String, returnType: ReturnType): Action[AnyContent] =
    identify.async { implicit request =>
      withResolvedTaxpayer(cisId) { taxpayer =>
        val flowId = UUID.randomUUID()
        val id     = flowId.toString

        sessionRepository
          .get(request.userId)
          .flatMap { existing =>
            val userAnswers = existing.getOrElse(UserAnswers(request.userId))

            val updatedAnswers = for {
              withCisId      <- userAnswers.set(CisIdPage(id), taxpayer.uniqueId)
              withContractor <- withCisId.set(ContractorNamePage(id), taxpayer.schemeName.getOrElse(""))
              withAgent      <- if request.isAgent then
                                  withContractor.set(
                                    AgentClientDataPage(id),
                                    AgentClientData(
                                      taxpayer.uniqueId,
                                      taxpayer.taxOfficeNumber,
                                      taxpayer.taxOfficeRef,
                                      taxpayer.schemeName
                                    )
                                  )
                                else Success(withContractor)
              updated        <- withAgent.set(ReturnTypePage(id), returnType)
            } yield updated

            Future
              .fromTry(updatedAnswers)
              .flatMap { updated =>
                sessionRepository
                  .set(updated)
                  .map { _ =>
                    Redirect(routes.DateConfirmPaymentsController.onPageLoadWithId(flowId, NormalMode))
                  }
              }
          }
      }
        .recover { case NonFatal(ex) =>
          logger.error(s"[FileYourMonthlyCisReturnController][onSubmitWithCisId] Failed to create journey", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

}
