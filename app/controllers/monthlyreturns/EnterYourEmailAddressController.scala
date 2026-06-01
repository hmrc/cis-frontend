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
import models.requests.{DataRequest, GetMonthlyReturnForEditRequest}
import navigation.Navigator
import pages.amend.AmendmentDetailsPage
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, EnterYourEmailAddressPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
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
      request.userAnswers.get(EnterYourEmailAddressPage) match {
        case Some(value)                =>
          Future.successful(Ok(view(form.fill(value), mode)))
        case None if mode == NormalMode =>
          getPrepopulationEmailAddress().map {
            case Some(email) => Ok(view(form.fill(email), mode))
            case None        => Ok(view(form, mode))
          }
        case None                       =>
          Future.successful(Ok(view(form, mode)))
      }
    }

  private def getPrepopulationEmailAddress()(implicit request: DataRequest[AnyContent]): Future[Option[String]] = {
    val requiredAnswers = for {
      cisId   <- request.userAnswers.get(CisIdPage)
      taxDate <- request.userAnswers.get(DateConfirmPaymentsPage)
    } yield (cisId, taxDate.getMonthValue, taxDate.getYear)

    requiredAnswers match {
      case Some((cisId, month, year)) =>
        val isAmendment = request.userAnswers.get(AmendmentDetailsPage).isDefined
        monthlyReturnService
          .retrieveMonthlyReturnForEditDetails(
            GetMonthlyReturnForEditRequest(
              cisId,
              taxMonth = month,
              taxYear = year,
              isAmendment = isAmendment
            )
          )
          .map { editDetails =>
            editDetails.scheme.flatMap(_.emailAddress).headOption
          }
          .recover { case _ =>
            None
          }
      case _                          =>
        Future.successful(None)
    }
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
