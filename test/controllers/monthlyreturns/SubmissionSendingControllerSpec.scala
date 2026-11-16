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
import models.monthlyreturns.Declaration.Confirmed
import models.requests.{CisIdDataRequest, DataRequest}
import models.submission.*
import models.{ReturnType, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import pages.submission.*
import play.api.http.Status.{NOT_FOUND, PRECONDITION_FAILED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.FormpRdsReconcileService
import services.submission.SubmissionService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID
import scala.concurrent.Future

final class SubmissionSendingControllerSpec extends SpecBase with MockitoSugar {

  private def buildAppWith(
    ua: Option[UserAnswers],
    service: SubmissionService,
    sessionDb: SessionRepository
  ): GuiceApplicationBuilder =
    applicationBuilder(userAnswers = ua)
      .overrides(
        bind[SubmissionService].toInstance(service),
        bind[SessionRepository].toInstance(sessionDb)
      )

  private val completeAnswers = emptyUserAnswers
    .setOrException(ReturnTypePage, ReturnType.MonthlyNilReturn)
    .setOrException(CisIdPage, "test-cis-id")
    .setOrException(DateConfirmPaymentsPage, LocalDate.of(2024, 3, 1))
    .setOrException(SubmitInactivityRequestPage, true)
    .setOrException(ConfirmationByEmailPage, false)
    .setOrException(DeclarationPage, Set(Confirmed))

  lazy val submissionSendingRoute: String =
    controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoad().url
  private def mkRequest                   = FakeRequest(GET, submissionSendingRoute)

  private def successRoute              = controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad.url
  private def successNoReceiptRoute     = controllers.monthlyreturns.routes.SubmittedNoReceiptController.onPageLoad.url
  private def awaitingRoute             = controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad.url
  private def pollingRoute              = controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect.url
  private def unsuccessfulRoute         = controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
  private def unsuccessfulResubmitRoute =
    controllers.monthlyreturns.routes.SubmissionUnsuccessfulResubmitController.onPageLoad().url
  private def recoveryRoute             = controllers.routes.JourneyRecoveryController.onPageLoad().url
  private def systemErrorRoute          = controllers.routes.SystemErrorController.onPageLoad().url
  private def unauthorisedRoute         =
    controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
  given hc: HeaderCarrier               = HeaderCarrier()

  private def stubSubmissionFlow(
    service: SubmissionService,
    status: String,
    createdId: String = "sub-123",
    endpoint: Option[ResponseEndPointDto] = None,
    irMark: String = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
    isResubmission: Boolean = false
  ): (String, UserAnswers, ChrisSubmissionResponse) = {

    val updatedAnswers =
      completeAnswers
        .set(SubmissionCreatedPage("submission-period"), true)
        .success
        .value

    val submitted = ChrisSubmissionResponse(
      submissionId = createdId,
      status = status,
      hmrcMarkGenerated = irMark,
      correlationId = Some("CID123"),
      responseEndPoint = endpoint,
      gatewayTimestamp = Some("2025-01-01T00:00:00"),
      error = None
    )

    when(service.getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier]))
      .thenReturn(
        Future.successful((createdId, updatedAnswers, isResubmission))
      )

    when(
      service.submitToChrisAndPersist(
        eqTo(createdId),
        eqTo(updatedAnswers),
        any[Boolean],
        eqTo(isResubmission)
      )(using any[HeaderCarrier])
    ).thenReturn(Future.successful(submitted))

    when(
      service.updateSubmissionFromChrisResponse(
        eqTo(createdId),
        eqTo(updatedAnswers),
        eqTo(submitted)
      )(
        any[CisIdDataRequest[AnyContent]],
        any[HeaderCarrier]
      )
    ).thenReturn(Future.successful(()))

    (createdId, updatedAnswers, submitted)
  }

  "SubmissionSendingController.onPageLoad" - {

    "redirects to polling page when status is PENDING" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, status = "PENDING")

      val app        = buildAppWith(Some(completeAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute
    }

    "redirects to polling page when status is ACCEPTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, status = "ACCEPTED")

      val app        = buildAppWith(Some(completeAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute
    }

    "redirects to Unsuccessful when status is FATAL_ERROR" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, status = "FATAL_ERROR")

      val app        = buildAppWith(Some(completeAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
    }

    "falls back to system error page when any step fails (e.g. getOrCreateSubmissionForChris fails)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      when(mockService.getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val app        = buildAppWith(Some(completeAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe systemErrorRoute

      verifyNoInteractions(mockMongoDb)
      verify(mockService, never()).submitToChrisAndPersist(any[String], any[UserAnswers], any[Boolean], any[Boolean])(
        any[HeaderCarrier]
      )
      verify(mockService, never()).updateSubmissionFromChrisResponse(any[String], any[UserAnswers], any())(
        any[CisIdDataRequest[AnyContent]],
        any[HeaderCarrier]
      )
    }

    "redirects to Unauthorised (CRR3) and does not submit to ChRIS when FormP/RDS reconciliation reports missing known facts" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val failingReconcile = new FormpRdsReconcileService {
        override def reconcile(instanceId: String, taxOfficeNumber: String, taxOfficeReference: String)(implicit
          hc: HeaderCarrier
        ): Future[Unit] =
          Future.failed(UpstreamErrorResponse("missing", PRECONDITION_FAILED, PRECONDITION_FAILED))
      }

      val app =
        applicationBuilder(userAnswers = Some(completeAnswers), formpRdsReconcileService = failingReconcile)
          .overrides(
            bind[SubmissionService].toInstance(mockService),
            bind[SessionRepository].toInstance(mockMongoDb)
          )
          .build()

      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unauthorisedRoute

      verify(mockService, never()).getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier])
      verify(mockService, never()).submitToChrisAndPersist(any[String], any[UserAnswers], any[Boolean], any[Boolean])(
        any[HeaderCarrier]
      )
    }

    "redirects to Unauthorised (CRR3) and does not submit to ChRIS when FormP/RDS reconciliation reports known facts not found" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val failingReconcile = new FormpRdsReconcileService {
        override def reconcile(instanceId: String, taxOfficeNumber: String, taxOfficeReference: String)(implicit
          hc: HeaderCarrier
        ): Future[Unit] =
          Future.failed(UpstreamErrorResponse("not found", NOT_FOUND, NOT_FOUND))
      }

      val app =
        applicationBuilder(userAnswers = Some(completeAnswers), formpRdsReconcileService = failingReconcile)
          .overrides(
            bind[SubmissionService].toInstance(mockService),
            bind[SessionRepository].toInstance(mockMongoDb)
          )
          .build()

      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unauthorisedRoute

      verify(mockService, never()).getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier])
      verify(mockService, never()).submitToChrisAndPersist(any[String], any[UserAnswers], any[Boolean], any[Boolean])(
        any[HeaderCarrier]
      )
    }

    "redirects to Journey Recovery and does not submit to ChRIS when FormP/RDS reconciliation fails unexpectedly" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val failingReconcile = new FormpRdsReconcileService {
        override def reconcile(instanceId: String, taxOfficeNumber: String, taxOfficeReference: String)(implicit
          hc: HeaderCarrier
        ): Future[Unit] =
          Future.failed(new RuntimeException("boom"))
      }

      val app =
        applicationBuilder(userAnswers = Some(completeAnswers), formpRdsReconcileService = failingReconcile)
          .overrides(
            bind[SubmissionService].toInstance(mockService),
            bind[SessionRepository].toInstance(mockMongoDb)
          )
          .build()

      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verify(mockService, never()).getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier])
      verify(mockService, never()).submitToChrisAndPersist(any[String], any[UserAnswers], any[Boolean], any[Boolean])(
        any[HeaderCarrier]
      )
    }

    "redirects to Submission Unsuccessful Resubmit page when status is STARTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, status = "STARTED")

      val app        = buildAppWith(Some(completeAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulResubmitRoute
    }

    "resumes polling without refreshing, creating or submitting when the submission was already sent to ChRIS" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val sentAnswers = completeAnswers
        .set(
          SubmissionDetailsPage,
          SubmissionDetails("sub-123", "PENDING", "irmark", LocalDateTime.now(), None, None)
        )
        .success
        .value

      val app        = buildAppWith(Some(sentAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute

      verifyNoInteractions(mockService)
    }

    "allows a retry when the stored submission status is STARTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, status = "STARTED")

      val startedAnswers = completeAnswers
        .set(
          SubmissionDetailsPage,
          SubmissionDetails("sub-123", "STARTED", "irmark", LocalDateTime.now(), None, None)
        )
        .success
        .value

      val app        = buildAppWith(Some(startedAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulResubmitRoute
      verify(mockService).getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to AlreadySubmitted when submission journey is already completed" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val completedAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionJourneyCompletedPage("2025-08"), true)
        .success
        .value

      val app        = buildAppWith(Some(completedAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url

      verifyNoInteractions(mockService)
      verifyNoInteractions(mockMongoDb)
    }

    "redirects to JourneyRecovery when journey is incomplete" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val incompleteAnswers = completeAnswers
        .remove(DeclarationPage)
        .success
        .value

      val app        = buildAppWith(Some(incompleteAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verifyNoInteractions(mockService)
      verifyNoInteractions(mockMongoDb)
    }

    "redirects to JourneyRecovery when DateConfirmPaymentsPage is missing" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verifyNoInteractions(mockService)
      verifyNoInteractions(mockMongoDb)
    }

    "passes isResubmission=true to submitToChrisAndPersist when an existing submission is reused" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val (createdId, updatedAnswers, submitted) =
        stubSubmissionFlow(service = mockService, status = "PENDING", isResubmission = true)

      val app        = buildAppWith(Some(completeAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute

      verify(mockService).getOrCreateSubmissionForChris(
        any[UserAnswers]
      )(using any[HeaderCarrier])

      verify(mockService).submitToChrisAndPersist(
        eqTo(createdId),
        eqTo(updatedAnswers),
        any[Boolean],
        eqTo(true)
      )(using any[HeaderCarrier])

      verify(mockService).updateSubmissionFromChrisResponse(
        eqTo(createdId),
        eqTo(updatedAnswers),
        eqTo(submitted)
      )(
        any[CisIdDataRequest[AnyContent]],
        any[HeaderCarrier]
      )
    }
  }

  "SubmissionSendingController.onPageLoad (resubmission path via ResubmissionIdPage)" - {

    val existingSubId   = 99L
    val resubmitAnswers = completeAnswers
      .setOrException(ResubmissionIdPage, existingSubId)

    def stubResubmissionFlow(
      service: SubmissionService,
      sessionDb: SessionRepository,
      status: String
    ): ChrisSubmissionResponse = {

      val submitted = ChrisSubmissionResponse(
        submissionId = existingSubId.toString,
        status = status,
        hmrcMarkGenerated = "IR-MARK",
        correlationId = Some("CID-RESUB"),
        responseEndPoint = None,
        gatewayTimestamp = Some("2025-01-01T00:00:00"),
        error = None
      )

      when(
        service.getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier])
      ).thenReturn(Future.successful((existingSubId.toString, resubmitAnswers, true)))

      when(
        service.submitToChrisAndPersist(eqTo(existingSubId.toString), any[UserAnswers], any[Boolean], eqTo(true))(using
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(submitted))

      when(sessionDb.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      when(
        service.updateSubmissionFromChrisResponse(eqTo(existingSubId.toString), any[UserAnswers], eqTo(submitted))(
          any[CisIdDataRequest[AnyContent]],
          any[HeaderCarrier]
        )
      ).thenReturn(Future.successful(()))

      submitted
    }

    "uses ResubmissionIdPage to skip creation and passes isResubmission=true (PENDING)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubResubmissionFlow(mockService, mockMongoDb, status = "PENDING")

      val app        = buildAppWith(Some(resubmitAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute

      verify(mockService).getOrCreateSubmissionForChris(any[UserAnswers])(using any[HeaderCarrier])
      verify(mockService, never()).create(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to polling when resubmission status is ACCEPTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubResubmissionFlow(mockService, mockMongoDb, status = "ACCEPTED")

      val app        = buildAppWith(Some(resubmitAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute
    }

    "redirects to Unsuccessful Resubmit when resubmission status is STARTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubResubmissionFlow(mockService, mockMongoDb, status = "STARTED")

      val app        = buildAppWith(Some(resubmitAnswers), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulResubmitRoute
    }
  }

  lazy val pollAndRedirectRoute: String =
    controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect.url
  private def mkPollRequest             = FakeRequest(GET, pollAndRedirectRoute)

  "SubmissionSendingController.onPollAndRedirect" - {

    "redirects to JourneyRecovery when SubmissionDetailsPage is missing" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val userAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value

      val app = buildAppWith(Some(userAnswers), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute
      verify(mockService, never()).getRefreshInterval
      verify(mockService, never()).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "returns OK with Refresh header when decision is Skip" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Skip))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe OK
      headers(result).get("Refresh").value mustBe "10"
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "returns OK with Refresh header when decision is Polled(PENDING)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("PENDING")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe OK
      headers(result).get("Refresh").value mustBe "10"
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "returns OK with Refresh header when decision is Polled(ACCEPTED)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "ACCEPTED",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("ACCEPTED")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe OK
      headers(result).get("Refresh").value mustBe "10"
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "redirects to SubmissionAwaiting when decision is Polled(TIMED_OUT)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("TIMED_OUT")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe awaitingRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "redirects to SubmissionSuccess when decision is Polled(SUBMITTED)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("SUBMITTED")))

      when(mockService.sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(uaWithSubmission))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
      verify(mockService).sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier])
    }

    "redirects to SubmissionSuccess when decision is Polled(SUBMITTED) even if sending email fails" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("SUBMITTED")))

      when(mockService.sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("email down")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successRoute

      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
      verify(mockService).sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier])
    }

    "redirects to SubmittedNoReceipt when decision is Polled(SUBMITTED_NO_RECEIPT)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("SUBMITTED_NO_RECEIPT")))

      when(mockService.sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(uaWithSubmission))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successNoReceiptRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
      verify(mockService).sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier])
    }

    "redirects to SubmittedNoReceipt when decision is Polled(SUBMITTED_NO_RECEIPT) even if sending email fails" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("SUBMITTED_NO_RECEIPT")))

      when(mockService.sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("email down")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successNoReceiptRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
      verify(mockService).sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier])
    }

    "redirects to SubmissionUnsuccessful when decision is Polled(FATAL_ERROR)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("FATAL_ERROR")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "redirects to SubmissionUnsuccessful when decision is Polled(DEPARTMENTAL_ERROR)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("DEPARTMENTAL_ERROR")))

      when(mockService.sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(uaWithSubmission))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "redirects to SubmissionUnsuccessful when decision is Polled(DEPARTMENTAL_ERROR) even if sending email fails" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("DEPARTMENTAL_ERROR")))

      when(mockService.sendSuccessEmail(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("email down")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "redirects to JourneyRecovery when decision is Polled with unknown status" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("UNKNOWN_STATUS")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute
      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }

    "redirects to SubmissionUnsuccessfulResubmit when decision is STARTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )
      val uaWithSubmission  = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      )
        .thenReturn(Future.successful(PollDecision.Polled("STARTED")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulResubmitRoute
    }

    "redirects to AlreadySubmitted when submission journey is already completed" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val completedAnswers = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionJourneyCompletedPage("2025-08"), true)
        .success
        .value

      val app = buildAppWith(Some(completedAnswers), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe
        controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoad().url

      verifyNoInteractions(mockService)
      verifyNoInteractions(mockMongoDb)
    }

    "redirects to JourneyRecovery when checking submission status fails" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
      )

      val uaWithSubmission = userAnswersWithCisId
        .set(DateConfirmPaymentsPage, LocalDate.of(2025, 8, 5))
        .success
        .value
        .set(SubmissionDetailsPage, submissionDetails)
        .success
        .value

      when(mockService.getRefreshInterval)
        .thenReturn(10)

      when(
        mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
          any[HeaderCarrier],
          any[CisIdDataRequest[AnyContent]]
        )
      ).thenReturn(Future.failed(new RuntimeException("poll failed")))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verify(mockService).getRefreshInterval
      verify(mockService).checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers])(using
        any[HeaderCarrier],
        any[CisIdDataRequest[AnyContent]]
      )
    }
  }

  "UUID-scoped journey" - {

    val flowId = UUID.randomUUID()
    val id     = flowId.toString

    val scopedAnswers = completeAnswers
      .setOrException(CisIdPage(id), "test-cis-id")
      .setOrException(ReturnTypePage(id), ReturnType.MonthlyNilReturn)
      .setOrException(DateConfirmPaymentsPage(id), LocalDate.of(2024, 3, 1))
      .setOrException(SubmitInactivityRequestPage(id), true)
      .setOrException(ConfirmationByEmailPage(id), false)
      .setOrException(DeclarationPage(id), Set(Confirmed))

    val submissionDetails = SubmissionDetails(
      id = "sub-123",
      status = "PENDING",
      irMark = "IR-MARK-123",
      submittedAt = LocalDateTime.parse("2025-01-01T00:00:00")
    )

    "must submit the ID-scoped return to ChRIS and redirect to polling" in {

      val mockService = mock[SubmissionService]

      val reconcileService = new FormpRdsReconcileService {
        override def reconcile(instanceId: String, taxOfficeNumber: String, taxOfficeReference: String)(implicit
          hc: HeaderCarrier
        ): Future[Unit] =
          Future.unit
      }

      val submitted = ChrisSubmissionResponse(
        submissionId = "sub-123",
        status = "ACCEPTED",
        hmrcMarkGenerated = "Dj5TVJDyRYCn9zta5EdySeY4fyA=",
        correlationId = Some("CID123"),
        responseEndPoint = None,
        gatewayTimestamp = Some("2025-01-01T00:00:00"),
        error = None
      )

      when(mockService.getOrCreateSubmissionForChris(any[UserAnswers], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(("sub-123", scopedAnswers, false)))

      when(
        mockService.submitToChrisAndPersist(
          any[String],
          any[UserAnswers],
          any[String],
          any[Boolean],
          any[Boolean]
        )(using any[HeaderCarrier])
      ).thenReturn(Future.successful(submitted))

      when(
        mockService.updateSubmissionFromChrisResponse(
          any[String],
          any[UserAnswers],
          any[String],
          any[ChrisSubmissionResponse]
        )(any[DataRequest[AnyContent]], any[HeaderCarrier])
      ).thenReturn(Future.unit)

      val application = applicationBuilder(
        userAnswers = Some(scopedAnswers),
        formpRdsReconcileService = reconcileService
      )
        .configure("features.multi-tab-enabled" -> true)
        .overrides(bind[SubmissionService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoadWithId(flowId).url
        )

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url

        val idCaptor = ArgumentCaptor.forClass(classOf[String])

        verify(mockService)
          .getOrCreateSubmissionForChris(any[UserAnswers], idCaptor.capture())(using any[HeaderCarrier])

        idCaptor.getValue mustBe id

        verify(mockService)
          .submitToChrisAndPersist(
            any[String],
            any[UserAnswers],
            idCaptor.capture(),
            any[Boolean],
            any[Boolean]
          )(using any[HeaderCarrier])

        idCaptor.getValue mustBe id

        verify(mockService)
          .updateSubmissionFromChrisResponse(
            any[String],
            any[UserAnswers],
            idCaptor.capture(),
            any[ChrisSubmissionResponse]
          )(any[DataRequest[AnyContent]], any[HeaderCarrier])

        idCaptor.getValue mustBe id
      }
    }

    "must resume ID-scoped polling without submitting again" in {

      val userAnswers = scopedAnswers
        .setOrException(SubmissionDetailsPage(id), submissionDetails)

      val mockService = mock[SubmissionService]

      val application = buildAppWith(Some(userAnswers), mockService, mock[SessionRepository])
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoadWithId(flowId).url
        )

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url

        verifyNoInteractions(mockService)
      }
    }

    "must redirect to ID-scoped Already Submitted when the journey is completed" in {

      val userAnswers = scopedAnswers
        .setOrException(SubmissionJourneyCompletedForFlowPage(id, "2024-03"), true)

      val mockService = mock[SubmissionService]

      val application = buildAppWith(Some(userAnswers), mockService, mock[SessionRepository])
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        Seq(
          controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoadWithId(flowId).url,
          controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url
        ).foreach { url =>

          val result = route(application, FakeRequest(GET, url)).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe
            controllers.monthlyreturns.routes.AlreadySubmittedController.onPageLoadWithId(flowId).url
        }

        verifyNoInteractions(mockService)
      }
    }

    "must redirect to Journey Recovery when the ID-scoped payment date is missing" in {

      val userAnswers = scopedAnswers
        .remove(DateConfirmPaymentsPage(id))
        .success
        .value

      val mockService = mock[SubmissionService]

      val application = buildAppWith(Some(userAnswers), mockService, mock[SessionRepository])
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        Seq(
          controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoadWithId(flowId).url,
          controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url
        ).foreach { url =>

          val result = route(application, FakeRequest(GET, url)).value

          status(result) mustBe SEE_OTHER
          redirectLocation(result).value mustBe recoveryRoute
        }

        verifyNoInteractions(mockService)
      }
    }

    "must redirect to Journey Recovery when ID-scoped submission details are missing" in {

      val userAnswers = scopedAnswers
        .setOrException(SubmissionDetailsPage, submissionDetails)

      val mockService = mock[SubmissionService]

      val application = buildAppWith(Some(userAnswers), mockService, mock[SessionRepository])
        .configure("features.multi-tab-enabled" -> true)
        .build()

      running(application) {
        val request = FakeRequest(
          GET,
          controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url
        )

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe recoveryRoute

        verifyNoInteractions(mockService)
      }
    }

    "must handle ID-scoped polling decisions and redirect to the correct pages" in {

      val userAnswers = scopedAnswers
        .setOrException(SubmissionDetailsPage(id), submissionDetails)

      val cases = Seq(
        (PollDecision.Skip, None),
        (PollDecision.Polled("PENDING"), None),
        (
          PollDecision.Polled("STARTED"),
          Some(controllers.monthlyreturns.routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithId(flowId).url)
        ),
        (
          PollDecision.Polled("TIMED_OUT"),
          Some(controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoadWithId(flowId).url)
        ),
        (
          PollDecision.Polled("SUBMITTED"),
          Some(controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoadWithId(flowId).url)
        ),
        (
          PollDecision.Polled("SUBMITTED_NO_RECEIPT"),
          Some(controllers.monthlyreturns.routes.SubmittedNoReceiptController.onPageLoadWithId(flowId).url)
        ),
        (
          PollDecision.Polled("DEPARTMENTAL_ERROR"),
          Some(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoadWithId(flowId).url)
        ),
        (
          PollDecision.Polled("FATAL_ERROR"),
          Some(controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoadWithId(flowId).url)
        )
      )

      cases.foreach { case (decision, expectedUrl) =>
        val mockService = mock[SubmissionService]

        when(mockService.getRefreshInterval)
          .thenReturn(10)

        when(
          mockService.checkAndUpdateSubmissionStatusIfAllowed(any[UserAnswers], any[String])(using
            any[HeaderCarrier],
            any[DataRequest[AnyContent]]
          )
        ).thenReturn(Future.successful(decision))

        when(mockService.sendSuccessEmail(any[UserAnswers], any[String], any[String])(using any[HeaderCarrier]))
          .thenReturn(Future.successful(userAnswers))

        val application = buildAppWith(Some(userAnswers), mockService, mock[SessionRepository])
          .configure("features.multi-tab-enabled" -> true)
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirectWithId(flowId).url
          )

          val result = route(application, request).value

          expectedUrl match {
            case None =>
              status(result) mustBe OK
              headers(result).get("Refresh").value mustBe "10"

            case Some(url) =>
              status(result) mustBe SEE_OTHER
              redirectLocation(result).value mustBe url
          }

          val idCaptor = ArgumentCaptor.forClass(classOf[String])

          verify(mockService)
            .checkAndUpdateSubmissionStatusIfAllowed(
              any[UserAnswers],
              idCaptor.capture()
            )(using
              any[HeaderCarrier],
              any[DataRequest[AnyContent]]
            )

          idCaptor.getValue mustBe id
        }
      }
    }
  }
}
