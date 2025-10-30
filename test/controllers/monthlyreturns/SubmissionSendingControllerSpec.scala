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
import play.api.test.FakeRequest
import models.UserAnswers
import models.submission.{ChrisSubmissionResponse, CreateSubmissionResponse, ResponseEndPointDto}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

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

  lazy val submissionSendingRoute: String =
    controllers.monthlyreturns.routes.SubmissionSendingController.onPageLoad().url
  private def mkRequest                   = FakeRequest(GET, submissionSendingRoute)

  private def successRoute      = controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad.url
  private def awaitingRoute     = controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad.url
  private def pollingRoute      = controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect.url
  private def unsuccessfulRoute = controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
  private def recoveryRoute     = controllers.routes.JourneyRecoveryController.onPageLoad().url
  private def systemErrorRoute  = controllers.routes.SystemErrorController.onPageLoad().url
  given hc: HeaderCarrier       = HeaderCarrier()

  private def stubSubmissionFlow(
    service: SubmissionService,
    sessionDb: SessionRepository,
    status: String,
    createdId: String = "sub-123",
    endpoint: Option[ResponseEndPointDto] = None,
    irMark: String = "Dj5TVJDyRYCn9zta5EdySeY4fyA="
  ): (CreateSubmissionResponse, ChrisSubmissionResponse) = {

    val created = CreateSubmissionResponse(submissionId = createdId)

    val submitted = ChrisSubmissionResponse(
      submissionId = createdId,
      status = status,
      hmrcMarkGenerated = irMark,
      correlationId = Some("CID123"),
      responseEndPoint = endpoint,
      gatewayTimestamp = Some("2025-01-01T00:00:00Z"),
      error = None
    )

    when(service.create(any[UserAnswers])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(created))

    when(service.submitToChrisAndPersist(eqTo(createdId), any[UserAnswers])(using any[HeaderCarrier]))
      .thenReturn(Future.successful(submitted))

    when(sessionDb.set(any[UserAnswers]))
      .thenReturn(Future.successful(true))

    when(service.updateSubmission(eqTo(createdId), any[UserAnswers], eqTo(submitted))(any[HeaderCarrier]))
      .thenReturn(Future.successful(()))

    (created, submitted)
  }

  "SubmissionSendingController.onPageLoad" - {

    "redirects to Success when status is SUBMITTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "SUBMITTED")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successRoute
      verify(mockService).updateSubmission(eqTo("sub-123"), any[UserAnswers], any())(any[HeaderCarrier])
    }

    "redirects to Success when status is SUBMITTED_NO_RECEIPT" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "SUBMITTED_NO_RECEIPT")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successRoute
    }

    "redirects to polling page when status is PENDING" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "PENDING")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute
    }

    "redirects to polling page when status is ACCEPTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "ACCEPTED")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe pollingRoute
    }

    "redirects to Unsuccessful when status is FATAL_ERROR" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "FATAL_ERROR")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
    }

    "redirects to Unsuccessful when status is DEPARTMENTAL_ERROR" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "DEPARTMENTAL_ERROR")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
    }

    "redirects to JourneyRecovery for unknown status" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "SOMETHING_ELSE")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute
    }

    "falls back to system error page when any step fails (e.g. create fails)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      when(mockService.create(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe systemErrorRoute

      verifyNoInteractions(mockMongoDb)
      verify(mockService, never()).submitToChrisAndPersist(any[String], any[UserAnswers])(any[HeaderCarrier])
      verify(mockService, never()).updateSubmission(any[String], any[UserAnswers], any())(any[HeaderCarrier])
    }

  }

  lazy val pollAndRedirectRoute: String =
    controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect.url
  private def mkPollRequest             = FakeRequest(GET, pollAndRedirectRoute)

  "SubmissionSendingController.onPollAndRedirect" - {

    "redirects to JourneyRecovery when SubmissionDetailsPage is missing" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val app = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute
      verifyNoInteractions(mockService)
    }

    "returns OK with Refresh header when status is PENDING" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("PENDING"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe OK
      headers(result).get("Refresh").value mustBe "10"
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "returns OK with Refresh header when status is ACCEPTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "ACCEPTED",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("ACCEPTED"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe OK
      headers(result).get("Refresh").value mustBe "10"
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to SubmissionAwaiting when status is TIMED_OUT" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("TIMED_OUT"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe awaitingRoute
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to SubmissionSuccess when status is SUBMITTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("SUBMITTED"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successRoute
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to SubmissionSuccess when status is SUBMITTED_NO_RECEIPT" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("SUBMITTED_NO_RECEIPT"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe successRoute
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to SubmissionUnsuccessful when status is FATAL_ERROR" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("FATAL_ERROR"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to SubmissionUnsuccessful when status is DEPARTMENTAL_ERROR" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("DEPARTMENTAL_ERROR"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe unsuccessfulRoute
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }

    "redirects to JourneyRecovery when status is unknown" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      import models.submission.SubmissionDetails
      import pages.submission.SubmissionDetailsPage
      import java.time.Instant

      val submissionDetails = SubmissionDetails(
        id = "sub-123",
        status = "PENDING",
        irMark = "IR-MARK-123",
        submittedAt = Instant.parse("2025-01-01T00:00:00Z")
      )
      val uaWithSubmission  = userAnswersWithCisId.set(SubmissionDetailsPage, submissionDetails).success.value

      when(mockService.checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier]))
        .thenReturn(Future.successful("UNKNOWN_STATUS"))

      val app = buildAppWith(Some(uaWithSubmission), mockService, mockMongoDb).build()
      val ctl = app.injector.instanceOf[SubmissionSendingController]

      val result = ctl.onPollAndRedirect()(mkPollRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute
      verify(mockService).checkAndUpdateSubmissionStatus(any[UserAnswers])(using any[HeaderCarrier])
    }
  }
}
