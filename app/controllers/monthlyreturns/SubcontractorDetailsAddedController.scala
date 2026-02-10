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
import forms.monthlyreturns.SubcontractorDetailsAddedFormProvider
import models.{Mode, UserAnswers}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.checkAnswers.monthlyreturns.SubcontractorDetailsAddedBuilder
import views.html.monthlyreturns.SubcontractorDetailsAddedView

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.Future

class SubcontractorDetailsAddedController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  formProvider: SubcontractorDetailsAddedFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SubcontractorDetailsAddedView
) extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val ua = request.userAnswers.getOrElse(seedUserAnswers(request.userId))
    SubcontractorDetailsAddedBuilder.build(ua) match {
      case Some(viewModel) => Ok(view(form, mode, viewModel))
      case None            => Redirect(controllers.routes.SystemErrorController.onPageLoad())
    }
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val ua = request.userAnswers.getOrElse(seedUserAnswers(request.userId))

    SubcontractorDetailsAddedBuilder.build(ua) match {
      case None =>
        Future.successful(Redirect(controllers.routes.SystemErrorController.onPageLoad()))

      case Some(viewModel) =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, viewModel))),
            answer =>
              if (answer) {
                if (viewModel.hasIncomplete) {
                  val withError = form.withError("value", "monthlyreturns.subcontractorDetailsAdded.error.incomplete")
                  Future.successful(BadRequest(view(withError, mode, viewModel)))
                } else {
                  Future.successful(
                    Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(mode))
                  )
                }
              } else {
                Future.successful(
                  Redirect(controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(mode))
                )
              }
          )
    }
  }

  private def seedUserAnswers(userId: String): UserAnswers =
    val data = Json.obj(
      "subcontractors" -> Json.arr(
        // index 0: complete (details added)
        Json.obj(
          "subcontractorId"  -> 1001L,
          "name"             -> "TyneWear Ltd",
          "totalPaymentMade" -> 1000.00,
          "costOfMaterials"  -> 200.00,
          "totalTaxDeducted" -> 200.00
        ),
        // index 1: incomplete
        Json.obj(
          "subcontractorId"  -> 1002L,
          "name"             -> "Northern Trades  Ltd"
        ),
        // index 2: incomplete
        Json.obj(
          "subcontractorId"  -> 1003L,
          "name"             -> "BuildRight Construction"
        )
      )
    )

    UserAnswers(
      id = userId,
      data = data,
      lastUpdated = Instant.now
    )
}
