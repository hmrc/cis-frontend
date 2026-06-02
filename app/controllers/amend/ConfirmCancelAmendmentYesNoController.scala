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
import forms.amend.ConfirmCancelAmendmentYesNoFormProvider
import models.{NormalMode, UserAnswers}
import models.amend.DeleteUnsubmittedMonthlyReturnRequest
import pages.amend.ConfirmCancelAmendmentYesNoPage
import pages.monthlyreturns.{ContractorNamePage, DateConfirmPaymentsPage}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.*
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.amend.ConfirmCancelAmendmentYesNoView

import java.time.format.DateTimeFormatter
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmCancelAmendmentYesNoController @Inject() (
  override val messagesApi: MessagesApi,
  amendMonthlyReturnService: AmendMonthlyReturnService,
  monthlyReturnService: MonthlyReturnService,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  formProvider: ConfirmCancelAmendmentYesNoFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConfirmCancelAmendmentYesNoView,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      getMonthYear(request.userAnswers) match {
        case Some(monthYear) =>
          hasCancellableMonthlyReturnStatus(request.cisId, request.userAnswers).map {
            case true =>
              val preparedForm = request.userAnswers.get(ConfirmCancelAmendmentYesNoPage) match {
                case None        => form
                case Some(value) => form.fill(value)
              }

              Ok(view(preparedForm, monthYear))

            case false =>
              logger.warn(s"[ConfirmCancelAmendmentYesNoController] monthly return status is not cancellable")
              Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          }

        case None =>
          logger.error("[ConfirmCancelAmendmentYesNoController] monthYear missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData andThen requireData andThen requireCisId).async {
    implicit request =>
      getMonthYear(request.userAnswers) match {
        case Some(monthYear) =>
          hasCancellableMonthlyReturnStatus(request.cisId, request.userAnswers).flatMap {
            case true =>
              form
                .bindFromRequest()
                .fold(
                  formWithErrors => Future.successful(BadRequest(view(formWithErrors, monthYear))),
                  value =>
                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(ConfirmCancelAmendmentYesNoPage, value))
                      _              <- sessionRepository.set(updatedAnswers)
                      result         <- if (value) handleYes(updatedAnswers, request.cisId) else handleNo
                    } yield result
                )

            case false =>
              logger.warn(s"[ConfirmCancelAmendmentYesNoController] monthly return status is not cancellable")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }

        case None =>
          logger.error("[ConfirmCancelAmendmentYesNoController] monthYear missing")
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }

  private def hasCancellableMonthlyReturnStatus(
    cisId: String,
    ua: UserAnswers
  )(implicit request: RequestHeader): Future[Boolean] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    monthlyReturnService
      .retrieveAllMonthlyReturns(cisId)
      .map { response =>
        ua.get(DateConfirmPaymentsPage).exists { monthYear =>
          response.monthlyReturnList.exists { monthlyReturn =>
            monthlyReturn.taxYear == monthYear.getYear &&
            monthlyReturn.taxMonth == monthYear.getMonthValue &&
            monthlyReturn.amendment.contains("Y") &&
            monthlyReturn.status.exists { status =>
              status == "STARTED" || status == "VALIDATED"
            }
          }
        }
      }
      .recover { case e =>
        logger.error(
          s"[ConfirmCancelAmendmentYesNoController] error checking monthly return status for cisId=$cisId",
          e
        )
        false
      }
  }

  private def handleYes(ua: UserAnswers, instanceId: String)(implicit request: RequestHeader): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val deleteRequest = DeleteUnsubmittedMonthlyReturnRequest.fromUserAnswers(ua)

    amendMonthlyReturnService
      .deleteUnsubmittedMonthlyReturn(deleteRequest)
      .map { _ =>
        Redirect(appConfig.returnsLandingPageUrl(instanceId, ua.get(ContractorNamePage)))
      }
  }

  private def handleNo: Future[Result] =
    Future.successful(
      Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode))
    )

  private val monthYearFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

  private def getMonthYear(ua: UserAnswers): Option[String] =
    ua.get(DateConfirmPaymentsPage).map(_.format(monthYearFormatter))
}
