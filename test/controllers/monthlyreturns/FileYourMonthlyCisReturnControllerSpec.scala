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
import controllers.actions.{SchemeAction, SchemeActionImpl}
import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.requests.CisPath
import models.requests.CisPath.{CisId, CisOrg}
import models.{NormalMode, ReturnType, UserAnswers, requests}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.ReturnTypePage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{CisTaxpayerService, FormpRdsReconcileService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import views.html.PageNotFoundView

import java.time.{Clock, ZoneId}
import scala.concurrent.{ExecutionContext, Future}

class FileYourMonthlyCisReturnControllerSpec extends SpecBase with MockitoSugar {
  private given ExecutionContext = ExecutionContext.global

  private val cisId = cisTaxpayer.uniqueId
  private val ton   = cisTaxpayer.taxOfficeNumber
  private val tor   = cisTaxpayer.taxOfficeRef

  private val stubNotFoundView    = mock[PageNotFoundView]
  private val stubNotFoundContent = "PageNotFound"
  when(stubNotFoundView.apply()(any, any)) thenReturn play.twirl.api.Html(stubNotFoundContent)

  "FileYourMonthlyCisReturnController.startMonthlyReturn" - {

    "Org: stores ReturnType and returns OK" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          clock = Clock.fixed(UNIX_EPOCH, ZoneId.systemDefault)
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisOrg).url)

        val result = route(app, request).value
        status(result) mustBe OK

        val expectedUserAnswers = emptyUserAnswers.set(ReturnTypePage, MonthlyStandardReturn).get
        verify(mockRepo).set(expectedUserAnswers)
      }
    }

    "Agent: service finds client => stores ReturnType and returns OK" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())) thenReturn Future.successful(true)

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), isAgent = true)
          .overrides(bind[SessionRepository] toInstance mockRepo)
          .build()

      running(app) {
        val req = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisId(cisId)).url)

        val res = route(app, req).value
        status(res) mustBe OK

        val expectedUserAnswers = emptyUserAnswers.set(ReturnTypePage, MonthlyStandardReturn).get
        verify(mockRepo).set(expectedUserAnswers)
      }
    }

    "Agent: service does NOT find client => return 404 Not Found" in {
      val mockService = mock[CisTaxpayerService]
      when(mockService.findForAgent(any)(using any)) thenReturn Future.successful(None)

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          schemeAction = buildSchemeAction(mockService)
        )
          .overrides(bind[SessionRepository] toInstance mock[SessionRepository])
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisId(cisId)).url)

        val result = route(app, request).value
        status(result) mustBe NOT_FOUND
        contentAsString(result) mustBe stubNotFoundContent

        verify(mockService).findForAgent(eqTo(cisId))(using any)
      }
    }

    "Agent: service throws => redirect JourneyRecovery" in {
      val mockService = mock[CisTaxpayerService]
      when(mockService.findForAgent(any)(using any)) thenReturn Future.failed(new RuntimeException("boom"))

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          schemeAction = buildSchemeAction(mockService)
        )
          .overrides(bind[SessionRepository].toInstance(mock[SessionRepository]))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisId(cisId)).url)

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService).findForAgent(eqTo(cisId))(using any)
      }
    }
  }

  "FileYourMonthlyCisReturnController.onSubmit" - {

    "redirects to DateConfirmPayments for MonthlyStandardReturn and persists cleaned answers" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          routes.FileYourMonthlyCisReturnController.onSubmit(CisOrg, ReturnType.MonthlyStandardReturn).url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.DateConfirmPaymentsController
            .onPageLoad(CisOrg, NormalMode, Some(ReturnType.MonthlyStandardReturn))
            .url

        verify(mockRepo).set(any())
      }
    }

    "redirects to DateConfirmPayments for MonthlyNilReturn and persists cleaned answers" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          routes.FileYourMonthlyCisReturnController.onSubmit(CisOrg, ReturnType.MonthlyNilReturn).url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.DateConfirmPaymentsController
            .onPageLoad(CisOrg, NormalMode, Some(ReturnType.MonthlyNilReturn))
            .url

        verify(mockRepo).set(any())
      }
    }

    "redirects to JourneyRecovery when session repository fails" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())).thenReturn(Future.failed(new RuntimeException("boom")))

      val app =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(
          POST,
          routes.FileYourMonthlyCisReturnController.onSubmit(CisOrg, ReturnType.MonthlyStandardReturn).url
        )

        val result = route(app, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockRepo).set(any())
      }
    }
  }

  "FileYourMonthlyCisReturnController.startNilReturn" - {

    "Org: stores ReturnType and returns OK" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          clock = Clock.fixed(UNIX_EPOCH, ZoneId.systemDefault)
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startNilReturn(CisOrg).url)

        val result = route(app, request).value
        status(result) mustBe OK

        val expectedUserAnswers = emptyUserAnswers.set(ReturnTypePage, MonthlyNilReturn).get
        verify(mockRepo).set(expectedUserAnswers)
      }
    }

    "Agent: services finds client => stores ReturnType and returns OK" in {
      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())) thenReturn Future.successful(true)

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          clock = Clock.fixed(UNIX_EPOCH, ZoneId.systemDefault)
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val req = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startNilReturn(CisId(cisId)).url)

        val res = route(app, req).value
        status(res) mustBe OK

        val expectedUserAnswers = emptyUserAnswers.set(ReturnTypePage, MonthlyNilReturn).get
        verify(mockRepo).set(expectedUserAnswers)
      }
    }

    "Agent: services does NOT find client => return 404 Not Found" in {
      val mockService = mock[CisTaxpayerService]
      when(mockService.findForAgent(any)(using any)) thenReturn Future.successful(None)

      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())) thenReturn Future.successful(true)

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          schemeAction = buildSchemeAction(mockService)
        )
          .overrides(bind[SessionRepository] toInstance mockRepo)
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startNilReturn(CisId(cisId)).url)

        val result = route(app, request).value
        status(result) mustBe NOT_FOUND

        verify(mockService).findForAgent(eqTo(cisId))(using any)
        verifyNoInteractions(mockRepo)
      }
    }

    "Agent: service throws => redirect JourneyRecovery" in {
      val mockService = mock[CisTaxpayerService]
      when(mockService.findForAgent(any)(using any)) thenReturn Future.failed(new RuntimeException("boom"))

      val mockRepo = mock[SessionRepository]

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          schemeAction = buildSchemeAction(mockService)
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startNilReturn(CisId(cisId)).url)

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService).findForAgent(eqTo(cisId))(using any)
      }
    }
  }

  "FileYourMonthlyCisReturnController FormP/RDS reconciliation" - {

    "Org: reconcile fails with PRECONDITION_FAILED => redirect to register (UnauthorisedOrganisationAffinity)" in {
      val mockReconcile = mock[FormpRdsReconcileService]
      val mockRepo      = mock[SessionRepository]

      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier])) thenReturn Future.failed(
        UpstreamErrorResponse("missing", PRECONDITION_FAILED, PRECONDITION_FAILED)
      )

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          formpRdsReconcileService = mockReconcile
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisOrg).url)

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo(cisId), any(), any())(any[HeaderCarrier])
      }
    }

    "Org: reconcile fails with a general error => redirect JourneyRecovery" in {
      val mockReconcile = mock[FormpRdsReconcileService]
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier])) thenReturn Future.failed(
        new RuntimeException("boom")
      )

      val mockRepo = mock[SessionRepository]

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          formpRdsReconcileService = mockReconcile
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisOrg).url)

        val result = route(app, request).value
        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockReconcile).reconcile(eqTo(cisId), any(), any())(any[HeaderCarrier])
      }
    }

    "Org: reconcile succeeds => uses employer tax office reference and returns OK" in {
      val mockReconcile = mock[FormpRdsReconcileService]
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier])) thenReturn Future.unit

      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())).thenReturn(Future.successful(true))

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          formpRdsReconcileService = mockReconcile,
          clock = Clock.fixed(UNIX_EPOCH, ZoneId.systemDefault)
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisOrg).url)

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockReconcile).reconcile(eqTo(cisId), eqTo(ton), eqTo(tor))(any[HeaderCarrier])

        val expectedUserAnswers = emptyUserAnswers.set(ReturnTypePage, MonthlyStandardReturn).get
        verify(mockRepo).set(expectedUserAnswers)
      }
    }

    "Agent: reconcile uses agent client tax office reference and returns OK" in {
      val mockReconcile = mock[FormpRdsReconcileService]
      when(mockReconcile.reconcile(any(), any(), any())(any[HeaderCarrier])) thenReturn Future.unit

      val mockRepo = mock[SessionRepository]
      when(mockRepo.set(any())) thenReturn Future.successful(true)

      val app =
        applicationBuilder(
          userAnswers = Some(emptyUserAnswers),
          isAgent = true,
          formpRdsReconcileService = mockReconcile,
          clock = Clock.fixed(UNIX_EPOCH, ZoneId.systemDefault)
        )
          .overrides(bind[SessionRepository].toInstance(mockRepo))
          .build()

      running(app) {
        val request = FakeRequest(GET, routes.FileYourMonthlyCisReturnController.startMonthlyReturn(CisId(cisId)).url)

        val result = route(app, request).value
        status(result) mustBe OK

        verify(mockReconcile).reconcile(eqTo(cisId), eqTo(ton), eqTo(tor))(any[HeaderCarrier])

        val expectedUserAnswers = emptyUserAnswers.set(ReturnTypePage, MonthlyStandardReturn).get
        verify(mockRepo).set(expectedUserAnswers)
      }
    }
  }

  private def buildSchemeAction(service: CisTaxpayerService)(using ExecutionContext) =
    new SchemeActionImpl(stubMessagesApi(), applicationConfig, service, stubNotFoundView)
}
