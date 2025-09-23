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
import controllers.routes
import models.UserAnswers
import pages.monthlyreturns.*
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    val ua = request.userAnswers

    def removeUserAnswers(userAnswers: UserAnswers): Try[UserAnswers] = {
      val pagesToRemove =
        Seq(DateConfirmNilPaymentsPage, InactivityRequestPage, ConfirmEmailAddressPage, DeclarationPage)

      pagesToRemove.foldLeft(Try(userAnswers)) { (currentUserAnswers, page) =>
        currentUserAnswers.flatMap(_.remove(page))
      }
    }

    for {
      emailAddress       <- Future.apply(ua.get(ConfirmEmailAddressPage))
      updatedUserAnswers <- Future.fromTry(removeUserAnswers(ua))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield emailAddress match {
      case Some(ea) if ea.equalsIgnoreCase("Submissionsuccessful@test.com")   =>
        Redirect(controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad)
      case Some(ea) if ea.equalsIgnoreCase("Submissionunsuccessful@test.com") =>
        Redirect(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad)
      case Some(ea) if ea.equalsIgnoreCase("Awaitingconfirmation@test.com")   =>
        Redirect(controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad)
      case _                                                                  =>
        Redirect(routes.JourneyRecoveryController.onPageLoad())
    }

  }
}
