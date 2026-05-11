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
import models.NormalMode
import models.amend.{AmendmentDetails, Subcontractor as AmendSubcontractor, WhichSubcontractorsToAdd}
import models.monthlyreturns.{ContractorScheme, GetAllMonthlyReturnDetailsResponse, MonthlyReturn, MonthlyReturnItem, Subcontractor as MonthlyReturnSubcontractor, Submission}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import pages.amend.{AmendmentDetailsPage, WhichSubcontractorsToAddPage}
import play.api.data.Form
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.SessionRepository
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import views.html.amend.WhichSubcontractorsToAddView

import scala.concurrent.Future

class WhichSubcontractorsToAddControllerSpec extends SpecBase with MockitoSugar {

  def onwardRoute = Call("GET", "/foo")

  lazy val whichSubcontractorsToAddRoute: String = routes.WhichSubcontractorsToAddController.onPageLoad(NormalMode).url

  private val instanceId       = "1"
  private val taxYear          = 2026
  private val taxMonth         = 4
  private val amendmentDetails = AmendmentDetails(instanceId, taxYear, taxMonth)

  private val mockResponse = GetAllMonthlyReturnDetailsResponse(
    scheme = Seq(
      ContractorScheme(
        schemeId = 1,
        instanceId = instanceId,
        accountsOfficeReference = "123PA12345678",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456"
      )
    ),
    monthlyReturn = Seq(
      MonthlyReturn(monthlyReturnId = 101, taxYear = taxYear, taxMonth = taxMonth, status = Some("STARTED"))
    ),
    subcontractors = Seq(
      MonthlyReturnSubcontractor(
        subcontractorId = 1001,
        utr = None,
        pageVisited = None,
        partnerUtr = None,
        crn = None,
        firstName = Some("John"),
        nino = None,
        secondName = None,
        surname = Some("Doe"),
        partnershipTradingName = None,
        tradingName = Some("Test Subcontractor Ltd"),
        subcontractorType = None,
        addressLine1 = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        country = None,
        postCode = None,
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = None,
        worksReferenceNumber = None,
        createDate = None,
        lastUpdate = None,
        subbieResourceRef = None,
        matched = None,
        autoVerified = None,
        verified = None,
        verificationNumber = None,
        taxTreatment = None,
        verificationDate = None,
        version = None,
        updatedTaxTreatment = None,
        lastMonthlyReturnDate = None,
        pendingVerifications = None,
        displayName = Some("Test Subcontractor Ltd")
      ),
      MonthlyReturnSubcontractor(
        subcontractorId = 1002,
        utr = None,
        pageVisited = None,
        partnerUtr = None,
        crn = None,
        firstName = Some("John"),
        nino = None,
        secondName = None,
        surname = Some("Doe"),
        partnershipTradingName = None,
        tradingName = Some("Test Subcontractor2 Ltd"),
        subcontractorType = None,
        addressLine1 = None,
        addressLine2 = None,
        addressLine3 = None,
        addressLine4 = None,
        country = None,
        postCode = None,
        emailAddress = None,
        phoneNumber = None,
        mobilePhoneNumber = None,
        worksReferenceNumber = None,
        createDate = None,
        lastUpdate = None,
        subbieResourceRef = None,
        matched = None,
        autoVerified = None,
        verified = None,
        verificationNumber = None,
        taxTreatment = None,
        verificationDate = None,
        version = None,
        updatedTaxTreatment = None,
        lastMonthlyReturnDate = None,
        pendingVerifications = None,
        displayName = Some("Test Subcontractor2 Ltd")
      )
    ),
    monthlyReturnItems = Seq(
      MonthlyReturnItem(
        monthlyReturnId = 101,
        monthlyReturnItemId = 2001,
        totalPayments = None,
        costOfMaterials = None,
        totalDeducted = None,
        unmatchedTaxRateIndicator = None,
        subcontractorId = Some(1001),
        subcontractorName = None,
        verificationNumber = None,
        itemResourceReference = None
      )
    ),
    submission = Seq.empty
  )

  val mockPreSelectedIds: Set[String]             = Set("1001")
  val mockSubcontractors: Seq[AmendSubcontractor] = Seq(
    AmendSubcontractor("1001", "Test Subcontractor Ltd"),
    AmendSubcontractor("1002", "Test Subcontractor2 Ltd")
  )

