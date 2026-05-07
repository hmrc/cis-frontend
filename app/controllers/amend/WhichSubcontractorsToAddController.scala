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
import forms.amend.WhichSubcontractorsToAddFormProvider

import javax.inject.Inject
import models.Mode
import models.amend.{Subcontractor, WhichSubcontractorsToAdd}
import navigation.Navigator
import pages.amend.WhichSubcontractorsToAddPage
import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.amend.WhichSubcontractorsToAddView

import scala.concurrent.{ExecutionContext, Future}

class WhichSubcontractorsToAddController @Inject() (
  override val messagesApi: MessagesApi,
  sessionRepository: SessionRepository,
  navigator: Navigator,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  requireData: DataRequiredAction,
  requireCisId: CisIdRequiredAction,
  formProvider: WhichSubcontractorsToAddFormProvider,
  monthlyReturnService: MonthlyReturnService,
  val controllerComponents: MessagesControllerComponents,
  view: WhichSubcontractorsToAddView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      monthlyReturnService
        .retrieveMonthlyReturnForEditDetails("1", 2026, 4)
        .map { data =>
          val subcontractorsInDb = data.subcontractors.map { item =>
            Subcontractor(item.subcontractorId.toString, item.displayName.getOrElse("No name provided"))
          }

          val selectedIds: Set[String] = request.userAnswers
            .get(WhichSubcontractorsToAddPage)
            .getOrElse(data.monthlyReturnItems.flatMap(_.subcontractorId.map(_.toString)).toSet)

          val checkboxItems = WhichSubcontractorsToAdd.checkboxItems(subcontractorsInDb, selectedIds)

          Ok(view(formProvider(subcontractorsInDb), mode, checkboxItems))
        }
        .recover { error =>
          logger.error(
            s"[WhichSubcontractorsToAddController] Failed to fetch monthly return for edit : ${error.toString}",
            error
          )
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (identify andThen getData andThen requireData andThen requireCisId).async { implicit request =>
      monthlyReturnService
        .retrieveMonthlyReturnForEditDetails("1", 2026, 4)
        .flatMap { data =>

          val submissionStatus = data.submission.headOption.flatMap(_.status)

          submissionStatus match {
            case Some("STARTED") | Some("VALIDATED") =>
              val subcontractorsInDb = data.subcontractors.map { item =>
                Subcontractor(item.subcontractorId.toString, item.displayName.getOrElse("No name provided"))
              }
              formProvider(subcontractorsInDb)
                .bindFromRequest()
                .fold(
                  formWithErrors =>
                    Future.successful(
                      BadRequest(view(formWithErrors, mode, WhichSubcontractorsToAdd.checkboxItems(subcontractorsInDb)))
                    ),
                  value =>
                    for {
                      updatedAnswers <- Future.fromTry(request.userAnswers.set(WhichSubcontractorsToAddPage, value))
                      _              <- sessionRepository.set(updatedAnswers)
                      _              <- monthlyReturnService.syncMonthlyReturnItems("1", 2026, 4, value.toSeq.map(_.toLong))
                    } yield Redirect(navigator.nextPage(WhichSubcontractorsToAddPage, mode, updatedAnswers))
                )
            case _                                   =>
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          }
        }
        .recover { error =>
          logger.error(
            s"[WhichSubcontractorsToAddController] Failed during submission : ${error.toString}",
            error
          )
          Redirect(controllers.routes.SystemErrorController.onPageLoad())
        }
    }
}
