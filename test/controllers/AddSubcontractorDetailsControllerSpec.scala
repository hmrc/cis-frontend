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

package controllers

import base.SpecBase
import controllers.monthlyreturns.{AddSubcontractorDetailsController, SubcontractorDetailsAddedController}
import forms.monthlyreturns.AddSubcontractorDetailsFormProvider
import models.monthlyreturns.SelectedSubcontractor
import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{AddSubcontractorDetailsPage, SelectedSubcontractorPage}
import play.api.i18n.{DefaultMessagesApi, Lang, Messages, MessagesImpl}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import views.html.monthlyreturns.AddSubcontractorDetailsView
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import scala.concurrent.Future

class AddSubcontractorDetailsControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val addSubcontractorDetailsRoute =
    controllers.monthlyreturns.routes.AddSubcontractorDetailsController.onPageLoad(NormalMode).url

  val formProvider = new AddSubcontractorDetailsFormProvider()
  val form         = formProvider()

  private val completeSubcontractor: SelectedSubcontractor =
    SelectedSubcontractor(
      id = 1L,
      name = "BuildRight Construction",
      totalPaymentsMade = Some(BigDecimal(1000)),
      costOfMaterials = Some(BigDecimal(200)),
      totalTaxDeducted = Some(BigDecimal(300))
    )

  private val incompleteSubcontractor1: SelectedSubcontractor =
    SelectedSubcontractor(
      id = 2L,
      name = "Northern Trades Ltd",
      totalPaymentsMade = None,
      costOfMaterials = None,
      totalTaxDeducted = None
    )

  private val incompleteSubcontractor2: SelectedSubcontractor =
    SelectedSubcontractor(
      id = 3L,
      name = "TyneWear Ltd",
      totalPaymentsMade = None,
      costOfMaterials = None,
      totalTaxDeducted = None
    )

  private val subcontractorsWithDetails: Seq[SelectedSubcontractor] =
    Seq(completeSubcontractor)

  private val subcontractorsWithoutDetails: Map[Int, SelectedSubcontractor] =
    Map(2 -> incompleteSubcontractor1, 3 -> incompleteSubcontractor2)

  private val userAnswersWithSubcontractors: UserAnswers =
    emptyUserAnswers
      .setOrException(SelectedSubcontractorPage(1), completeSubcontractor)
      .setOrException(SelectedSubcontractorPage(2), incompleteSubcontractor1)
      .setOrException(SelectedSubcontractorPage(3), incompleteSubcontractor2)

  "AddSubcontractorDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSubcontractors)).build()

      running(application) {
        val request = FakeRequest(GET, addSubcontractorDetailsRoute)

        val result = route(application, request).value

        val view = application.injector.instanceOf[AddSubcontractorDetailsView]

        status(result) mustEqual OK
        val content = contentAsString(result)

        content mustEqual view(form, NormalMode, subcontractorsWithDetails, subcontractorsWithoutDetails)(
          request,
          messages(application)
        ).toString

        content must include("BuildRight Construction")
        content must include("Northern Trades Ltd")
        content must include("TyneWear Ltd")
      }
    }

    "must return OK and render the view when subcontractors exist in user answers" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSubcontractors)).build()

      running(application) {
        val request = FakeRequest(GET, addSubcontractorDetailsRoute)

        val view = application.injector.instanceOf[AddSubcontractorDetailsView]

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(
          form,
          NormalMode,
          subcontractorsWithDetails,
          subcontractorsWithoutDetails
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to the next page when valid data is submitted" in {

      val selectedIndex = subcontractorsWithoutDetails.head._1

      val application =
        applicationBuilder(userAnswers = Some(userAnswersWithSubcontractors))
          .build()

      running(application) {
        val request =
          FakeRequest(POST, addSubcontractorDetailsRoute)
            .withFormUrlEncodedBody(("value", selectedIndex.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual controllers.monthlyreturns.routes.PaymentDetailsController
          .onPageLoad(NormalMode, selectedIndex)
          .url
      }
    }

    "must return a Bad Request and errors when invalid data is submitted" in {

      val application = applicationBuilder(userAnswers = Some(userAnswersWithSubcontractors)).build()

      running(application) {
        val request =
          FakeRequest(POST, addSubcontractorDetailsRoute)
            .withFormUrlEncodedBody(("value", "invalid value"))

        val boundForm = form.bind(Map("value" -> "invalid value"))

        val view = application.injector.instanceOf[AddSubcontractorDetailsView]

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
        contentAsString(result) mustEqual view(
          boundForm,
          NormalMode,
          subcontractorsWithDetails,
          subcontractorsWithoutDetails
        )(
          request,
          messages(application)
        ).toString
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, addSubcontractorDetailsRoute)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "redirect to Journey Recovery for a POST if no existing data is found" in {

      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request =
          FakeRequest(POST, addSubcontractorDetailsRoute)
            .withFormUrlEncodedBody(("value", subcontractorsWithoutDetails.head._1.toString))

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }

  "AddSubcontractorDetailsController.radioItems" - {

    implicit val messages: Messages =
      MessagesImpl(Lang("en"), new DefaultMessagesApi())

    "return an empty sequence when given no subcontractors" in {
      val items = AddSubcontractorDetailsController.radioItems(Map.empty)
      items mustBe empty
    }

    "create radio items with correct id, value and label for each subcontractor" in {
      val subcontractors = Map(
        1 -> SelectedSubcontractor(1L, "First Subcontractor", None, None, None),
        2 -> SelectedSubcontractor(2L, "Second Subcontractor", None, None, None)
      )

      val items = AddSubcontractorDetailsController.radioItems(subcontractors)

      items.size mustBe 2

      items.head.id mustBe Some("subcontractor-1")
      items.head.value mustBe Some("1")
      items.head.content mustBe Text("First Subcontractor")

      items(1).id mustBe Some("subcontractor-2")
      items(1).value mustBe Some("2")
      items(1).content mustBe Text("Second Subcontractor")
    }
  }
}
