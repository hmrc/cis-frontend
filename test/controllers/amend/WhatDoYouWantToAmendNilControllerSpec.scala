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

package controllers.amend

import base.SpecBase
import controllers.routes
import forms.amend.WhatDoYouWantToAmendNilFormProvider
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyStandardReturn}
import models.{JourneyScope, NormalMode, UserAnswers}
import org.mockito.Mockito.{atLeastOnce, never, times, verify, verifyNoInteractions, when}
import services.{AmendMonthlyReturnService, CisTaxpayerService, MonthlyReturnService}
import models.amend.WhatDoYouWantToAmendNil
import models.monthlyreturns.UpdateMonthlyReturnRequest
import models.amend.WhatDoYouWantToAmendNil.AmendNilReturn
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhatDoYouWantToAmendNilPage
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.amend.WhatDoYouWantToAmendNilView

import java.time.LocalDate
import scala.concurrent.Future

import config.FrontendAppConfig
import models.monthlyreturns.CisTaxpayer
import models.requests.CisPath.CisOrg
import org.mockito.ArgumentCaptor
import play.api.Application

import java.util.UUID
import scala.jdk.CollectionConverters.*

class WhatDoYouWantToAmendNilControllerSpec extends SpecBase with MockitoSugar {

  private lazy val whatDoYouWantToAmendNilRoute =
    controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoad().url

  private val formProvider = new WhatDoYouWantToAmendNilFormProvider()
  private val form         = formProvider()

  private val completeUserAnswers =
    emptyUserAnswers
      .set(CisIdPage, "cis-123")
      .success
      .value
      .set(DateConfirmPaymentsPage, LocalDate.of(2025, 1, 1))
      .success
      .value
      .set(ReturnTypePage, MonthlyStandardReturn)
      .success
      .value

  private def scopedId(flowId: UUID): String = JourneyScope.subJourneyId(CisOrg, flowId)

  private def withSubJourney(base: UserAnswers, flowId: UUID): UserAnswers =
    base.initialiseSubJourney(scopedId(flowId)).success.value

  private def scopedGetUrl(flowId: UUID): String =
    controllers.amend.routes.WhatDoYouWantToAmendNilController.onPageLoadWithId(CisOrg, flowId).url

  private def scopedPostUrl(flowId: UUID): String =
    controllers.amend.routes.WhatDoYouWantToAmendNilController.onSubmitWithId(CisOrg, flowId).url

