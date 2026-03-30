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
import models.{NormalMode, UserAnswers}
import models.monthlyreturns.{SelectedSubcontractor, UpdateMonthlyReturnItemRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.{MonthlyReturnItemPayloadBuilder, MonthlyReturnService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import viewmodels.checkAnswers.monthlyreturns.CheckAnswersTotalPaymentsViewModel
import views.html.monthlyreturns.CheckAnswersTotalPaymentsView

import scala.concurrent.Future

class CheckAnswersTotalPaymentsControllerSpec extends SpecBase with MockitoSugar {

  given HeaderCarrier = HeaderCarrier()

  private val index = 1

  private val subcontractor = SelectedSubcontractor(
    id = 1,
    name = "TyneWear Ltd",
    totalPaymentsMade = Some(BigDecimal("1200")),
    costOfMaterials = Some(BigDecimal("500")),
    totalTaxDeducted = Some(BigDecimal("240"))
  )

  "CheckAnswersTotalPayments Controller" - {

    "GET must return OK and the correct view" in {
      val ua          = userAnswersWithCisId.set(SelectedSubcontractorPage(index), subcontractor).success.value
      val application = applicationBuilder(userAnswers = Some(ua)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckAnswersTotalPaymentsController.onPageLoad(index).url)

        val result = route(application, request).value
        val view   = application.injector.instanceOf[CheckAnswersTotalPaymentsView]

        val expectedVm = CheckAnswersTotalPaymentsViewModel.fromModel(subcontractor)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedVm, index)(request, messages(application)).toString
      }
    }

    "GET must redirect to JourneyRecovery when subcontractor is missing" in {
      val application = applicationBuilder(userAnswers = Some(userAnswersWithCisId)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckAnswersTotalPaymentsController.onPageLoad(index).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "POST must redirect to JourneyRecovery when payloadBuilder returns None" in {
      val mockService = mock[MonthlyReturnService]
      val mockBuilder = mock[MonthlyReturnItemPayloadBuilder]

      when(mockBuilder.build(any[UserAnswers], any[Int])) thenReturn None

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockService),
            bind[MonthlyReturnItemPayloadBuilder].toInstance(mockBuilder)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckAnswersTotalPaymentsController.onSubmit(index).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        verify(mockService, never).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier])
      }
    }

    "POST must call MonthlyReturnService and redirect to SubcontractorDetailsAdded on success" in {
      val mockService = mock[MonthlyReturnService]
      val mockBuilder = mock[MonthlyReturnItemPayloadBuilder]

      val payload = UpdateMonthlyReturnItemRequest(
        instanceId = "i-123",
        taxYear = 2026,
        taxMonth = 3,
        subcontractorId = 1,
        subcontractorName = "TyneWear Ltd",
        totalPayments = "1,200.00",
        costOfMaterials = "500.00",
        totalDeducted = "240.00"
      )

      when(mockBuilder.build(any[UserAnswers], any[Int])) thenReturn Some(payload)
      when(mockService.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockService),
            bind[MonthlyReturnItemPayloadBuilder].toInstance(mockBuilder)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckAnswersTotalPaymentsController.onSubmit(index).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.SubcontractorDetailsAddedController.onPageLoad(NormalMode).url

        val captor = ArgumentCaptor.forClass(classOf[UpdateMonthlyReturnItemRequest])
        verify(mockService).updateMonthlyReturnItem(captor.capture())(any[HeaderCarrier])
        captor.getValue mustBe payload
      }
    }

    "POST must redirect to SystemError when MonthlyReturnService fails with UpstreamErrorResponse" in {
      val mockService = mock[MonthlyReturnService]
      val mockBuilder = mock[MonthlyReturnItemPayloadBuilder]

      val payload = UpdateMonthlyReturnItemRequest(
        instanceId = "i-123",
        taxYear = 2026,
        taxMonth = 3,
        subcontractorId = 1,
        subcontractorName = "TyneWear Ltd",
        totalPayments = "1,200.00",
        costOfMaterials = "500.00",
        totalDeducted = "240.00"
      )

      when(mockBuilder.build(any[UserAnswers], any[Int])) thenReturn Some(payload)
      when(mockService.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("boom", 500)))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockService),
            bind[MonthlyReturnItemPayloadBuilder].toInstance(mockBuilder)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckAnswersTotalPaymentsController.onSubmit(index).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "POST must redirect to SystemError when MonthlyReturnService throws a non-fatal exception" in {
      val mockService = mock[MonthlyReturnService]
      val mockBuilder = mock[MonthlyReturnItemPayloadBuilder]

      val payload = UpdateMonthlyReturnItemRequest(
        instanceId = "i-123",
        taxYear = 2026,
        taxMonth = 3,
        subcontractorId = 1,
        subcontractorName = "TyneWear Ltd",
        totalPayments = "1,200.00",
        costOfMaterials = "500.00",
        totalDeducted = "240.00"
      )

      when(mockBuilder.build(any[UserAnswers], any[Int])) thenReturn Some(payload)
      when(mockService.updateMonthlyReturnItem(any[UpdateMonthlyReturnItemRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithCisId))
          .overrides(
            bind[MonthlyReturnService].toInstance(mockService),
            bind[MonthlyReturnItemPayloadBuilder].toInstance(mockBuilder)
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckAnswersTotalPaymentsController.onSubmit(index).url)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

  }
}
