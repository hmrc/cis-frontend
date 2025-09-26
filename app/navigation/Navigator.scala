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

package navigation

import javax.inject.{Inject, Singleton}
import play.api.mvc.Call
import pages.*
import pages.monthlyreturns.{ConfirmEmailAddressPage, DateConfirmNilPaymentsPage, DeclarationPage, InactivityRequestPage, InactivityWarningPage}
import models.*
import models.monthlyreturns.InactivityRequest

@Singleton
class Navigator @Inject() () {

  private val normalRoutes: Page => UserAnswers => Call = {
    case DateConfirmNilPaymentsPage =>
      _ => controllers.monthlyreturns.routes.InactivityRequestController.onPageLoad(NormalMode)
    case InactivityRequestPage      =>
      _ => controllers.monthlyreturns.routes.ConfirmEmailAddressController.onPageLoad(NormalMode)
    case ConfirmEmailAddressPage    =>
      _ => controllers.monthlyreturns.routes.DeclarationController.onPageLoad(NormalMode)
    case DeclarationPage            =>
      userAnswers =>
        userAnswers.get(InactivityRequestPage) match {
          case Some(InactivityRequest.Option2)        =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
          case Some(InactivityRequest.Option1) | None =>
            controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
        }
    case InactivityWarningPage      =>
      _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
    case _                          => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  private val checkRouteMap: Page => UserAnswers => Call = {
    case InactivityRequestPage =>
      userAnswers =>
        userAnswers.get(InactivityRequestPage) match {
          case Some(InactivityRequest.Option2)        =>
            controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
          case Some(InactivityRequest.Option1) | None =>
            controllers.monthlyreturns.routes.InactivityWarningController.onPageLoad
        }
    case _                     => _ => controllers.monthlyreturns.routes.CheckYourAnswersController.onPageLoad()
  }

  def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = mode match {
    case NormalMode =>
      normalRoutes(page)(userAnswers)
    case CheckMode  =>
      checkRouteMap(page)(userAnswers)
  }
}