  private def scopedApplication(
    userAnswers: Option[UserAnswers],
    amendService: AmendMonthlyReturnService = mock[AmendMonthlyReturnService],
    monthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService],
    enabled: Boolean = true
  ): (Application, SessionRepository) = {
    val repository = mockSessionRepository(userAnswers)
    when(repository.set(any())).thenReturn(Future.successful(true))

    val taxpayer = mock[CisTaxpayer]
    when(taxpayer.uniqueId).thenReturn("1")

    val taxpayerService = mock[CisTaxpayerService]
    when(taxpayerService.findForOrganisation(using any())).thenReturn(Future.successful(taxpayer))

    val application = applicationBuilder()
      .configure("features.multi-tab-enabled" -> enabled)
      .overrides(
        bind[SessionRepository].toInstance(repository),
        bind[CisTaxpayerService].toInstance(taxpayerService),
        bind[AmendMonthlyReturnService].toInstance(amendService),
        bind[MonthlyReturnService].toInstance(monthlyReturnService)
      )
      .build()

    (application, repository)
  }

  "WhatDoYouWantToAmendNil Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form)(request, messages(application)).toString
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val userAnswers =
        completeUserAnswers
          .set(WhatDoYouWantToAmendNilPage, AmendNilReturn)
          .success
          .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form.fill(AmendNilReturn))(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the SubmitInactivityRequestController page when valid data AmendNilReturn is submitted" in {

      val mockSessionRepository    = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockMonthlyReturnService.updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AmendNilReturn.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.SubmitInactivityRequestController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must redirect to the WhichSubcontractorsToAddController page when valid data AddPaymentOrSubcontractorDetails is submitted" in {

      val mockSessionRepository         = mock[SessionRepository]
      val mockMonthlyReturnService      = mock[MonthlyReturnService]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(
        mockAmendMonthlyReturnService.startStandardAmendment(any[UserAnswers]())(any())
      ) thenReturn Future.successful(Right(()))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.amend.routes.WhichSubcontractorsToAddController
          .onPageLoad(NormalMode)
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[WhatDoYouWantToAmendNilView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whatDoYouWantToAmendNilRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", AmendNilReturn.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if service failed" in {

      val mockSessionRepository         = mock[SessionRepository]
      val mockAmendMonthlyReturnService = mock[AmendMonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(
        mockAmendMonthlyReturnService.startStandardAmendment(any[UserAnswers]())(any())
      ) thenReturn Future.successful(Left("Missing amendment details"))

      val application =
        applicationBuilder(userAnswers = Some(completeUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(("value", WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must fail when UpdateMonthlyReturnRequest cannot be built" in {

      val mockSessionRepository    = mock[SessionRepository]
      val mockMonthlyReturnService = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val incompleteUserAnswers = emptyUserAnswers
        .set(CisIdPage, "cis-123")
        .success
        .value
        .set(ReturnTypePage, MonthlyStandardReturn)
        .success
        .value

      val application =
        applicationBuilder(userAnswers = Some(incompleteUserAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whatDoYouWantToAmendNilRoute)
            .withFormUrlEncodedBody(
              "value" -> WhatDoYouWantToAmendNil.AmendNilReturn.toString
            )

        val result = route(application, request).value

        whenReady(result.failed) { exception =>
          exception mustBe a[RuntimeException]
        }

        verify(mockMonthlyReturnService, never())
          .updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any())
      }
    }
  }

  "UUID-scoped WhatDoYouWantToAmendNil Controller" - {

    "onPageLoadWithId" - {

      "must return OK and the correct view for a valid flowId" in {
        val flowId           = UUID.randomUUID()
        val (application, _) = scopedApplication(Some(withSubJourney(completeUserAnswers, flowId)))

        running(application) {
          val request = FakeRequest(GET, scopedGetUrl(flowId))
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[WhatDoYouWantToAmendNilView]
          val action  = Some(
            controllers.amend.routes.WhatDoYouWantToAmendNilController.onSubmitWithId(CisOrg, flowId)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, action)(request, messages(application)).toString
        }
      }

      "must populate the view when the question has previously been answered" in {
        val flowId  = UUID.randomUUID()
        val answers = withSubJourney(completeUserAnswers, flowId)
          .set(WhatDoYouWantToAmendNilPage, AmendNilReturn)
          .success
          .value

        val (application, _) = scopedApplication(Some(answers))

        running(application) {
          val request = FakeRequest(GET, scopedGetUrl(flowId))
          val result  = route(application, request).value
          val view    = application.injector.instanceOf[WhatDoYouWantToAmendNilView]
          val action  = Some(
            controllers.amend.routes.WhatDoYouWantToAmendNilController.onSubmitWithId(CisOrg, flowId)
          )

          status(result) mustEqual OK
          contentAsString(result) mustEqual
            view(form.fill(AmendNilReturn), action)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery when the flowId does not exist" in {
        val (application, _) = scopedApplication(Some(completeUserAnswers))

        running(application) {
          val result = route(application, FakeRequest(GET, scopedGetUrl(UUID.randomUUID()))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to Journey Recovery when no session data exists" in {
        val (application, _) = scopedApplication(None)

        running(application) {
          val result = route(application, FakeRequest(GET, scopedGetUrl(UUID.randomUUID()))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to the landing page when multi-tab is disabled" in {
        val flowId           = UUID.randomUUID()
        val (application, _) =
          scopedApplication(Some(withSubJourney(completeUserAnswers, flowId)), enabled = false)

        running(application) {
          val result = route(application, FakeRequest(GET, scopedGetUrl(flowId))).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            application.injector.instanceOf[FrontendAppConfig].returnsLandingPageUrl("1", None)
        }
      }
    }

    "onSubmitWithId" - {

      def post(flowId: UUID, value: String) =
        FakeRequest(POST, scopedPostUrl(flowId)).withFormUrlEncodedBody("value" -> value)

      "must return Bad Request and not call any service when invalid data is submitted" in {
        val flowId                    = UUID.randomUUID()
        val amendService              = mock[AmendMonthlyReturnService]
        val monthlyReturnService      = mock[MonthlyReturnService]
        val (application, repository) = scopedApplication(
          Some(withSubJourney(completeUserAnswers, flowId)),
          amendService,
          monthlyReturnService
        )

        running(application) {
          val result = route(application, post(flowId, "invalid value")).value

          status(result) mustEqual BAD_REQUEST
          verifyNoInteractions(amendService, monthlyReturnService)
          verify(repository, never()).set(any())
        }
      }

      "must start a standard amendment, set the return type and redirect when AddPaymentOrSubcontractorDetails is submitted" in {
        val flowId       = UUID.randomUUID()
        val amendService = mock[AmendMonthlyReturnService]

        when(amendService.startStandardAmendment(any[UserAnswers]())(any()))
          .thenReturn(Future.successful(Right(())))

        val (application, repository) =
          scopedApplication(Some(withSubJourney(completeUserAnswers, flowId)), amendService)

        running(application) {
          val result =
            route(application, post(flowId, WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails.toString)).value

          status(result) mustEqual SEE_OTHER
          // TODO: update when WhichSubcontractorsToAddController.onPageLoadWithId replaces the temp redirect
          redirectLocation(result).value mustEqual scopedGetUrl(flowId)

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository, times(2)).set(captor.capture())
          val saved  = captor.getAllValues.asScala.toList

          saved.head.get(WhatDoYouWantToAmendNilPage) mustBe Some(
            WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails
          )
          saved.last.get(ReturnTypePage) mustBe Some(MonthlyAmendedStandardReturn)
          saved.last.hasSubJourney(scopedId(flowId)) mustBe true

          verify(amendService).startStandardAmendment(any[UserAnswers]())(any())
        }
      }

      "must redirect to Journey Recovery when startStandardAmendment returns Left" in {
        val flowId       = UUID.randomUUID()
        val amendService = mock[AmendMonthlyReturnService]

        when(amendService.startStandardAmendment(any[UserAnswers]())(any()))
          .thenReturn(Future.successful(Left("Missing amendment details")))

        val (application, _) =
          scopedApplication(Some(withSubJourney(completeUserAnswers, flowId)), amendService)

        running(application) {
          val result =
            route(application, post(flowId, WhatDoYouWantToAmendNil.AddPaymentOrSubcontractorDetails.toString)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must set the nil return type, update the monthly return and redirect when AmendNilReturn is submitted" in {
        val flowId               = UUID.randomUUID()
        val monthlyReturnService = mock[MonthlyReturnService]

        when(monthlyReturnService.updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any()))
          .thenReturn(Future.successful(()))

        val (application, repository) = scopedApplication(
          Some(withSubJourney(completeUserAnswers, flowId)),
          monthlyReturnService = monthlyReturnService
        )

        running(application) {
          val result = route(application, post(flowId, AmendNilReturn.toString)).value

          status(result) mustEqual SEE_OTHER
          // TODO: update when SubmitInactivityRequestController.onPageLoadWithId replaces the temp redirect
          redirectLocation(result).value mustEqual scopedGetUrl(flowId)

          val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
          verify(repository, atLeastOnce()).set(captor.capture())
          val saved  = captor.getAllValues.asScala.toList.last

          saved.get(ReturnTypePage) mustBe Some(MonthlyAmendedNilReturn)
          saved.hasSubJourney(scopedId(flowId)) mustBe true

          verify(monthlyReturnService).updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any())
        }
      }

      "must fail when UpdateMonthlyReturnRequest cannot be built" in {
        val flowId               = UUID.randomUUID()
        val monthlyReturnService = mock[MonthlyReturnService]

        val incompleteUserAnswers = emptyUserAnswers
          .set(CisIdPage, "cis-123")
          .success
          .value
          .set(ReturnTypePage, MonthlyStandardReturn)
          .success
          .value

        val (application, _) = scopedApplication(
          Some(withSubJourney(incompleteUserAnswers, flowId)),
          monthlyReturnService = monthlyReturnService
        )

        running(application) {
          val result = route(application, post(flowId, AmendNilReturn.toString)).value

          whenReady(result.failed) { exception =>
            exception mustBe a[RuntimeException]
          }

          verify(monthlyReturnService, never()).updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any())
        }
      }

      "must redirect to Journey Recovery and not save when the flowId does not exist" in {
        val monthlyReturnService      = mock[MonthlyReturnService]
        val (application, repository) =
          scopedApplication(Some(completeUserAnswers), monthlyReturnService = monthlyReturnService)

        running(application) {
          val result = route(application, post(UUID.randomUUID(), AmendNilReturn.toString)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
          verify(repository, never()).set(any())
          verifyNoInteractions(monthlyReturnService)
        }
      }

      "must redirect to the landing page and not save when multi-tab is disabled" in {
        val flowId                    = UUID.randomUUID()
        val (application, repository) =
          scopedApplication(Some(withSubJourney(completeUserAnswers, flowId)), enabled = false)

        running(application) {
          val result = route(application, post(flowId, AmendNilReturn.toString)).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual
            application.injector.instanceOf[FrontendAppConfig].returnsLandingPageUrl("1", None)
          verify(repository, never()).set(any())
        }
      }
    }
  }
}
