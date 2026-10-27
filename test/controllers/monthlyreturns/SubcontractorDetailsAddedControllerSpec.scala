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

import base.SpecBase
import models.ReturnType.{MonthlyAmendedStandardReturn, MonthlyStandardReturn}
import models.amend.AmendmentDetails
import models.{NormalMode, UserAnswers}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito.Mockito.{verify, when}
import org.mockito.ArgumentCaptor
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.AmendmentDetailsPage
import pages.finalvalidations.*
import pages.monthlyreturns.{AllSubcontractorDetailsAdded, CisIdPage, ReturnTypePage}
import play.api.data.Form
import play.twirl.api.Html
import views.html.monthlyreturns.SubcontractorDetailsAddedView
import play.api.inject.bind
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future
import pages.monthlyreturns.OriginalSubcontractorCountPage

class SubcontractorDetailsAddedControllerSpec extends SpecBase with MockitoSugar {

  private val now: Instant = Instant.now

  private def uaWithSubcontractors(subs: (Int, JsObject)*): UserAnswers =
    UserAnswers(
      id = userAnswersId,
      data = Json.obj(
        "cisId"               -> "1",
        "dateConfirmPayments" -> "2025-10-01",
        "subcontractors"      -> JsObject(subs.map { case (i, o) => i.toString -> o })
      ),
      lastUpdated = now
    )
      .setOrException(ReturnTypePage, MonthlyStandardReturn)
      .setOrException(MonthlyFinalValidationContinuationPage, NormalMode.toString)

  private def completeSub(id: Long, name: String): JsObject =
    Json.obj(
      "id"                -> id,
      "name"              -> name,
      "totalPaymentsMade" -> 1000.00,
      "costOfMaterials"   -> 200.00,
      "totalTaxDeducted"  -> 200.00
    )

  private def incompleteSub(id: Long, name: String): JsObject =
    Json.obj(
      "id"   -> id,
      "name" -> name
    )

  private def stubIsEditable(service: MonthlyReturnService, editable: Boolean = true): Unit =
    when(service.isEditable(any[String], any[Int], any[Int], any[Boolean])(any[HeaderCarrier]))
      .thenReturn(Future.successful(editable))

  private def buildApp(
    ua: UserAnswers,
    monthlyReturnService: Option[MonthlyReturnService] = None,
    view: Option[SubcontractorDetailsAddedView] = None,
    sessionRepository: Option[SessionRepository] = None
  ) = {
    val base = applicationBuilder(userAnswers = Some(ua))
      .configure(
        "features.welsh-translation" -> false,
        "timeout-dialog.timeout"     -> 900,
        "timeout-dialog.countdown"   -> 120,
        "contact-frontend.serviceId" -> "cis-frontend",
        "host"                       -> "http://localhost"
      )

    val withService =
      monthlyReturnService
        .fold(base)(svc => base.overrides(bind[MonthlyReturnService].toInstance(svc)))

    val withView =
      view
        .fold(withService)(v => withService.overrides(bind[SubcontractorDetailsAddedView].toInstance(v)))

    sessionRepository
      .fold(withView)(repo => withView.overrides(bind[SessionRepository].toInstance(repo)))
      .build()
  }

