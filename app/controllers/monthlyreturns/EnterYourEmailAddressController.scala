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
import forms.monthlyreturns.EnterYourEmailAddressFormProvider
import models.Mode
import models.requests.{CisIdDataRequest, CisPath}
import navigation.Navigator
import pages.monthlyreturns.{CisIdPage, ConfirmationByEmailPage, EnterYourEmailAddressPage}
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.EnterYourEmailAddressView

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EnterYourEmailAddressController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  resolveScheme: SchemeAction,
  getJourney: JourneyAction,
  formProvider: EnterYourEmailAddressFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: EnterYourEmailAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      if (!request.userAnswers.get(ConfirmationByEmailPage).contains(true)) {
        Future.successful(Redirect(controllers.monthlyreturns.routes.ConfirmationByEmailController.onPageLoad(mode)))
      } else {
        request.userAnswers.get(EnterYourEmailAddressPage) match {
          case Some(value) =>
            Future.successful(Ok(view(form.fill(value), mode)))
          case None        =>
            getPrepopulationEmailAddress(request).map {
              case Some(email) => Ok(view(form.fill(email), mode))
              case None        => Ok(view(form, mode))
            }
        }
      }
    }

  private def getPrepopulationEmailAddress(
    request: CisIdDataRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Future[Option[String]] =
    request.userAnswers.get(CisIdPage) match {
      case Some(cisId) =>
        monthlyReturnService
          .getSchemeEmail(cisId)
          .recover { case ex =>
            logger.warn(
              s"[EnterYourEmailAddressController][getPrepopulationEmailAddress] - email lookup failed, cisId: $cisId",
              ex
            )
            None
          }
      case None        =>
        Future.successful(None)
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(EnterYourEmailAddressPage, value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(EnterYourEmailAddressPage, mode, updatedAnswers))
        )
    }

  // Multi-tab
  def onPageLoadWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val id         = request.subJourneyId
      val formAction = Some(routes.EnterYourEmailAddressController.onSubmitWithId(cisPath, flowId, mode))

      if (!request.userAnswers.get(ConfirmationByEmailPage(id)).contains(true)) {
        Future.successful(Redirect(routes.ConfirmationByEmailController.onPageLoadWithId(cisPath, flowId, mode)))
      } else {
        request.userAnswers.get(EnterYourEmailAddressPage(id)) match {
          case Some(value) =>
            Future.successful(Ok(view(form.fill(value), mode, formAction)))
          case None        =>
            monthlyReturnService
              .getSchemeEmail(request.cisId)
              .recover { case NonFatal(ex) =>
                logger.warn("[EnterYourEmailAddressController][onPageLoadWithId] - email lookup failed", ex)
                None
              }
              .map {
                case Some(email) => Ok(view(form.fill(email), mode, formAction))
                case None        => Ok(view(form, mode, formAction))
              }
        }
      }
    }

  def onSubmitWithId(cisPath: CisPath, flowId: UUID, mode: Mode): Action[AnyContent] =
    (identify andThen resolveScheme(cisPath) andThen getJourney(flowId)).async { implicit request =>
      val id         = request.subJourneyId
      val formAction = Some(routes.EnterYourEmailAddressController.onSubmitWithId(cisPath, flowId, mode))

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, formAction))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(EnterYourEmailAddressPage(id), value))
              _              <- sessionRepository.set(updatedAnswers)
            } yield Redirect(navigator.nextPage(cisPath, flowId, EnterYourEmailAddressPage, mode, updatedAnswers))
        )
    }
}
