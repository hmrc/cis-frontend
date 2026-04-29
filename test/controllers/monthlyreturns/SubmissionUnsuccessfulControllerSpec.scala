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
import models.UserAnswers
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import play.api.inject.bind
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.monthlyreturns.SubmissionUnsuccessfulView

import scala.concurrent.Future

class SubmissionUnsuccessfulControllerSpec extends SpecBase with MockitoSugar {

  "SubmissionUnsuccessful Controller" - {

    "GET onPageLoad" - {

      "must return OK and the correct view when user answers exist" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        when(mockMonthlyReturnService.completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

        running(application) {
          val request   = FakeRequest(GET, routes.SubmissionUnsuccessfulController.onPageLoad.url)
          val fakeCisId = "1"
          val result    = route(application, request).value
          val view      = application.injector.instanceOf[SubmissionUnsuccessfulView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(fakeCisId)(request, messages(application)).toString

          verify(mockMonthlyReturnService).completeSubmissionJourney(any[UserAnswers])(any[HeaderCarrier])
        }
      }

      "must return OK and the correct view when cisId is provided in query param" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(
            GET,
            routes.SubmissionUnsuccessfulController.onPageLoad.url + "?cisId=123"
          )
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[SubmissionUnsuccessfulView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view("123")(request, messages(application)).toString
        }
      }

      "must throw exception when cisId is missing from both UserAnswers and query param" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, routes.SubmissionUnsuccessfulController.onPageLoad.url)
          val result  = route(application, request).value

          val ex = intercept[IllegalStateException] {
            await(result)
          }

          ex.getMessage must include("cisId missing from userAnswers")
        }
      }

      "must throw exception when no existing data is found and no cisId query param is provided" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, routes.SubmissionUnsuccessfulController.onPageLoad.url)
          val result  = route(application, request).value

          val ex = intercept[IllegalStateException] {
            await(result)
          }

          ex.getMessage must include("cisId missing from userAnswers")
        }
      }
    }
  }
}
