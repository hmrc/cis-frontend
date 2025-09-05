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

import controllers.actions.*
import forms.monthlyreturns.DateConfirmNilPaymentsFormProvider
import models.Mode
import navigation.Navigator
import pages.monthlyreturns.DateConfirmNilPaymentsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import views.html.monthlyreturns.DateConfirmNilPaymentsView

import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DateConfirmNilPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DateConfirmNilPaymentsFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: DateConfirmNilPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val form = formProvider()

    val preparedForm = request.userAnswers.get(DateConfirmNilPaymentsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequestAndSession(request, request.session)

      val form = formProvider()

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value => {
            val year      = value.getYear
            val month     = value.getMonthValue
            val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.UK)

            monthlyReturnService
              .isDuplicate(year, month)
              .flatMap {
                case true =>
                  val dupForm =
                    form.fill(value).withGlobalError("monthlyReturn.duplicate", monthName, year.toString)
                  Future.successful(BadRequest(view(dupForm, mode)))

                case false =>
                  // Not a duplicate — proceed as before
                  for {
                    updatedAnswers <- Future.fromTry(request.userAnswers.set(DateConfirmNilPaymentsPage, value))
                    _              <- sessionRepository.set(updatedAnswers)
                  } yield Redirect(navigator.nextPage(DateConfirmNilPaymentsPage, mode, updatedAnswers))
              }
              .recover { case _ =>
                val errForm =
                  form.fill(value).withGlobalError("error.technical")
                InternalServerError(view(errForm, mode))
              }
          }
        )
  }
}
