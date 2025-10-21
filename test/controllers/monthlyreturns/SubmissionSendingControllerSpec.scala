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
import models.submission.{ChrisSubmissionResponse, CreateAndTrackSubmissionResponse, ResponseEndPointDto}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.submission.*
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Writes
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.util.Failure

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
  private def unsuccessfulRoute = controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
  private def recoveryRoute     = controllers.routes.JourneyRecoveryController.onPageLoad().url

  private def stubSubmissionFlow(
    service: SubmissionService,
    sessionDb: SessionRepository,
    status: String,
    createdId: String = "sub-123",
    endpoint: Option[ResponseEndPointDto] = None,
    irMark: String = "Dj5TVJDyRYCn9zta5EdySeY4fyA="
  ): (CreateAndTrackSubmissionResponse, ChrisSubmissionResponse) = {

    val created = CreateAndTrackSubmissionResponse(submissionId = createdId)

    val submitted = ChrisSubmissionResponse(
      submissionId = createdId,
      status = status,
      hmrcMarkGenerated = irMark,
      correlationId = Some("CID123"),
      responseEndPoint = endpoint,
      gatewayTimestamp = Some("2025-01-01T00:00:00Z"),
      error = None
    )

    when(service.createAndTrack(any[UserAnswers])(any[HeaderCarrier]))
      .thenReturn(Future.successful(created))

    when(service.submitToChris(eqTo(createdId), any[UserAnswers])(any[HeaderCarrier]))
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
      verify(mockMongoDb, atLeastOnce()).set(any[UserAnswers])
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

    "redirects to Awaiting when status is PENDING" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "PENDING")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe awaitingRoute
    }

    "redirects to Awaiting when status is ACCEPTED" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "ACCEPTED")

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe awaitingRoute
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

    "falls back to JourneyRecovery when any step fails (e.g. create fails)" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      when(mockService.createAndTrack(any[UserAnswers])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verifyNoInteractions(mockMongoDb)
      verify(mockService, never()).submitToChris(any[String], any[UserAnswers])(any[HeaderCarrier])
      verify(mockService, never()).updateSubmission(any[String], any[UserAnswers], any())(any[HeaderCarrier])
    }

    "falls back to JourneyRecovery when persisting to FE Mongo fails" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]
      stubSubmissionFlow(mockService, mockMongoDb, status = "PENDING")

      when(mockMongoDb.set(any[UserAnswers]))
        .thenReturn(Future.failed(new RuntimeException("mongo down")))

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verify(mockService, never()).updateSubmission(any[String], any[UserAnswers], any())(any[HeaderCarrier])
    }

    "persists IRMark and endpoint when ack contains responseEndPoint" in {
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      val ep = ResponseEndPointDto("https://poll.example/CID-123", 15)
      stubSubmissionFlow(
        mockService,
        mockMongoDb,
        status = "PENDING",
        endpoint = Some(ep),
        irMark = "Dj5TVJDyRYCn9zta5EdySeY4fyA="
      )

      val uaCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
      when(mockMongoDb.set(uaCaptor.capture())).thenReturn(Future.successful(true))

      val app        = buildAppWith(Some(userAnswersWithCisId), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]

      val result = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe awaitingRoute

      val savedUa = uaCaptor.getValue
      savedUa.get(SubmissionIdPage).value mustBe "sub-123"
      savedUa.get(SubmissionStatusPage).value mustBe "PENDING"
      savedUa.get(IrMarkPage).value mustBe "Dj5TVJDyRYCn9zta5EdySeY4fyA="
      savedUa.get(PollUrlPage).value mustBe "https://poll.example/CID-123"
      savedUa.get(PollIntervalPage).value mustBe 15
    }

    "falls back to JourneyRecovery when updating UserAnswers fails before persisting" in { // <-- NEW
      val mockService = mock[SubmissionService]
      val mockMongoDb = mock[SessionRepository]

      stubSubmissionFlow(mockService, mockMongoDb, status = "PENDING")
      val uaSpy = spy(userAnswersWithCisId)
      doReturn(Failure(new RuntimeException("UA set failed")))
        .when(uaSpy)
        .set(eqTo(SubmissionIdPage), eqTo("sub-123"))(any[Writes[String]])

      val app        = buildAppWith(Some(uaSpy), mockService, mockMongoDb).build()
      val controller = app.injector.instanceOf[SubmissionSendingController]
      val result     = controller.onPageLoad()(mkRequest)

      status(result) mustBe SEE_OTHER
      redirectLocation(result).value mustBe recoveryRoute

      verify(mockMongoDb, never()).set(any[UserAnswers])
      verify(mockService, never()).updateSubmission(any[String], any[UserAnswers], any())(any[HeaderCarrier])
    }
  }
}
