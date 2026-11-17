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
import models.UserAnswers
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.CisIdPage
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.monthlyreturns.SubmissionUnsuccessfulResubmitView

import java.util.UUID
import scala.concurrent.Future

class SubmissionUnsuccessfulResubmitControllerSpec extends SpecBase with MockitoSugar {

  "ResubmissionUnsuccessful Controller" - {

    "must return OK and the correct view for a GET" in {

      val fakeCisId = "1"

      val mockMonthlyReturnService = mock[MonthlyReturnService]
      when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
        .overrides(
          bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.SubmissionUnsuccessfulResubmitController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[SubmissionUnsuccessfulResubmitView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(fakeCisId)(request, messages(application)).toString

        verify(mockMonthlyReturnService).completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier])
      }
    }

    "throw IllegalStateException when cisId is missing" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.SubmissionUnsuccessfulResubmitController.onPageLoad().url)

        val controller = application.injector.instanceOf[SubmissionUnsuccessfulResubmitController]

        val exception = controller.onPageLoad()(request).failed.futureValue

        exception mustBe a[IllegalStateException]
        exception.getMessage mustBe "[SubmissionUnsuccessfulResubmit] cisId missing from userAnswers"
      }
    }

    "UUID-scoped journey" - {

      "must return OK and the correct view using the ID-scoped cisId" in {

        val flowId    = UUID.randomUUID()
        val id        = flowId.toString
        val fakeCisId = "2"

        val userAnswers = userAnswersWithCisId
          .setOrException(CisIdPage(id), fakeCisId)

        val mockMonthlyReturnService = mock[MonthlyReturnService]

        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers], any[String])(any[HeaderCarrier]))
          .thenReturn(Future.unit)

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithId(flowId).url)

          val result = route(application, request).value

          val view = application.injector.instanceOf[SubmissionUnsuccessfulResubmitView]

          val exitUrl = controllers.monthlyreturns.routes.ManageCisReturnController
            .onExitWithId(flowId)
            .url

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(fakeCisId, exitUrl)(request, messages(application)).toString

          val idCaptor = org.mockito.ArgumentCaptor.forClass(classOf[String])

          verify(mockMonthlyReturnService)
            .completeSubmissionJourney(any[UserAnswers], idCaptor.capture())(any[HeaderCarrier])

          idCaptor.getValue mustBe id
        }
      }

      "must redirect to Journey Recovery when the flowId is missing" in {

        val flowId = UUID.randomUUID()

        val mockMonthlyReturnService = mock[MonthlyReturnService]

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .configure("features.multi-tab-enabled" -> true)
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.SubmissionUnsuccessfulResubmitController.onPageLoadWithId(flowId).url)
          val result  = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

          verify(mockMonthlyReturnService, never())
            .completeSubmissionJourney(any[UserAnswers], any[String])(any[HeaderCarrier])
        }
      }
    }
  }
}
