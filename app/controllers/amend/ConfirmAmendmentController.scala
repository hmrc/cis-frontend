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
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.{ReturnType, UserAnswers}
import models.amend.{AmendmentDetails, CreateAmendedMonthlyReturnRequest}
import models.monthlyreturns.{ContinueReturnJourneyQueryParams, MonthlyReturn, Submission}
import models.requests.OptionalDataRequest
import pages.agent.AgentClientDataPage
import pages.amend.{AmendmentDetailsPage, ConfirmAmendmentPage}
import pages.monthlyreturns.CisIdPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.ConfirmAmendmentView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmAmendmentController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  sessionRepository: SessionRepository,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmAmendmentView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(queryParams: ContinueReturnJourneyQueryParams): Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      validateUserCanAccessRequest(queryParams.instanceId)
        .flatMap {
          case true =>
            retrieveAndStoreAmendmentDetails(queryParams).map(_ => Ok(view()))

          case false =>
            logger.warn(
              s"[ConfirmAmendmentController] User ${request.userId} attempted to access instanceId ${queryParams.instanceId} " +
                s"which they are not authorised to access"
            )
            Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
        .recover { case ex =>
          logger.warn(
            s"[ConfirmAmendmentController] Failed to validate access for instanceId ${queryParams.instanceId}",
            ex
          )
          Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    }

  def onSubmit: Action[AnyContent] =
    (identify andThen getData).async { implicit request =>
      request.userAnswers.flatMap(_.get(AmendmentDetailsPage)) match {
        case Some(amendmentDetails) =>
          implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

          val queryParams = ContinueReturnJourneyQueryParams(
            instanceId = amendmentDetails.instanceId,
            taxYear = amendmentDetails.taxYear,
            taxMonth = amendmentDetails.taxMonth
          )

          validateUserCanAccessRequest(queryParams.instanceId)
            .flatMap {
              case true =>
                val createRequest = CreateAmendedMonthlyReturnRequest(
                  instanceId = amendmentDetails.instanceId,
                  taxYear = amendmentDetails.taxYear,
                  taxMonth = amendmentDetails.taxMonth,
                  version = 0
                )

                for {
                  _             <- amendMonthlyReturnService.createAmendedMonthlyReturn(createRequest)
                  updatedAnswers =
                    request.userAnswers.getOrElse(UserAnswers(request.userId)).set(ConfirmAmendmentPage, true).get
                  _             <- sessionRepository.set(updatedAnswers)
                } yield Redirect(
                  controllers.amend.routes.ConfirmAmendmentController.onPageLoad(queryParams)
                ) // TODO: DTR-4657

              case false =>
                logger.warn(
                  s"[ConfirmAmendmentController][onSubmit] User ${request.userId} attempted to submit for instanceId ${queryParams.instanceId} " +
                    s"which they are not authorised to access"
                )
                Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            }
            .recover { case ex =>
              logger.warn(
                s"[ConfirmAmendmentController][onSubmit] Failed to validate access for instanceId ${queryParams.instanceId}",
                ex
              )
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            }

        case None =>
          logger.warn(s"[ConfirmAmendmentController] AmendmentDetails missing from userAnswers")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    }

  private def validateUserCanAccessRequest(
    instanceId: String
  )(implicit request: OptionalDataRequest[_], hc: HeaderCarrier): Future[Boolean] =
    if (request.isAgent) {
      validateAgentCanAccessClient()
    } else {
      validateOrgCanAccessInstanceId(instanceId)
    }

  private def validateAgentCanAccessClient()(implicit
    request: OptionalDataRequest[_],
    hc: HeaderCarrier
  ): Future[Boolean] =
    monthlyReturnService.getAgentClient(request.userId).flatMap {
      case Some(agentClientData) =>
        val updatedUserAnswers =
          request.userAnswers
            .getOrElse(UserAnswers(request.userId))
            .set(AgentClientDataPage, agentClientData)
            .get

        sessionRepository.set(updatedUserAnswers).flatMap { _ =>
          monthlyReturnService.hasClient(agentClientData.taxOfficeNumber, agentClientData.taxOfficeReference)
        }

      case None =>
        logger.warn(s"[ConfirmAmendmentController] Agent user ${request.userId} has no AgentClientData")
        Future.successful(false)
    }

  private def validateOrgCanAccessInstanceId(instanceId: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    monthlyReturnService.getCisTaxpayer().map { taxPayer =>
      val authorised = taxPayer.uniqueId == instanceId

      if (!authorised) {
        logger.warn(
          s"[ConfirmAmendmentController] Organisation user attempted to access instanceId $instanceId " +
            s"which does not match their uniqueId ${taxPayer.uniqueId}"
        )
      }

      authorised
    }

  private def retrieveAndStoreAmendmentDetails(
    params: ContinueReturnJourneyQueryParams
  )(implicit request: OptionalDataRequest[_], hc: HeaderCarrier): Future[Unit] =
    monthlyReturnService
      .retrieveMonthlyReturnForEditDetails(
        instanceId = params.instanceId,
        taxMonth = params.taxMonth,
        taxYear = params.taxYear
      )
      .flatMap { details =>
        details.monthlyReturn.find(matchesQueryParams(_, params)) match {
          case Some(monthlyReturn) =>
            val amendmentDetails   = toAmendmentDetails(params, monthlyReturn, details.submission)
            val updatedUserAnswers =
              request.userAnswers
                .getOrElse(UserAnswers(request.userId))
                .set(CisIdPage, params.instanceId)
                .flatMap(_.set(AmendmentDetailsPage, amendmentDetails))
                .get

            sessionRepository.set(updatedUserAnswers).map(_ => ())

          case None =>
            Future.failed(
              new RuntimeException(
                s"No monthly return found for instanceId ${params.instanceId}, taxMonth ${params.taxMonth}, taxYear ${params.taxYear}"
              )
            )
        }
      }

  private def matchesQueryParams(monthlyReturn: MonthlyReturn, queryParams: ContinueReturnJourneyQueryParams): Boolean =
    monthlyReturn.taxYear == queryParams.taxYear &&
      monthlyReturn.taxMonth == queryParams.taxMonth

  private def toAmendmentDetails(
    params: ContinueReturnJourneyQueryParams,
    monthlyReturn: MonthlyReturn,
    submissions: Seq[Submission]
  ): AmendmentDetails =
    AmendmentDetails(
      instanceId = params.instanceId,
      taxYear = params.taxYear,
      taxMonth = params.taxMonth,
      returnType = deriveReturnType(monthlyReturn),
      acceptedTime = acceptedTimeForMonthlyReturn(monthlyReturn, submissions)
    )

  private def deriveReturnType(monthlyReturn: MonthlyReturn): ReturnType =
    monthlyReturn.nilReturnIndicator match {
      case Some("Y") => MonthlyNilReturn
      case _         => MonthlyStandardReturn
    }

  private def acceptedTimeForMonthlyReturn(monthlyReturn: MonthlyReturn, submissions: Seq[Submission]): Option[String] =
    submissions
      .find(_.activeObjectId.contains(monthlyReturn.monthlyReturnId))
      .flatMap(_.acceptedTime)
}
