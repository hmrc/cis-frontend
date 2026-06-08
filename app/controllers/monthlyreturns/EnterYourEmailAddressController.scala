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
import models.{Mode, NormalMode}
import models.requests.CisIdDataRequest
import navigation.Navigator
import pages.monthlyreturns.{CisIdPage, EnterYourEmailAddressPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.EnterYourEmailAddressView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EnterYourEmailAddressController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  formProvider: EnterYourEmailAddressFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: EnterYourEmailAddressView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      request.userAnswers.get(EnterYourEmailAddressPage) match {
        case Some(value)                =>
          Future.successful(Ok(view(form.fill(value), mode)))
        case None if mode == NormalMode =>
          getPrepopulationEmailAddress(request).map {
            case Some(email) => Ok(view(form.fill(email), mode))
            case None        => Ok(view(form, mode))
          }
        case None                       =>
          Future.successful(Ok(view(form, mode)))
      }
    }

  private def getPrepopulationEmailAddress(
    request: CisIdDataRequest[AnyContent]
  )(implicit hc: HeaderCarrier): Future[Option[String]] =
    request.userAnswers.get(CisIdPage) match {
      case Some(cisId) =>
        monthlyReturnService
          .getSchemeEmail(cisId)
          .recover { case _ => None }
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
}
