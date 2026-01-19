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
import forms.monthlyreturns.DateConfirmPaymentsFormProvider
import models.Mode
import models.monthlyreturns.MonthlyReturnRequest
import navigation.Navigator
import pages.monthlyreturns.DateConfirmPaymentsPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.monthlyreturns.DateConfirmPaymentsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DateConfirmPaymentsController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  formProvider: DateConfirmPaymentsFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: DateConfirmPaymentsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) { implicit request =>

    val form = formProvider()

    val preparedForm = request.userAnswers.get(DateConfirmPaymentsPage) match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      val form = formProvider()

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          value => {
            val year  = value.getYear
            val month = value.getMonthValue

            monthlyReturnService
              .resolveAndStoreCisId(request.userAnswers, request.isAgent)
              .flatMap { case (cisId, _) =>
                monthlyReturnService
                  .isDuplicate(cisId, year, month)
                  .flatMap {
                    case true =>
                      val dupForm =
                        form
                          .fill(value)
                          .withError(
                            "value",
                            "dateConfirmPayments.taxYear.error.duplicate"
                          )
                      Future.successful(BadRequest(view(dupForm, mode)))

                    case false =>
                      val createRequest = MonthlyReturnRequest(cisId, year, month)
                      monthlyReturnService
                        .createMonthlyReturn(createRequest)
                        .flatMap { _ =>
                          for {
                            updatedAnswers <- Future.fromTry(request.userAnswers.set(DateConfirmPaymentsPage, value))
                            _              <- sessionRepository.set(updatedAnswers)
                          } yield Redirect(navigator.nextPage(DateConfirmPaymentsPage, mode, updatedAnswers))
                        }
                  }
              }
              .recover { exception =>
                logger.error(
                  s"[DateConfirmPaymentsController] duplicate check fails unexpectedly: ${exception.getMessage}",
                  exception
                )
                Redirect(controllers.routes.SystemErrorController.onPageLoad())
              }
          }
        )
  }
}
