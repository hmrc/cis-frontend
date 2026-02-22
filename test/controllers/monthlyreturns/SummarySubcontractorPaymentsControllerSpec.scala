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
import models.monthlyreturns.SelectedSubcontractor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.SelectedSubcontractorPage
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import views.html.monthlyreturns.SummarySubcontractorPaymentsView

import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode

class SummarySubcontractorPaymentsControllerSpec extends SpecBase with MockitoSugar {

  private val mockSessionRepository = mock[SessionRepository]

  private def buildUserAnswers(subcontractors: Map[Int, SelectedSubcontractor]) =
    subcontractors.foldLeft(emptyUserAnswers) { case (ua, (index, sub)) =>
      ua.set(SelectedSubcontractorPage(index), sub).success.value
    }

  lazy val summaryRoute =
    controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController.onPageLoad().url

  "SummarySubcontractorPayments Controller" - {

    "must return OK and render totals calculated from subcontractors with complete details" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val subcontractors = Map(
        1 -> SelectedSubcontractor(101, "Alpha Ltd",  Some(BigDecimal(1000)), Some(BigDecimal(200)), Some(BigDecimal(160))),
        2 -> SelectedSubcontractor(102, "Beta Ltd",   Some(BigDecimal(2000)), Some(BigDecimal(400)), Some(BigDecimal(320))),
        3 -> SelectedSubcontractor(103, "Gamma Ltd",  Some(BigDecimal(600)),  Some(BigDecimal(300)), Some(BigDecimal(60)))
      )

      val userAnswers = buildUserAnswers(subcontractors)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, summaryRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[SummarySubcontractorPaymentsView]

        val expectedPayments      = BigDecimal(3600).setScale(2, RoundingMode.HALF_DOWN)
        val expectedMaterialsCost = BigDecimal(900).setScale(2, RoundingMode.HALF_DOWN)
        val expectedCisDeductions = BigDecimal(540).setScale(2, RoundingMode.HALF_DOWN)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(3, expectedPayments, expectedMaterialsCost, expectedCisDeductions)(
          request,
          messages(application)
        ).toString
      }
    }

    "must exclude subcontractors with incomplete details from totals" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val subcontractors = Map(
        1 -> SelectedSubcontractor(101, "Alpha Ltd", Some(BigDecimal(1000)), Some(BigDecimal(200)), Some(BigDecimal(160))),
        2 -> SelectedSubcontractor(102, "Beta Ltd",  None,                  None,                  None)
      )

      val userAnswers = buildUserAnswers(subcontractors)

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, summaryRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[SummarySubcontractorPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          1,
          BigDecimal(1000).setScale(2, RoundingMode.HALF_DOWN),
          BigDecimal(200).setScale(2, RoundingMode.HALF_DOWN),
          BigDecimal(160).setScale(2, RoundingMode.HALF_DOWN)
        )(request, messages(application)).toString
      }
    }

    "must return OK with zero totals when no subcontractors have complete details" in {

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SessionRepository].toInstance(mockSessionRepository))
        .build()

      running(application) {
        val request = FakeRequest(GET, summaryRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[SummarySubcontractorPaymentsView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          0,
          BigDecimal(0).setScale(2, RoundingMode.HALF_DOWN),
          BigDecimal(0).setScale(2, RoundingMode.HALF_DOWN),
          BigDecimal(0).setScale(2, RoundingMode.HALF_DOWN)
        )(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, summaryRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
