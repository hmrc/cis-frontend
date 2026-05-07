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
import models.{NormalMode, UserAnswers}
import models.amend.{Subcontractor, WhichSubcontractorsToAdd}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.WhichSubcontractorsToAddPage
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import views.html.amend.WhichSubcontractorsToAddView

import scala.concurrent.Future

class WhichSubcontractorsToAddControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val whichSubcontractorsToAddRoute: String = routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode).url

  val mockPreSelectedIds: Set[String]        = Set("2", "4", "6", "12", "15")
  val mockSubcontractors: Seq[Subcontractor] = Seq(
    Subcontractor("1", "Alice, A"),
    Subcontractor("2", "Apex Construction Solutions"),
    Subcontractor("3", "Bob, B"),
    Subcontractor("4", "Bloggs, Joe"),
    Subcontractor("5", "Bloggs, Joseph"),
    Subcontractor("6", "Build Right Construction"),
    Subcontractor("7", "Charles, C"),
    Subcontractor("8", "Dave, D"),
    Subcontractor("9", "Draft Services Ltd"),
    Subcontractor("10", "Elise, E"),
    Subcontractor("11", "Frank, F"),
    Subcontractor("12", "Northern Trades Ltd"),
    Subcontractor("13", "Pro-Build Subcontractors"),
    Subcontractor("14", "Tynewear Ltd"),
    Subcontractor("15", "SubbyCo Ltd")
  )
  private val subcontractors                 = mockSubcontractors
  private val preSelectedItems               = WhichSubcontractorsToAdd.checkboxItems(subcontractors, mockPreSelectedIds)
  private val emptyItems                     = WhichSubcontractorsToAdd.checkboxItems(subcontractors)
  val formProvider                           = new WhichSubcontractorsToAddFormProvider()
  val form: Form[Set[String]]                = formProvider(subcontractors)

  "WhichSubcontractorsToAdd Controller" - {

    "onPageLoad" - {
      "must return OK and the correct view" in {
        val monthlyReturnService = mock[MonthlyReturnService]

        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

          val result = route(application, request).value

          val view = application.injector.instanceOf[WhichSubcontractorsToAddView]

          status(result) mustEqual OK

          contentAsString(result) mustEqual view(form, NormalMode, preSelectedItems)(
            request,
            messages(application)
          ).toString
        }
      }
    }

    "must populate the view correctly on a GET when the question has previously been answered" in {

      val selectedIds = Set(subcontractors.head.id)

      val userAnswers = UserAnswers(userAnswersId)
        .set(WhichSubcontractorsToAddPage, selectedIds)
        .success
        .value

      val application = applicationBuilder(userAnswers = Some(userAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

        val view = application.injector.instanceOf[WhichSubcontractorsToAddView]

        val result = route(application, request).value

        val expectedItems = WhichSubcontractorsToAdd.checkboxItems(subcontractors, selectedIds)

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(form, NormalMode, expectedItems)(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
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

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository)
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

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {
        val request =
          FakeRequest(POST, whichSubcontractorsToAddRoute)
            .withFormUrlEncodedBody(("value", ""))

        val boundForm = form.bind(Map("value" -> ""))

        val view = application.injector.instanceOf[WhichSubcontractorsToAddView]

        val result = route(application, request).value

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

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
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
  }
}
