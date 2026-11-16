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
import models.ReturnType.{MonthlyAmendedNilReturn, MonthlyAmendedStandardReturn, MonthlyStandardReturn}
import org.mockito.Mockito.{never, verify, verifyNoInteractions, when}
import forms.amend.AreYouSureYouWantToAmendYesNoFormProvider
import models.amend.AreYouSureYouWantToAmendYesNo.*
import models.amend.{AreYouSureYouWantToAmendYesNo, DeleteAllMonthlyReturnItemsRequest}
import models.monthlyreturns.UpdateMonthlyReturnRequest
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.AreYouSureYouWantToAmendYesNoPage
import pages.monthlyreturns.*
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{AmendMonthlyReturnService, MonthlyReturnService}
import views.html.amend.AreYouSureYouWantToAmendYesNoView
import config.FrontendAppConfig
import models.{ReturnType, UserAnswers}
import play.api.Application

import java.util.UUID
import java.time.LocalDate
import scala.concurrent.Future

class AreYouSureYouWantToAmendYesNoControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val areYouSureYouWantToAmendYesNoRoute =
    routes.AreYouSureYouWantToAmendYesNoController.onPageLoad().url

  val formProvider = new AreYouSureYouWantToAmendYesNoFormProvider()
  val form         = formProvider()

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

  // --- Multi-tab helpers ---

  private def scopedId(flowId: UUID): String = flowId.toString

  // Satisfies MultiTabAction.forJourney and both scoped request builders (cisId, date, return type)
  private def scopedAnswers(flowId: UUID, returnType: ReturnType = MonthlyStandardReturn): UserAnswers = {
    val id = scopedId(flowId)

    emptyUserAnswers
      .set(CisIdPage(id), "1")
      .success
      .value
      .set(DateConfirmPaymentsPage(id), LocalDate.of(2025, 1, 1))
      .success
      .value
      .set(ReturnTypePage(id), returnType)
      .success
      .value
  }

  private def scopedGetUrl(flowId: UUID): String =
    routes.AreYouSureYouWantToAmendYesNoController.onPageLoadWithId(flowId).url

  private def scopedPostUrl(flowId: UUID): String =
    routes.AreYouSureYouWantToAmendYesNoController.onSubmitWithId(flowId).url

  private def scopedFormAction(flowId: UUID): Option[Call] =
    Some(routes.AreYouSureYouWantToAmendYesNoController.onSubmitWithId(flowId))

  private def scopedApplication(
    userAnswers: Option[UserAnswers],
    amendService: AmendMonthlyReturnService = mock[AmendMonthlyReturnService],
    monthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService],
    enabled: Boolean = true
  ): (Application, SessionRepository) = {
    val repository = mockSessionRepository(userAnswers)
    when(repository.set(any())).thenReturn(Future.successful(true))

    val application = applicationBuilder(userAnswers = userAnswers) // getData/requireData read from here
      .configure("features.multi-tab-enabled" -> enabled)
      .overrides(
        bind[SessionRepository].toInstance(repository),
        bind[AmendMonthlyReturnService].toInstance(amendService),
        bind[MonthlyReturnService].toInstance(monthlyReturnService)
      )
      .build()

    (application, repository)
  }

  private def stubServicesOk(
    amendService: AmendMonthlyReturnService,
    monthlyReturnService: MonthlyReturnService
  ): Unit = {
    when(amendService.deleteAllMonthlyReturnItems(any[DeleteAllMonthlyReturnItemsRequest]())(any()))
      .thenReturn(Future.successful(()))
    when(monthlyReturnService.updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any()))
      .thenReturn(Future.successful(()))
  }

  "AreYouSureYouWantToAmendYesNo Controller" - {

    "unscoped - legacy" - {

      "must return OK and the correct view for a GET" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, areYouSureYouWantToAmendYesNoRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form)(request, messages(application)).toString
        }
      }

      "must populate the view correctly on a GET when the question has previously been answered" in {

        val userAnswers =
          completeUserAnswers
            .set(AreYouSureYouWantToAmendYesNoPage, AreYouSureYouWantToAmendYesNo.values.head)
            .success
            .value

        val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, areYouSureYouWantToAmendYesNoRoute)

          val view = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form.fill(AreYouSureYouWantToAmendYesNo.values.head))(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to the next page when No is submitted" in {

        val mockSessionRepository = mock[SessionRepository]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val application =
          applicationBuilder(userAnswers = Some(completeUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
              .withFormUrlEncodedBody(("value", No.toString))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url
        }
      }

      "must delete all monthly return items, update the monthly return with the nil return indicator set to Y and redirect when Yes is submitted" in {

        val mockSessionRepository     = mock[SessionRepository]
        val mockAmendMonthlyReturnSvc = mock[AmendMonthlyReturnService]
        val mockMonthlyReturnService  = mock[MonthlyReturnService]

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        when(mockAmendMonthlyReturnSvc.deleteAllMonthlyReturnItems(any[DeleteAllMonthlyReturnItemsRequest]())(any()))
          .thenReturn(Future.successful(()))

        when(mockMonthlyReturnService.updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any()))
          .thenReturn(Future.successful(()))

        val application =
          applicationBuilder(userAnswers = Some(completeUserAnswers))
            .overrides(
              bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
              bind[SessionRepository].toInstance(mockSessionRepository),
              bind[AmendMonthlyReturnService].toInstance(mockAmendMonthlyReturnSvc),
              bind[MonthlyReturnService].toInstance(mockMonthlyReturnService)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
              .withFormUrlEncodedBody(("value", Yes.toString))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual onwardRoute.url

          val updateCaptor = ArgumentCaptor.forClass(classOf[UpdateMonthlyReturnRequest])
          verify(mockMonthlyReturnService).updateMonthlyReturn(updateCaptor.capture())(any())
          updateCaptor.getValue.nilReturnIndicator mustEqual "Y"
        }
      }

      "must return a Bad Request and errors when invalid data is submitted" in {

        val application = applicationBuilder(userAnswers = Some(completeUserAnswers)).build()

        running(application) {
          val request =
            FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
              .withFormUrlEncodedBody(("value", "invalid value"))

          val boundForm = form.bind(Map("value" -> "invalid value"))

          val view = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

          val result = route(application, request).value

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm)(request, messages(application)).toString
        }
      }

      "must redirect to Journey Recovery for a GET if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, areYouSureYouWantToAmendYesNoRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "redirect to Journey Recovery for a POST if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request =
            FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
              .withFormUrlEncodedBody(("value", No.toString))

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must fail when DeleteAllMonthlyReturnItemsRequest cannot be built" in {

        val mockSessionRepository = mock[SessionRepository]

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
              bind[SessionRepository].toInstance(mockSessionRepository)
            )
            .build()

        running(application) {
          val request =
            FakeRequest(POST, areYouSureYouWantToAmendYesNoRoute)
              .withFormUrlEncodedBody("value" -> Yes.toString)

          val result = route(application, request).value

          whenReady(result.failed) { ex =>
            ex mustBe a[RuntimeException]
          }
        }
      }

    }

    "UUID-scoped journey" - {

      "onPageLoadWithId" - {

        "must return OK and the correct view for a valid flowId" in {
          val flowId           = UUID.randomUUID()
          val (application, _) = scopedApplication(Some(scopedAnswers(flowId)))

          running(application) {
            val request = FakeRequest(GET, scopedGetUrl(flowId))
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual
              view(form, scopedFormAction(flowId))(request, messages(application)).toString
          }
        }

        "must populate the view when the question has previously been answered in this journey" in {
          val flowId  = UUID.randomUUID()
          val answers = scopedAnswers(flowId)
            .set(AreYouSureYouWantToAmendYesNoPage(scopedId(flowId)), Yes)
            .success
            .value

          val (application, _) = scopedApplication(Some(answers))

          running(application) {
            val request = FakeRequest(GET, scopedGetUrl(flowId))
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

            status(result) mustEqual OK
            contentAsString(result) mustEqual
              view(form.fill(Yes), scopedFormAction(flowId))(request, messages(application)).toString
          }
        }

        "must not populate the view from the legacy unscoped answer" in {
          val flowId  = UUID.randomUUID()
          val answers = scopedAnswers(flowId)
            .set(AreYouSureYouWantToAmendYesNoPage, Yes)
            .success
            .value

          val (application, _) = scopedApplication(Some(answers))

          running(application) {
            val request = FakeRequest(GET, scopedGetUrl(flowId))
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

            contentAsString(result) mustEqual
              view(form, scopedFormAction(flowId))(request, messages(application)).toString
          }
        }

        "must redirect to Journey Recovery when the scoped CIS ID is missing, even if a legacy one exists" in {
          val (application, _) = scopedApplication(Some(completeUserAnswers))

          running(application) {
            val result = route(application, FakeRequest(GET, scopedGetUrl(UUID.randomUUID()))).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to Journey Recovery when no session data exists" in {
          val (application, _) = scopedApplication(None)

          running(application) {
            val result = route(application, FakeRequest(GET, scopedGetUrl(UUID.randomUUID()))).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to the landing page when multi-tab is disabled" in {
          val flowId           = UUID.randomUUID()
          val (application, _) = scopedApplication(Some(scopedAnswers(flowId)), enabled = false)

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

        "must return a Bad Request with the scoped form action and not call any service when invalid data is submitted" in {
          val flowId               = UUID.randomUUID()
          val amendService         = mock[AmendMonthlyReturnService]
          val monthlyReturnService = mock[MonthlyReturnService]

          val (application, repository) =
            scopedApplication(Some(scopedAnswers(flowId)), amendService, monthlyReturnService)

          running(application) {
            val request = post(flowId, "invalid value")
            val result  = route(application, request).value
            val view    = application.injector.instanceOf[AreYouSureYouWantToAmendYesNoView]

            status(result) mustEqual BAD_REQUEST
            contentAsString(result) mustEqual
              view(form.bind(Map("value" -> "invalid value")), scopedFormAction(flowId))(
                request,
                messages(application)
              ).toString

            verifyNoInteractions(amendService, monthlyReturnService)
            verify(repository, never()).set(any())
          }
        }

        "must save the scoped answers and redirect when No is submitted" in {
          val flowId = UUID.randomUUID()
          val id     = scopedId(flowId)

          val (application, repository) = scopedApplication(Some(scopedAnswers(flowId)))

          running(application) {
            val result = route(application, post(flowId, No.toString)).value

            status(result) mustEqual SEE_OTHER
            // TODO: update when the next page has an ID-scoped route
            redirectLocation(result).value mustEqual scopedGetUrl(flowId)

            val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(repository).set(captor.capture())
            val saved  = captor.getValue

            saved.get(AreYouSureYouWantToAmendYesNoPage(id)) mustBe Some(No)
            saved.get(ReturnTypePage(id)) mustBe Some(MonthlyAmendedStandardReturn)

            // the legacy keys are untouched
            saved.get(AreYouSureYouWantToAmendYesNoPage) mustBe None
            saved.get(ReturnTypePage) mustBe None
          }
        }

        "must delete the items, update the return and redirect when Yes is submitted" in {
          val flowId               = UUID.randomUUID()
          val id                   = scopedId(flowId)
          val amendService         = mock[AmendMonthlyReturnService]
          val monthlyReturnService = mock[MonthlyReturnService]
          stubServicesOk(amendService, monthlyReturnService)

          val (application, repository) =
            scopedApplication(Some(scopedAnswers(flowId)), amendService, monthlyReturnService)

          running(application) {
            val result = route(application, post(flowId, Yes.toString)).value

            status(result) mustEqual SEE_OTHER
            // TODO: update when the next page has an ID-scoped route
            redirectLocation(result).value mustEqual scopedGetUrl(flowId)

            val savedCaptor = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(repository).set(savedCaptor.capture())
            val saved       = savedCaptor.getValue

            saved.get(ReturnTypePage(id)) mustBe Some(MonthlyAmendedNilReturn)
            saved.get(AreYouSureYouWantToAmendYesNoPage(id)) mustBe Some(Yes)

            val deleteCaptor = ArgumentCaptor.forClass(classOf[DeleteAllMonthlyReturnItemsRequest])
            verify(amendService).deleteAllMonthlyReturnItems(deleteCaptor.capture())(any())
            deleteCaptor.getValue mustBe
              DeleteAllMonthlyReturnItemsRequest("1", 2025, 1, MonthlyAmendedNilReturn.amendmentFlag)

            val updateCaptor = ArgumentCaptor.forClass(classOf[UpdateMonthlyReturnRequest])
            verify(monthlyReturnService).updateMonthlyReturn(updateCaptor.capture())(any())

            val sent = updateCaptor.getValue
            sent.instanceId mustBe "1"
            sent.taxYear mustBe 2025
            sent.taxMonth mustBe 1
            sent.amendment mustBe MonthlyAmendedNilReturn.amendmentFlag
            sent.nilReturnIndicator mustBe "Y"
          }
        }

        "must fail without calling any service when the scoped request cannot be built, even if legacy answers exist" in {
          val flowId               = UUID.randomUUID()
          val id                   = scopedId(flowId)
          val amendService         = mock[AmendMonthlyReturnService]
          val monthlyReturnService = mock[MonthlyReturnService]

          // scoped cis id and return type, but no scoped date; completeUserAnswers supplies a legacy date
          val answers = completeUserAnswers
            .set(CisIdPage(id), "1")
            .success
            .value
            .set(ReturnTypePage(id), MonthlyStandardReturn)
            .success
            .value

          val (application, _) = scopedApplication(Some(answers), amendService, monthlyReturnService)

          running(application) {
            val result = route(application, post(flowId, Yes.toString)).value

            whenReady(result.failed) { ex =>
              ex mustBe a[RuntimeException]
            }

            verifyNoInteractions(amendService, monthlyReturnService)
          }
        }

        "must not update the monthly return when deleting the items fails" in {
          val flowId               = UUID.randomUUID()
          val amendService         = mock[AmendMonthlyReturnService]
          val monthlyReturnService = mock[MonthlyReturnService]

          when(amendService.deleteAllMonthlyReturnItems(any[DeleteAllMonthlyReturnItemsRequest]())(any()))
            .thenReturn(Future.failed(new RuntimeException("Backend error")))

          val (application, _) =
            scopedApplication(Some(scopedAnswers(flowId)), amendService, monthlyReturnService)

          running(application) {
            val result = route(application, post(flowId, Yes.toString)).value

            whenReady(result.failed) { ex =>
              ex.getMessage mustBe "Backend error"
            }

            verify(monthlyReturnService, never()).updateMonthlyReturn(any[UpdateMonthlyReturnRequest]())(any())
          }
        }

        "must not change answers belonging to another journey" in {
          val flowId      = UUID.randomUUID()
          val otherFlowId = UUID.randomUUID()
          val otherId     = scopedId(otherFlowId)

          val answers = scopedAnswers(flowId)
            .set(CisIdPage(otherId), "OTHER-CIS")
            .success
            .value
            .set(AreYouSureYouWantToAmendYesNoPage(otherId), Yes)
            .success
            .value

          val (application, repository) = scopedApplication(Some(answers))

          running(application) {
            val result = route(application, post(flowId, No.toString)).value

            status(result) mustEqual SEE_OTHER

            val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(repository).set(captor.capture())
            val saved  = captor.getValue

            saved.get(CisIdPage(otherId)) mustBe Some("OTHER-CIS")
            saved.get(AreYouSureYouWantToAmendYesNoPage(otherId)) mustBe Some(Yes)
            saved.get(AreYouSureYouWantToAmendYesNoPage(scopedId(flowId))) mustBe Some(No)
          }
        }

        "must keep another journey's answers when Yes clears the amended standard return journey" in {
          val flowId      = UUID.randomUUID()
          val otherFlowId = UUID.randomUUID()
          val otherId     = scopedId(otherFlowId)

          val amendService         = mock[AmendMonthlyReturnService]
          val monthlyReturnService = mock[MonthlyReturnService]
          stubServicesOk(amendService, monthlyReturnService)

          val answers = scopedAnswers(flowId)
            .set(CisIdPage(otherId), "OTHER-CIS")
            .success
            .value
            .set(ReturnTypePage(otherId), MonthlyStandardReturn)
            .success
            .value

          val (application, repository) = scopedApplication(Some(answers), amendService, monthlyReturnService)

          running(application) {
            val result = route(application, post(flowId, Yes.toString)).value

            status(result) mustEqual SEE_OTHER

            val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
            verify(repository).set(captor.capture())
            val saved  = captor.getValue

            saved.get(CisIdPage(otherId)) mustBe Some("OTHER-CIS")
            saved.get(ReturnTypePage(otherId)) mustBe Some(MonthlyStandardReturn)
          }
        }

        "must redirect to Journey Recovery and not save when the flowId has no scoped CIS ID" in {
          val monthlyReturnService      = mock[MonthlyReturnService]
          val (application, repository) =
            scopedApplication(Some(completeUserAnswers), monthlyReturnService = monthlyReturnService)

          running(application) {
            val result = route(application, post(UUID.randomUUID(), Yes.toString)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
            verify(repository, never()).set(any())
            verifyNoInteractions(monthlyReturnService)
          }
        }

        "must redirect to Journey Recovery when no session data exists" in {
          val (application, _) = scopedApplication(None)

          running(application) {
            val result = route(application, post(UUID.randomUUID(), No.toString)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
          }
        }

        "must redirect to the landing page and not save when multi-tab is disabled" in {
          val flowId                    = UUID.randomUUID()
          val (application, repository) = scopedApplication(Some(scopedAnswers(flowId)), enabled = false)

          running(application) {
            val result = route(application, post(flowId, Yes.toString)).value

            status(result) mustEqual SEE_OTHER
            redirectLocation(result).value mustEqual
              application.injector.instanceOf[FrontendAppConfig].returnsLandingPageUrl("1", None)
            verify(repository, never()).set(any())
          }
        }
      }
    }

  }
}
