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
import models.{ReturnType, UserAnswers}
import models.amend.{AmendmentDetails, CreateAmendedMonthlyReturnRequest}
import models.monthlyreturns.CisTaxpayer
import models.requests.IdentifierRequest
import pages.agent.AgentClientDataPage
import pages.amend.{AmendmentDetailsPage, ConfirmAmendmentPage}
import pages.monthlyreturns.*
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, CisTaxpayerService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.ConfirmAmendmentView

import java.time.*
import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

private case class AmendmentHandoffContext(
  amendmentDetails: AmendmentDetails,
  amendedReturnType: ReturnType
)

class ConfirmAmendmentController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  monthlyReturnService: MonthlyReturnService,
  cisTaxpayerService: CisTaxpayerService,
  appConfig: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmAmendmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  // Placeholder the backend substitutes when the scheme has no name (GetSubmittedMonthlyReturnsDataResponse)
  private val NoNameProvidedPlaceholder = "No name provided"

  private val firstDate: Instant  = Instant.parse("2016-02-06T00:00:00Z")
  private val secondDate: Instant = Instant.parse("2016-04-06T00:00:00Z")

  private def isInWarningPeriod(acceptedTime: Option[String]): Boolean =
    acceptedTime
      .flatMap(t => Try(Instant.parse(t)).toOption)
      .exists(instant => instant.isAfter(firstDate) && instant.isBefore(secondDate))

  def onPageLoad(handoffId: String): Action[AnyContent] = identify.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    amendMonthlyReturnService
      .getAmendmentHandoff(handoffId)
      .flatMap {
        case Some(handoff) =>
          val contractorName =
            Some(handoff.contractorName.trim).filter(n => n.nonEmpty && n != NoNameProvidedPlaceholder)

          val amendmentDetails = AmendmentDetails(
            instanceId = handoff.instanceId,
            taxYear = handoff.taxYear,
            taxMonth = handoff.taxMonth,
            contractorName = contractorName.getOrElse(""),
            originalReturnType = handoff.originalReturnType,
            acceptedTime = handoff.acceptedTime
          )

          ReturnType.amendedFrom(handoff.originalReturnType) match {
            case Some(amendedReturnType) =>
              val updatedUserAnswers =
                UserAnswers(request.userId)
                  .set(CisIdPage, handoff.instanceId)
                  .flatMap(ua => contractorName.fold(Try(ua))(name => ua.set(ContractorNamePage, name)))
                  .flatMap(_.set(ReturnTypePage, amendedReturnType))
                  .flatMap(_.set(AmendmentDetailsPage, amendmentDetails))
                  .flatMap(
                    _.set(DateConfirmPaymentsPage, LocalDate.of(amendmentDetails.taxYear, amendmentDetails.taxMonth, 5))
                  )
                  .get

              val showWarning = isInWarningPeriod(handoff.acceptedTime)

              if (request.isAgent) {
                monthlyReturnService.getAgentClient(request.userId).flatMap {
                  case Some(agentData) =>
                    for {
                      uaWithAgentClientData <- Future.fromTry(
                                                 updatedUserAnswers.set(AgentClientDataPage, agentData)
                                               )
                      _                     <- sessionRepository.set(uaWithAgentClientData)
                    } yield Ok(view(showWarning))

                  case None =>
                    logger.warn("[ConfirmAmendmentController] Agent data not found")
                    Future.successful(
                      Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    )
                }
              } else {
                sessionRepository.set(updatedUserAnswers).map { _ =>
                  Ok(view(showWarning))
                }
              }

            case None =>
              logger.warn(
                s"[ConfirmAmendmentController] Unexpected originalReturnType in handoff: ${handoff.originalReturnType}"
              )
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }

        case None =>
          logger.warn(s"[ConfirmAmendmentController] No handoff found for handoffId: $handoffId")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit: Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      request.userAnswers.flatMap(_.get(AmendmentDetailsPage)) match {
        case Some(amendmentDetails) =>
          implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

          val createRequest = CreateAmendedMonthlyReturnRequest(
            instanceId = amendmentDetails.instanceId,
            taxYear = amendmentDetails.taxYear,
            taxMonth = amendmentDetails.taxMonth,
            version = 0
          )

          // A repeat POST (double click, back then resubmit) must not amend the same return again
          val alreadyAmended = request.userAnswers.flatMap(_.get(ConfirmAmendmentPage)).contains(true)

          (
            for {
              _             <- if (alreadyAmended) Future.unit
                               else amendMonthlyReturnService.createAmendedMonthlyReturn(createRequest)
              updatedAnswers = request.userAnswers.get.set(ConfirmAmendmentPage, true).get
              _             <- sessionRepository.set(updatedAnswers)
            } yield amendmentDetails.originalReturnType match {
              case ReturnType.MonthlyStandardReturn =>
                Redirect(
                  controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoad()
                )
              case ReturnType.MonthlyNilReturn      =>
                Redirect(
                  controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad()
                )
              case unexpected                       =>
                logger.warn(s"[ConfirmAmendmentController] Unexpected original return type: $unexpected")
                Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }
          ).recover { case ex =>
            logger.error(
              s"[ConfirmAmendmentController] Failed to create amended monthly return for instanceId ${amendmentDetails.instanceId}",
              ex
            )
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

        case None =>
          logger.warn(
            s"[ConfirmAmendmentController] AmendmentDetails missing from userAnswers"
          )
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  // Multi-tab
  def onPageLoadMultiTab(handoffId: String): Action[AnyContent] = identify.async { implicit request =>
    if (!appConfig.multiTabEnabled) {
      Future.successful(Redirect(routes.ConfirmAmendmentController.onPageLoad(handoffId)))
    } else {
      getAmendmentHandoffDetails(handoffId)
        .map {
          case Some(context) =>
            val showWarning = isInWarningPeriod(context.amendmentDetails.acceptedTime)
            Ok(view(showWarning, Some(routes.ConfirmAmendmentController.onSubmitMultiTab(handoffId))))
          case None          =>
            Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
        .recover { case ex =>
          logger.error("[ConfirmAmendmentController] Failed to retrieve amendment handoff", ex)
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }

  def onSubmitMultiTab(handoffId: String): Action[AnyContent] = identify.async { implicit request =>
    if (!appConfig.multiTabEnabled) {
      Future.successful(Redirect(routes.ConfirmAmendmentController.onPageLoad(handoffId)))
    } else {
      getAmendmentHandoffDetails(handoffId)
        .flatMap {
          case Some(context) =>
            resolveTaxpayer(context.amendmentDetails.instanceId).flatMap {
              case Some(taxpayer) =>
                createIdScopedAmendment(request.userId, request.isAgent, context, taxpayer)
              case None           =>
                logger.warn(
                  s"[ConfirmAmendmentController] CIS taxpayer not found for instanceId: ${context.amendmentDetails.instanceId}"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
          case None          =>
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
        .recover { case ex =>
          logger.error(
            s"[ConfirmAmendmentController] Failed to create amended monthly return for handoffId: $handoffId",
            ex
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }

  private def getAmendmentHandoffDetails(
    handoffId: String
  )(implicit hc: HeaderCarrier): Future[Option[AmendmentHandoffContext]] =
    amendMonthlyReturnService
      .getAmendmentHandoff(handoffId)
      .map {
        case Some(handoff) =>
          ReturnType.amendedFrom(handoff.originalReturnType) match {
            case Some(amendedReturnType) =>
              val contractorName =
                Some(handoff.contractorName.trim).filter(n => n.nonEmpty && n != NoNameProvidedPlaceholder)

              val amendmentDetails = AmendmentDetails(
                instanceId = handoff.instanceId,
                taxYear = handoff.taxYear,
                taxMonth = handoff.taxMonth,
                contractorName = contractorName.getOrElse(""),
                originalReturnType = handoff.originalReturnType,
                acceptedTime = handoff.acceptedTime
              )

              Some(AmendmentHandoffContext(amendmentDetails, amendedReturnType))

            case None =>
              logger.warn(
                s"[ConfirmAmendmentController] Unexpected originalReturnType in handoff: ${handoff.originalReturnType}"
              )
              None
          }

        case None =>
          logger.warn(s"[ConfirmAmendmentController] No handoff found for handoffId: $handoffId")
          None
      }

  private def resolveTaxpayer[A](cisId: String)(implicit request: IdentifierRequest[A]): Future[Option[CisTaxpayer]] =
    if (request.isAgent) {
      cisTaxpayerService
        .findForAgent(cisId)
        .map(_.filter(_.uniqueId == cisId))
    } else {
      cisTaxpayerService.findForOrganisation
        .map(taxpayer => Option.when(taxpayer.uniqueId == cisId)(taxpayer))
    }

  private def buildIdScopedUserAnswers(
    userAnswers: UserAnswers,
    id: String,
    context: AmendmentHandoffContext,
    agentData: Option[AgentClientData]
  ): Try[UserAnswers] = {
    val amendmentDetails = context.amendmentDetails

    userAnswers
      .set(CisIdPage(id), amendmentDetails.instanceId)
      .flatMap(ua =>
        Option(amendmentDetails.contractorName)
          .filter(_.nonEmpty)
          .fold(Try(ua))(name => ua.set(ContractorNamePage(id), name))
      )
      .flatMap(_.set(ReturnTypePage(id), context.amendedReturnType))
      .flatMap(_.set(AmendmentDetailsPage(id), amendmentDetails))
      .flatMap(
        _.set(DateConfirmPaymentsPage(id), LocalDate.of(amendmentDetails.taxYear, amendmentDetails.taxMonth, 5))
      )
      .flatMap(ua => agentData.fold(Try(ua))(data => ua.set(AgentClientDataPage(id), data)))
  }

  private def redirectToAmendment(flowId: UUID, originalReturnType: ReturnType): Result =
    originalReturnType match {
      case ReturnType.MonthlyStandardReturn =>
        Redirect(controllers.amend.routes.WhatDoYouWantToAmendStandardController.onPageLoadWithId(flowId))
      case ReturnType.MonthlyNilReturn      =>
        // TODO: Replace with WhatDoYouWantToAmendNilController.onPageLoadWithId(flowId) when available.
        logger.warn(
          "[ConfirmAmendmentController] Temporary redirect to JourneyRecovery: multi-tab nil amendment endpoint not yet implemented"
        )
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case unexpected                       =>
        logger.warn(s"[ConfirmAmendmentController] Unexpected original return type: $unexpected")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  private def createIdScopedAmendment(
    userId: String,
    isAgent: Boolean,
    context: AmendmentHandoffContext,
    taxpayer: CisTaxpayer
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val flowId           = UUID.randomUUID()
    val id               = flowId.toString
    val amendmentDetails = context.amendmentDetails

    val agentData =
      if (isAgent) {
        Some(AgentClientData(taxpayer.uniqueId, taxpayer.taxOfficeNumber, taxpayer.taxOfficeRef, taxpayer.schemeName))
      } else {
        None
      }

    val createRequest = CreateAmendedMonthlyReturnRequest(
      instanceId = amendmentDetails.instanceId,
      taxYear = amendmentDetails.taxYear,
      taxMonth = amendmentDetails.taxMonth,
      version = 0
    )

    sessionRepository.get(userId).flatMap { existing =>
      val userAnswers        = existing.getOrElse(UserAnswers(userId))
      val updatedUserAnswers = buildIdScopedUserAnswers(userAnswers, id, context, agentData)

      for {
        updatedAnswers <- Future.fromTry(updatedUserAnswers)
        _              <- amendMonthlyReturnService.createAmendedMonthlyReturn(createRequest)
        saved          <- sessionRepository.set(updatedAnswers)
      } yield
        if (saved) {
          redirectToAmendment(flowId, amendmentDetails.originalReturnType)
        } else {
          logger.error("[ConfirmAmendmentController] Failed to save multi-tab amendment user answers")
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }
  }
}
