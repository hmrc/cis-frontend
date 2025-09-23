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

package stub.controllers.monthlyreturns

import controllers.actions.*
import models.UserAnswers
import pages.monthlyreturns.{ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, DeclarationPage, InactivityRequestPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import stub.views.html.monthlyreturns.StubSubmissionSendingView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class StubSubmissionSendingController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  val controllerComponents: MessagesControllerComponents,
  view: StubSubmissionSendingView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (identify andThen getData andThen requireData).async { implicit request =>

    def removeUserAnswers(userAnswers: UserAnswers): Try[UserAnswers] = {
      val pagesToRemove =
        Seq(DateConfirmNilPaymentsPage, InactivityRequestPage, ConfirmEmailAddressPage, DeclarationPage)

      pagesToRemove.foldLeft(Try(userAnswers)) { (currentUserAnswers, page) =>
        currentUserAnswers.flatMap(_.remove(page))
      }
    }

    for {
      updatedUserAnswers <- Future.fromTry(removeUserAnswers(request.userAnswers))
      _                  <- sessionRepository.set(updatedUserAnswers)
    } yield Ok(view())

  }
}
