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
import forms.amend.WhichSubcontractorsToAddFormProvider
import models.amend.{Subcontractor, WhichSubcontractorsToAdd, WhichSubcontractorsToAddPageModel}
import models.{CheckMode, NormalMode, UserAnswers}
import models.finalvalidation.MonthlyFinalValidationSource
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhichSubcontractorsToAddPage
import pages.finalvalidations.*
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage, OriginalSubcontractorCountPage, ReturnTypePage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.{MonthlyReturnService, SubcontractorService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.amend.WhichSubcontractorsToAddView

import java.time.LocalDate
import scala.concurrent.Future

class WhichSubcontractorsToAddControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val whichSubcontractorsToAddRoute: String = routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode).url

  private val cisId   = "CIS-123"
  private val taxDate = LocalDate.of(2025, 10, 5)

  private val subcontractors = Seq(
    Subcontractor("1", "Alice, A"),
    Subcontractor("2", "Bob, B"),
    Subcontractor("3", "Charlie, C")
  )

  private val preSelectedIds = Set("1", "3")

  private def fullSubcontractor(id: Long): models.monthlyreturns.Subcontractor = {
    val subcontractor = mock[models.monthlyreturns.Subcontractor]
    when(subcontractor.subcontractorId).thenReturn(id)
    subcontractor
  }

  private val fullSubcontractors = Seq(
    fullSubcontractor(1L),
    fullSubcontractor(2L),
    fullSubcontractor(3L)
  )

  private val pageModel = WhichSubcontractorsToAddPageModel(
    subcontractors = subcontractors,
    preSelectedIds = preSelectedIds,
    status = Some("STARTED"),
    fullSubcontractors = fullSubcontractors
  )

  private val monthYear = LocalDate.of(2025, 10, 5)

  private val userAnswersWithRequiredPages =
    emptyUserAnswers
      .set(CisIdPage, cisId)
      .success
      .value
      .set(DateConfirmPaymentsPage, monthYear)
      .success
      .value
      .set(ReturnTypePage, models.ReturnType.MonthlyAmendedStandardReturn)
      .success
      .value

  val formProvider            = new WhichSubcontractorsToAddFormProvider()
  val form: Form[Set[String]] = formProvider(subcontractors)

  private def stubService(service: SubcontractorService, model: WhichSubcontractorsToAddPageModel): Unit =
    when(
      service.buildAmendWhichSubcontractorsPage(
        eqTo(cisId),
        eqTo(taxDate.getMonthValue),
        eqTo(taxDate.getYear),
        any[Option[UserAnswers]],
        any[Boolean]
      )(any[HeaderCarrier])
    ).thenReturn(Future.successful(model))

  "WhichSubcontractorsToAdd Controller" - {

    "must return OK and the correct view for a GET with pre-selected subcontractors" in {

      val subcontractorService = mock[SubcontractorService]
      val monthlyReturnService = mock[MonthlyReturnService]
      stubService(subcontractorService, pageModel)
      when(monthlyReturnService.isEditable(any[String], any[Int], any[Int], any[Boolean])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
        .overrides(
          bind[SubcontractorService].toInstance(subcontractorService),
          bind[MonthlyReturnService].toInstance(monthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val result  = route(application, request).value
        val view    = application.injector.instanceOf[WhichSubcontractorsToAddView]

        val expectedItems = WhichSubcontractorsToAdd.checkboxItems(subcontractors, preSelectedIds)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, expectedItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must use the standard return amendment flag on GET when ReturnTypePage is MonthlyStandardReturn" in {

      val userAnswers = userAnswersWithRequiredPages
        .setOrException(ReturnTypePage, models.ReturnType.MonthlyStandardReturn)

      val subcontractorService = mock[SubcontractorService]
      val monthlyReturnService = mock[MonthlyReturnService]
      stubService(subcontractorService, pageModel)
      when(monthlyReturnService.isEditable(any[String], any[Int], any[Int], any[Boolean])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SubcontractorService].toInstance(subcontractorService),
          bind[MonthlyReturnService].toInstance(monthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val result  = route(application, request).value

        status(result) mustEqual OK

        val isEditableAmendment = ArgumentCaptor.forClass(classOf[Boolean])
        verify(monthlyReturnService).isEditable(
          any[String],
          any[Int],
          any[Int],
          isEditableAmendment.capture()
        )(any[HeaderCarrier])
        isEditableAmendment.getValue mustBe false

        val serviceAmendment = ArgumentCaptor.forClass(classOf[Boolean])
        verify(subcontractorService).buildAmendWhichSubcontractorsPage(
          any[String],
          any[Int],
          any[Int],
          any[Option[UserAnswers]],
          serviceAmendment.capture()
        )(any[HeaderCarrier])
        serviceAmendment.getValue mustBe false
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val previouslySelected = Set(subcontractors.head.id)

      val userAnswers = userAnswersWithRequiredPages
        .set(WhichSubcontractorsToAddPage, previouslySelected)
        .success
        .value

      val subcontractorService = mock[SubcontractorService]
      val monthlyReturnService = mock[MonthlyReturnService]
      when(
        subcontractorService.buildAmendWhichSubcontractorsPage(
          eqTo(cisId),
          eqTo(taxDate.getMonthValue),
          eqTo(taxDate.getYear),
          any[Option[UserAnswers]],
          any[Boolean]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(pageModel))
      when(monthlyReturnService.isEditable(any[String], any[Int], any[Int], any[Boolean])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswers))
        .overrides(
          bind[SubcontractorService].toInstance(subcontractorService),
          bind[MonthlyReturnService].toInstance(monthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val view    = application.injector.instanceOf[WhichSubcontractorsToAddView]
        val result  = route(application, request).value

        val expectedItems = WhichSubcontractorsToAdd.checkboxItems(subcontractors, previouslySelected)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, expectedItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET when resource is not editable" in {

      val monthlyReturnService = mock[MonthlyReturnService]
      when(monthlyReturnService.isEditable(any[String], any[Int], any[Int], any[Boolean])(any[HeaderCarrier]))
        .thenReturn(Future.successful(false))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
        .overrides(bind[MonthlyReturnService].toInstance(monthlyReturnService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to the next page when valid data is submitted and monthly return status = STARTED" in {

      val mockSessionRepository = mock[SessionRepository]
      val subcontractorService  = mock[SubcontractorService]
      val monthlyReturnService  = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      stubService(subcontractorService, pageModel)
      when(
        monthlyReturnService.syncMonthlyReturnItems(
          any[UserAnswers],
          any[Seq[Long]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[SubcontractorService].toInstance(subcontractorService),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when valid data is submitted and monthly return status = VALIDATED" in {

      val mockSessionRepository = mock[SessionRepository]
      val subcontractorService  = mock[SubcontractorService]
      val monthlyReturnService  = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      stubService(subcontractorService, pageModel.copy(status = Some("VALIDATED")))
      when(
        monthlyReturnService.syncMonthlyReturnItems(
          any[UserAnswers],
          any[Seq[Long]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[SubcontractorService].toInstance(subcontractorService),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must redirect to the next page when multiple checkboxes are selected" in {

      val mockSessionRepository = mock[SessionRepository]
      val subcontractorService  = mock[SubcontractorService]
      val monthlyReturnService  = mock[MonthlyReturnService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      stubService(subcontractorService, pageModel)
      when(
        monthlyReturnService.syncMonthlyReturnItems(
          any[UserAnswers],
          any[Seq[Long]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[SubcontractorService].toInstance(subcontractorService),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[]", subcontractors.head.id), ("value[]", subcontractors(1).id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }

    "must set OriginalSubcontractorCountPage to total available subcontractors on valid submit" in {

      val mockSessionRepository = mock[SessionRepository]
      val subcontractorService  = mock[SubcontractorService]
      val monthlyReturnService  = mock[MonthlyReturnService]

      val answersWithPreviousFinalValidation = userAnswersWithRequiredPages
        .setOrException(FinalValidationDraftIdPage, "previous-draft")
        .setOrException(MonthlyFinalValidationContinuationPage, NormalMode.toString)
        .setOrException(
          MonthlyFinalValidationSourcePage,
          MonthlyFinalValidationSource.SelectSubcontractors
        )

      val captor = ArgumentCaptor.forClass(classOf[UserAnswers])

      when(mockSessionRepository.set(any[UserAnswers]))
        .thenReturn(Future.successful(true))

      stubService(subcontractorService, pageModel)

      when(
        monthlyReturnService.syncMonthlyReturnItems(
          any[UserAnswers],
          any[Seq[Long]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(answersWithPreviousFinalValidation))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[SubcontractorService].toInstance(subcontractorService),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockSessionRepository).set(captor.capture())

        captor.getValue.get(OriginalSubcontractorCountPage) mustBe
          Some(subcontractors.size)
        captor.getValue.get(FinalValidationDraftIdPage) mustBe None
        captor.getValue.get(MonthlyFinalValidationContinuationPage) mustBe None
        captor.getValue.get(MonthlyFinalValidationSourcePage) mustBe Some(
          MonthlyFinalValidationSource.WhichSubcontractorsToAdd(NormalMode.toString)
        )
      }
    }

    "must save CheckMode Final Validation source for a standard return on submit" in {

      val mockSessionRepository = mock[SessionRepository]
      val subcontractorService  = mock[SubcontractorService]
      val monthlyReturnService  = mock[MonthlyReturnService]

      val userAnswers = userAnswersWithRequiredPages
        .setOrException(ReturnTypePage, models.ReturnType.MonthlyStandardReturn)

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      stubService(subcontractorService, pageModel)
      when(
        monthlyReturnService.syncMonthlyReturnItems(
          any[UserAnswers],
          any[Seq[Long]]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[SubcontractorService].toInstance(subcontractorService),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, routes.WhichSubcontractorsToAddController.onSubmit(CheckMode).url)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockSessionRepository).set(captor.capture())
        captor.getValue.get(MonthlyFinalValidationSourcePage) mustBe Some(
          MonthlyFinalValidationSource.WhichSubcontractorsToAdd(CheckMode.toString)
        )

        val serviceAmendment = ArgumentCaptor.forClass(classOf[Boolean])
        verify(subcontractorService).buildAmendWhichSubcontractorsPage(
          any[String],
          any[Int],
          any[Int],
          any[Option[UserAnswers]],
          serviceAmendment.capture()
        )(any[HeaderCarrier])
        serviceAmendment.getValue mustBe false
      }
    }

    "must redirect to Journey Recovery when Submission status is not STARTED or VALIDATED" in {

      val mockSessionRepository = mock[SessionRepository]
      val subcontractorService  = mock[SubcontractorService]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)
      stubService(subcontractorService, pageModel.copy(status = Some("ACCEPTED")))

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[SubcontractorService].toInstance(subcontractorService)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val subcontractorService = mock[SubcontractorService]
      stubService(subcontractorService, pageModel)

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
        .overrides(bind[SubcontractorService].toInstance(subcontractorService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))
        val view      = application.injector.instanceOf[WhichSubcontractorsToAddView]
        val result    = route(application, request).value

        val emptyItems = WhichSubcontractorsToAdd.checkboxItems(subcontractors)

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(boundForm, NormalMode, emptyItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a GET when required answers are missing" in {

      val subcontractorService = mock[SubcontractorService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SubcontractorService].toInstance(subcontractorService))
        .build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery when ReturnTypePage is missing" in {

      val userAnswers = userAnswersWithRequiredPages
        .remove(ReturnTypePage)
        .get

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val getResult = route(application, FakeRequest(GET, whichSubcontractorsToAddRoute)).value
        status(getResult) mustEqual SEE_OTHER
        redirectLocation(getResult).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url

        val postResult = route(
          application,
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))
        ).value
        status(postResult) mustEqual SEE_OTHER
        redirectLocation(postResult).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to SystemError when the service call fails on GET" in {

      val subcontractorService = mock[SubcontractorService]
      val monthlyReturnService = mock[MonthlyReturnService]
      when(
        subcontractorService.buildAmendWhichSubcontractorsPage(
          eqTo(cisId),
          eqTo(taxDate.getMonthValue),
          eqTo(taxDate.getYear),
          any[Option[UserAnswers]],
          any[Boolean]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("boom")))
      when(monthlyReturnService.isEditable(any[String], any[Int], any[Int], any[Boolean])(any[HeaderCarrier]))
        .thenReturn(Future.successful(true))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
        .overrides(
          bind[SubcontractorService].toInstance(subcontractorService),
          bind[MonthlyReturnService].toInstance(monthlyReturnService)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)
        val result  = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to Journey Recovery for a POST when required answers are missing" in {

      val subcontractorService = mock[SubcontractorService]

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[SubcontractorService].toInstance(subcontractorService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to SystemError when the service call fails on POST" in {

      val subcontractorService = mock[SubcontractorService]
      when(
        subcontractorService.buildAmendWhichSubcontractorsPage(
          eqTo(cisId),
          eqTo(taxDate.getMonthValue),
          eqTo(taxDate.getYear),
          any[Option[UserAnswers]],
          any[Boolean]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("boom")))

      val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
        .overrides(bind[SubcontractorService].toInstance(subcontractorService))
        .build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value[0]", subcontractors.head.id))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
      }
    }
  }
}