  private val subcontractors   = mockSubcontractors
  private val preSelectedItems = WhichSubcontractorsToAdd.checkboxItems(subcontractors, mockPreSelectedIds)
  private val emptyItems       = WhichSubcontractorsToAdd.checkboxItems(subcontractors)
  val formProvider             = new WhichSubcontractorsToAddFormProvider()
  val form: Form[Set[String]]  = formProvider(subcontractors)

  "WhichSubcontractorsToAdd Controller" - {

    "onPageLoad" - {

      "must return OK and the correct view" in {
        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(mockResponse))

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

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

      "must populate the view correctly when the question has previously been answered" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(mockResponse))

        val selectedIds = Set(subcontractors.head.id)

        val userAnswers = userAnswersWithCisId
          .set(AmendmentDetailsPage, amendmentDetails)
          .success
          .value
          .set(WhichSubcontractorsToAddPage, selectedIds)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

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

      "must redirect to Journey Recovery if no existing data is found" in {

        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to System Error Page when API failed" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.failed(new RuntimeException("boom")))

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

        running(application) {
          val request = FakeRequest(GET, whichSubcontractorsToAddRoute)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.SystemErrorController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      "must redirect to the next page when valid data is submitted and Submission status = STARTED" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(mockResponse))

        when(
          monthlyReturnService.syncMonthlyReturnItems(
            eqTo("1"),
            eqTo(2026),
            eqTo(4),
            eqTo(mockPreSelectedIds.toSeq.map(_.toLong))
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
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

      "must redirect to the next page when valid data is submitted and Submission status = VALIDATED" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(
          Future.successful(
            mockResponse.copy(
              monthlyReturn = mockResponse.monthlyReturn.updated(
                0,
                mockResponse.monthlyReturn.head.copy(status = Some("VALIDATED"))
              )
            )
          )
        )

        when(
          monthlyReturnService.syncMonthlyReturnItems(
            eqTo(instanceId),
            eqTo(taxYear),
            eqTo(taxMonth),
            eqTo(mockPreSelectedIds.toSeq.map(_.toLong))
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
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

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(mockResponse))

        val selectedIds: Set[String] = Set("1001", "1002")
        when(
          monthlyReturnService.syncMonthlyReturnItems(
            eqTo(instanceId),
            eqTo(taxYear),
            eqTo(taxMonth),
            eqTo(selectedIds.toSeq.map(_.toLong))
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
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

      "must return a Bad Request and errors when invalid data is submitted" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.successful(mockResponse))

        when(
          monthlyReturnService.syncMonthlyReturnItems(
            eqTo(instanceId),
            eqTo(taxYear),
            eqTo(taxMonth),
            eqTo(mockPreSelectedIds.toSeq.map(_.toLong))
          )(any[HeaderCarrier])
        ).thenReturn(Future.successful(()))

        when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute)),
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
          .build()

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

      "must redirect to Journey Recovery if no existing data is found" in {

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

      "must redirect to Journey Recovery when Submission status is not STARTED or VALIDATED" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(
          Future.successful(
            mockResponse.copy(
              monthlyReturn = mockResponse.monthlyReturn.updated(
                0,
                mockResponse.monthlyReturn.head.copy(status = Some("ACCEPTED"))
              )
            )
          )
        )

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
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

      "must redirect to System Error Page when API failed" in {

        val monthlyReturnService  = mock[MonthlyReturnService]
        val mockSessionRepository = mock[SessionRepository]

        when(
          monthlyReturnService.retrieveMonthlyReturnForEditDetails(eqTo(instanceId), eqTo(taxMonth), eqTo(taxYear))(
            any[HeaderCarrier]
          )
        ).thenReturn(Future.failed(new RuntimeException("boom")))

        val userAnswers = userAnswersWithCisId.set(AmendmentDetailsPage, amendmentDetails).success.value

        val application = applicationBuilder(userAnswers = Some(userAnswers))
          .overrides(
            bind[SessionRepository].toInstance(mockSessionRepository),
            bind[MonthlyReturnService].toInstance(monthlyReturnService)
          )
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
}
