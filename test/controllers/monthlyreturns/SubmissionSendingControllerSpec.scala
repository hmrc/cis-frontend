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
import models.ChrisResult.*
import navigation.{FakeNavigator, Navigator}
import play.api.test.FakeRequest
import models.UserAnswers
import models.monthlyreturns.{Declaration, InactivityRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.Helpers.*
import play.api.libs.json.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class SubmissionSendingControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  "SubmissionSending Controller" - {

    val userAnswers = emptyUserAnswers
      .set(CisIdPage, "test-cis-id")
      .success
      .value
      .set(DateConfirmNilPaymentsPage, LocalDate.of(2024, 3, 1))
      .success
      .value
      .set(InactivityRequestPage, InactivityRequest.Option1)
      .success
      .value
      .set(ConfirmEmailAddressPage, "anything@test.com")
      .success
      .value
      .set(DeclarationPage, Set(Declaration.Confirmed))
      .success
      .value

    "onPageLoad must redirect to poll endpoint when service returns Pending" in {
      val mockService = mock[MonthlyReturnService]
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Pending))

      val app = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("features.stub-sending-enabled" -> false)
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(app) {
        val req = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)
        val res = route(app, req).value

        status(res) mustEqual SEE_OTHER
        redirectLocation(res).value mustEqual
          controllers.monthlyreturns.routes.SubmissionSendingController.onPollAndRedirect.url

        verify(mockService, times(1)).submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier])
      }
    }

    "onPageLoad must redirect to Success when service returns Submitted" in {
      val mockService = mock[MonthlyReturnService]
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Submitted))

      val app = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("features.stub-sending-enabled" -> false)
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(app) {
        val req = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)
        val res = route(app, req).value

        status(res) mustEqual SEE_OTHER
        redirectLocation(res).value mustEqual
          controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad.url

        verify(mockService, times(1)).submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier])
      }
    }

    "onPageLoad must redirect to Unsuccessful when service returns Rejected" in {
      val mockService = mock[MonthlyReturnService]
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Rejected(NOT_MODIFIED, "reason")))

      val app = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("features.stub-sending-enabled" -> false)
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(app) {
        val req = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)
        val res = route(app, req).value

        status(res) mustEqual SEE_OTHER
        redirectLocation(res).value mustEqual
          controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url

        verify(mockService, times(1)).submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier])
      }
    }

    "onPageLoad must redirect to Journey Recovery when service throws" in {
      val mockService = mock[MonthlyReturnService]
      when(mockService.submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val app = applicationBuilder(userAnswers = Some(userAnswers))
        .configure("features.stub-sending-enabled" -> false)
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(app) {
        val req = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)
        val res = route(app, req).value

        status(res) mustEqual SEE_OTHER
        redirectLocation(res).value mustEqual
          controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, times(1)).submitNilMonthlyReturn(any[UserAnswers])(any[HeaderCarrier])
      }
    }

    "onPageLoad must redirect to Unauthorised Organisation Affinity if cisId is not found in UserAnswer" in {

      val data: JsObject = Json.obj(
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Submissionsuccessful@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.UnauthorisedOrganisationAffinityController.onPageLoad().url
      }
    }

    "onPageLoad must return SEE_OTHER and redirect to the Submission Success page when user answer has email address as Submissionsuccessful@test.com" in {

      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Submissionsuccessful@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .configure("features.stub-sending-enabled" -> true)
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad.url
      }
    }

    "onPageLoad must return SEE_OTHER and redirect to the Submission Unsuccess page when user answer has email address as Submissionunsuccessful@test.com" in {

      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Submissionunsuccessful@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .configure("features.stub-sending-enabled" -> true)
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
      }
    }

    "onPageLoad must return SEE_OTHER and redirect to the Submission Awaiting page when user answer has email address as Awaitingconfirmation@test.com" in {

      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "Awaitingconfirmation@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .configure("features.stub-sending-enabled" -> true)
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad.url
      }
    }

    "onPageLoad must return SEE_OTHER and redirect to the Journey Recovery page when user answer has email address other than above 3 emails" in {

      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )

      val ua = UserAnswers("randomId", data)

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(ua))
        .configure("features.stub-sending-enabled" -> true)
        .overrides(
          bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
          bind[SessionRepository].toInstance(mockSessionRepository)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "onPageLoad must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPageLoad().url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "onPollAndRedirect should redirect to Success page if poll returns success" in {
      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )
      val ua             = UserAnswers("randomId", data)
        .set(
          SubmissionStatusPage,
          SubmissionStatus(
            "123",
            LocalDateTime.now,
            Pending,
            false
          )
        )
        .get

      val mockService = mock[MonthlyReturnService]
      when(mockService.checkAndUpdateSubmissionStatus(any[SubmissionStatus], any[UserAnswers]))
        .thenReturn(Future.successful(SubmissionStatus("123", LocalDateTime.now(), Submitted, false)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPollAndRedirect.url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionSuccessController.onPageLoad.url
      }
    }

    "onPollAndRedirect should redirect to Unsuccessful page if poll returns Rejected" in {
      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )
      val ua             = UserAnswers("randomId", data)
        .set(
          SubmissionStatusPage,
          SubmissionStatus(
            "123",
            LocalDateTime.now,
            Pending,
            false
          )
        )
        .get

      val mockService = mock[MonthlyReturnService]
      when(mockService.checkAndUpdateSubmissionStatus(any[SubmissionStatus], any[UserAnswers]))
        .thenReturn(Future.successful(SubmissionStatus("123", LocalDateTime.now(), Rejected(1, "Error"), false)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPollAndRedirect.url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
      }
    }

    "onPollAndRedirect should redirect to Unsuccessful page if poll returns UpstreamError" in {
      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )
      val ua             = UserAnswers("randomId", data)
        .set(
          SubmissionStatusPage,
          SubmissionStatus(
            "123",
            LocalDateTime.now,
            Pending,
            false
          )
        )
        .get

      val mockService = mock[MonthlyReturnService]
      when(mockService.checkAndUpdateSubmissionStatus(any[SubmissionStatus], any[UserAnswers]))
        .thenReturn(Future.successful(SubmissionStatus("123", LocalDateTime.now(), UpstreamFailed(1, "Error"), false)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPollAndRedirect.url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionUnsuccessfulController.onPageLoad.url
      }
    }

    "onPollAndRedirect should redirect to Awaiting page if poll times out" in {
      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )
      val ua             = UserAnswers("randomId", data)
        .set(
          SubmissionStatusPage,
          SubmissionStatus(
            "123",
            LocalDateTime.now,
            Pending,
            false
          )
        )
        .get

      val mockService = mock[MonthlyReturnService]
      when(mockService.checkAndUpdateSubmissionStatus(any[SubmissionStatus], any[UserAnswers]))
        .thenReturn(Future.successful(SubmissionStatus("123", LocalDateTime.now(), Pending, true)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPollAndRedirect.url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(
          result
        ).value mustEqual controllers.monthlyreturns.routes.SubmissionAwaitingController.onPageLoad.url
      }
    }

    "onPollAndRedirect should remain on polling page with refresh header if poll is still pending and has not timed out" in {
      val data: JsObject = Json.obj(
        "cisId"                  -> "1",
        "dateConfirmNilPayments" -> "2019-09-05",
        "inactivityRequest"      -> "option1",
        "confirmEmailAddress"    -> "AnyOther@test.com",
        "declaration"            -> Json.arr("confirmed")
      )
      val ua             = UserAnswers("randomId", data)
        .set(
          SubmissionStatusPage,
          SubmissionStatus(
            "123",
            LocalDateTime.now,
            Pending,
            false
          )
        )
        .get

      val mockService = mock[MonthlyReturnService]
      when(mockService.checkAndUpdateSubmissionStatus(any[SubmissionStatus], any[UserAnswers]))
        .thenReturn(Future.successful(SubmissionStatus("123", LocalDateTime.now(), Pending, false)))

      val application = applicationBuilder(userAnswers = Some(ua))
        .overrides(bind[MonthlyReturnService].toInstance(mockService))
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionSendingController.onPollAndRedirect.url)
        val result  = route(application, request).value

        status(result) mustEqual OK
        header("Refresh", result) mustEqual Some("1")
      }
    }
  }
}