  private def getUrl: String =
    controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode).url

  private def postUrl: String =
    controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onSubmit(NormalMode).url

  private def cancelUrl: String =
    controllers.monthlyreturns.routes.SubcontractorDetailsAddedController.onCancelAmendment().url

  "SubcontractorDetailsAddedController" - {

    "must return OK on GET when builder returns a view model" in {
      val ua  = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe OK
      }
    }

    "must redirect to ReviewSubcontractorDetails on GET when a Final Validation draft exists" in {
      val ua  = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
        .setOrException(FinalValidationDraftIdPage, "draft-123")
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.finalvalidations.routes.ReviewSubcontractorDetailsController.onPageLoad().url
      }
    }

    "must redirect to JourneyRecovery on GET when Final Validation source and continuation are missing" in {
      val ua  = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
        .remove(MonthlyFinalValidationContinuationPage)
        .get
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must render page after Final Validation when continuation is valid" in {
      val ua  = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoadAfterFinalValidation(NormalMode)
            .url
        )
        val result  = route(application, request).value

        status(result) mustBe OK
      }
    }

    "must redirect to JourneyRecovery after Final Validation when continuation is missing" in {
      val ua  = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
        .remove(MonthlyFinalValidationContinuationPage)
        .get
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoadAfterFinalValidation(NormalMode)
            .url
        )
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must display cancel amendment link on GET when builder returns a view model with isAmendment = true" in {
      val ua  = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      ).setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        .set(AmendmentDetailsPage, AmendmentDetails("1", 2025, 1, "Test Contractor", MonthlyStandardReturn, None))
        .get
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe OK
        contentAsString(result) must include("Cancel amendment")
      }
    }

    "must redirect to SystemError on GET when builder returns None (no subcontractors)" in {
      val ua  = uaWithSubcontractors()
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on GET when resource is not editable" in {
      val ua  = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc, editable = false)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery on GET when required answers are missing" in {
      val ua          = UserAnswers(userAnswersId).setOrException(CisIdPage, "1")
      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must retain Yes when the user previously selected that they need to add more subcontractors" in {
      val ua =
        uaWithSubcontractors(
          1 -> completeSub(1001L, "TyneWear Ltd")
        )
          .set(AllSubcontractorDetailsAdded, false)
          .get

      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val mockView = mock[SubcontractorDetailsAddedView]

      when(
        mockView.apply(any(), any(), any())(any(), any())
      ).thenReturn(Html(""))

      val application = buildApp(
        ua,
        Some(svc),
        Some(mockView)
      )

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe OK

        verify(mockView).apply(
          argThat[Form[Boolean]](_.value.contains(true)),
          any(),
          any()
        )(any(), any())
      }
    }

    "must retain No when the user previously selected that they do not need to add more subcontractors" in {
      val ua =
        uaWithSubcontractors(
          1 -> completeSub(1001L, "TyneWear Ltd")
        )
          .set(AllSubcontractorDetailsAdded, true)
          .get

      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val mockView = mock[SubcontractorDetailsAddedView]

      when(
        mockView.apply(any(), any(), any())(any(), any())
      ).thenReturn(Html(""))

      val application = buildApp(
        ua,
        Some(svc),
        Some(mockView)
      )

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe OK

        verify(mockView).apply(
          argThat[Form[Boolean]](_.value.contains(false)),
          any(),
          any()
        )(any(), any())
      }
    }

    "must redirect to onPageLoad when submitting before Final Validation completes" in {
      val ua = uaWithSubcontractors(1 -> completeSub(1001L, "TyneWear Ltd"))
        .remove(MonthlyFinalValidationContinuationPage)
        .get

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe getUrl
      }
    }

    "must return BadRequest on POST No when viewModel.hasIncomplete = true (incomplete error)" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd"),
        2 -> incompleteSub(1002L, "Incomplete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(
          "You have not entered payment details for all of your selected subcontractors"
        )
      }
    }

    "must redirect to SystemError on POST when builder returns None" in {
      val ua = uaWithSubcontractors()

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to MRAR06 Standard on GET when in amendment mode and no subcontractors remain" in {
      val ua  = uaWithSubcontractors()
        .setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        .set(AmendmentDetailsPage, AmendmentDetails("1", 2025, 1, "Test", MonthlyAmendedStandardReturn, None))
        .get
      val svc = mock[MonthlyReturnService]
      stubIsEditable(svc)

      val application = buildApp(ua, Some(svc))

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.amend.routes.WhatDoYouWantToAmendStandardController
          .onPageLoad()
          .url
      }
    }

    "must redirect to MRAR06 Standard on POST when in amendment mode and no subcontractors remain" in {
      val ua = uaWithSubcontractors()
        .setOrException(ReturnTypePage, MonthlyAmendedStandardReturn)
        .set(AmendmentDetailsPage, AmendmentDetails("1", 2025, 1, "Test", MonthlyAmendedStandardReturn, None))
        .get

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.amend.routes.WhatDoYouWantToAmendStandardController
          .onPageLoad()
          .url
      }
    }

    "must return BadRequest on POST when form has errors (no value) and showYesNo is true" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      ).set(OriginalSubcontractorCountPage, 3).success.value

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(POST, postUrl)
        val result  = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "must redirect to SummarySubcontractorPayments on POST when all subcontractors already added (showYesNo is false)" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      ).set(OriginalSubcontractorCountPage, 1).success.value

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(POST, postUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController.onPageLoad().url
      }
    }

    "must return BadRequest on POST when all subcontractors already added but details are incomplete" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd"),
        2 -> incompleteSub(1002L, "Incomplete Ltd")
      ).set(OriginalSubcontractorCountPage, 2).success.value

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(POST, postUrl)
        val result  = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(
          messages(application)("monthlyreturns.subcontractorDetailsAdded.error.incomplete")
        )
      }
    }

    "must redirect on POST No when viewModel.hasIncomplete = false" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd"),
        2 -> completeSub(1002L, "Also Complete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController
            .onPageLoad()
            .url
      }
    }

    "must return 400 error on POST No when there are incomplete subcontractors" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd"),
        2 -> incompleteSub(1002L, "Incomplete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(
          "You have not entered payment details for all of your selected subcontractors"
        )
      }
    }

    "must redirect on POST Yes when adding more subcontractors ReturnType = MonthlyAmendedStandardReturn" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd")
      ).set(ReturnTypePage, MonthlyAmendedStandardReturn)
        .success
        .value
        .set(OriginalSubcontractorCountPage, 3)
        .success
        .value

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode).url
      }
    }

    "must redirect on POST Yes when adding more subcontractors on a new standard return" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd")
      ).set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value
        .set(pages.monthlyreturns.IsResumedMonthlyReturnPage, false)
        .success
        .value
        .set(OriginalSubcontractorCountPage, 3)
        .success
        .value

      val application = buildApp(ua, sessionRepository = Some(mockSessionRepository))

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url

        val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(userAnswersCaptor.capture())
        userAnswersCaptor.getValue.get(MonthlyFinalValidationContinuationPage) mustBe None
        userAnswersCaptor.getValue.get(AllSubcontractorDetailsAdded) mustBe Some(false)
        userAnswersCaptor.getValue.get(pages.monthlyreturns.IsResumedMonthlyReturnPage) mustBe Some(false)
      }
    }

    "must redirect on POST Yes when adding more subcontractors on a resumed standard return" in {
      val mockSessionRepository = mock[SessionRepository]
      when(mockSessionRepository.set(any[UserAnswers])).thenReturn(Future.successful(true))

      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd")
      ).setOrException(pages.monthlyreturns.IsResumedMonthlyReturnPage, true)
        .setOrException(OriginalSubcontractorCountPage, 3)

      val application = buildApp(ua, sessionRepository = Some(mockSessionRepository))

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.amend.routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode).url

        val userAnswersCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(userAnswersCaptor.capture())
        userAnswersCaptor.getValue.get(MonthlyFinalValidationContinuationPage) mustBe None
        userAnswersCaptor.getValue.get(AllSubcontractorDetailsAdded) mustBe Some(false)
        userAnswersCaptor.getValue.get(pages.monthlyreturns.IsResumedMonthlyReturnPage) mustBe Some(true)
      }
    }

    "must redirect to confirm cancel amendment page when cancel amendment is selected" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(GET, cancelUrl)

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.amend.routes.ConfirmCancelAmendmentYesNoController.onPageLoad().url
      }
    }
  }
}
