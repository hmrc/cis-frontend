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

import base.SpecBase
import config.FrontendAppConfig
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.UserAnswers
import models.monthlyreturns.*
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.SubcontractorService
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SelectSubcontractorsControllerSpec extends SpecBase with MockitoSugar {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  private val formProvider = new SelectSubcontractorsFormProvider()
  private val form         = formProvider()

  private val cisId    = "CIS-123"
  private val taxDate  = LocalDate.of(2025, 10, 15)
  private val taxMonth = taxDate.getMonthValue
  private val taxYear  = taxDate.getYear

  private val userAnswersWithRequiredPages =
    emptyUserAnswers
      .set(CisIdPage, cisId)
      .success
      .value
      .set(DateConfirmPaymentsPage, taxDate)
      .success
      .value

  private val subcontractors = Seq(
    SelectSubcontractorsViewModel(1, "A", "Yes", "Unknown", "Unknown"),
    SelectSubcontractorsViewModel(2, "B", "No", "Unknown", "Unknown")
  )

  private val pageModelSelected =
    SelectSubcontractorsPageModel(subcontractors = subcontractors, initiallySelectedIds = Seq(1))

  private val pageModelNoneSelected =
    SelectSubcontractorsPageModel(subcontractors = subcontractors, initiallySelectedIds = Seq.empty)

  private def applicationWith(
    service: SubcontractorService,
    ua: Option[UserAnswers] = Some(userAnswersWithRequiredPages)
  ) =
    applicationBuilder(userAnswers = ua)
      .overrides(bind[SubcontractorService].toInstance(service))
      .build()

  private def stubBuild(
    service: SubcontractorService,
    model: SelectSubcontractorsPageModel,
    defaultSel: Option[Boolean]
  ): Unit =
    when(
      service.buildSelectSubcontractorPage(
        eqTo(cisId),
        eqTo(taxMonth),
        eqTo(taxYear),
        eqTo(defaultSel),
        any[LocalDate]
      )(using any[HeaderCarrier])
    ).thenReturn(Future.successful(model))

  "SelectSubcontractors Controller" - {

    "onPageLoad" - {

      "renders OK and fills form when initiallySelectedIds is non-empty" in {
        val service = mock[SubcontractorService]
        stubBuild(service, pageModelSelected, defaultSel = None)

        val app = applicationWith(service)

        running(app) {
          val request =
            FakeRequest(GET, controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url)

          val result = route(app, request).value
          val view   = app.injector.instanceOf[SelectSubcontractorsView]

          status(result) mustBe OK
          contentAsString(result) mustBe
            view(
              form.fill(SelectSubcontractorsFormData(confirmation = false, subcontractorsToInclude = Seq(1))),
              subcontractors,
              app.injector.instanceOf[FrontendAppConfig].selectSubcontractorsUpfrontDeclaration
            )(request, messages(app)).toString
        }
      }

      "renders OK without filling form when initiallySelectedIds is empty" in {
        val service = mock[SubcontractorService]
        stubBuild(service, pageModelNoneSelected, defaultSel = None)

        val app = applicationWith(service)

        running(app) {
          val request =
            FakeRequest(GET, controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url)

          val result = route(app, request).value
          val view   = app.injector.instanceOf[SelectSubcontractorsView]

          status(result) mustBe OK
          contentAsString(result) mustBe
            view(
              form,
              subcontractors,
              app.injector.instanceOf[FrontendAppConfig].selectSubcontractorsUpfrontDeclaration
            )(request, messages(app)).toString
        }
      }

      "redirects to JourneyRecovery when required answers are missing" in {
        val service = mock[SubcontractorService]
        val app     = applicationWith(service, ua = None)

        running(app) {
          val request =
            FakeRequest(GET, controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url)

          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "returns OK when confirmation is true" in {
        val service = mock[SubcontractorService]
        stubBuild(service, pageModelNoneSelected, defaultSel = None)

        val app = applicationWith(service)

        running(app) {
          val request =
            FakeRequest(POST, controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url)
              .withFormUrlEncodedBody(
                "confirmation"              -> "true",
                "subcontractorsToInclude.0" -> "1"
              )

          val result = route(app, request).value
          status(result) mustBe OK
        }
      }

      "returns BAD_REQUEST when confirmation is false" in {
        val service = mock[SubcontractorService]
        stubBuild(service, pageModelNoneSelected, defaultSel = None)

        val app = applicationWith(service)

        running(app) {
          val request =
            FakeRequest(POST, controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url)
              .withFormUrlEncodedBody(
                "confirmation"              -> "false",
                "subcontractorsToInclude.0" -> "1"
              )

          val result = route(app, request).value
          status(result) mustBe BAD_REQUEST
        }
      }

      "redirects to JourneyRecovery when no user answers exist" in {
        val service = mock[SubcontractorService]
        val app     = applicationWith(service, ua = None)

        running(app) {
          val request =
            FakeRequest(POST, controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url)
              .withFormUrlEncodedBody("confirmation" -> "true")

          val result = route(app, request).value
          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
