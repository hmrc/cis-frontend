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
import com.google.inject.AbstractModule
import forms.monthlyreturns.SelectSubcontractorsFormProvider
import models.monthlyreturns._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import pages.monthlyreturns.{CisIdPage, DateConfirmPaymentsPage}
import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.SelectSubcontractorsViewModel
import views.html.monthlyreturns.SelectSubcontractorsView

import java.time.LocalDate
import scala.concurrent.Future

class SelectSubcontractorsControllerSpec extends SpecBase with MockitoSugar {

  "SelectSubcontractors Controller" - {
    val formProvider = new SelectSubcontractorsFormProvider()
    val form         = formProvider()

    val testCisId    = "CIS-123"
    val testTaxDate  = LocalDate.of(2025, 10, 15)
    val testTaxMonth = testTaxDate.getMonthValue
    val testTaxYear  = testTaxDate.getYear

    def createSubcontractor(
      id: Long,
      tradingName: Option[String],
      subbieResourceRef: Option[Long] = None
    ): Subcontractor =
      Subcontractor(
        subcontractorId = id,
        utr = None,
        pageVisited = None,
        partnerUtr = None,
        crn = None,
        firstName = None,
        nino = None,
        secondName = None,
        surname = None,
        partnershipTradingName = None,
        tradingName = tradingName,
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
        subbieResourceRef = subbieResourceRef,
        matched = None,
        autoVerified = None,
        verified = None,
        verificationNumber = None,
        taxTreatment = None,
        verificationDate = None,
        version = None,
        updatedTaxTreatment = None,
        lastMonthlyReturnDate = None,
        pendingVerifications = None
      )

    def createMonthlyReturnItem(
      monthlyReturnItemId: Long,
      itemResourceReference: Option[Long]
    ): MonthlyReturnItem =
      MonthlyReturnItem(
        monthlyReturnId = 1L,
        monthlyReturnItemId = monthlyReturnItemId,
        totalPayments = None,
        costOfMaterials = None,
        totalDeducted = None,
        unmatchedTaxRateIndicator = None,
        subcontractorId = None,
        subcontractorName = None,
        verificationNumber = None,
        itemResourceReference = itemResourceReference
      )

    def mockGetAllMonthlyReturnDetailsResponse(
      subcontractors: Seq[Subcontractor],
      monthlyReturnItems: Seq[MonthlyReturnItem] = Seq.empty
    ): GetAllMonthlyReturnDetailsResponse =
      GetAllMonthlyReturnDetailsResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = subcontractors,
        monthlyReturnItems = monthlyReturnItems,
        submission = Seq.empty
      )

    val backendSubcontractors = Seq(
      createSubcontractor(1, Some("Alice, A"), Some(1L)),
      createSubcontractor(2, Some("Bob, B"), Some(2L)),
      createSubcontractor(3, Some("Charles, C"), Some(3L))
    )

    val expectedViewModels = Seq(
      SelectSubcontractorsViewModel(1, "Alice, A", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(2, "Bob, B", "Yes", "Unknown", "Unknown"),
      SelectSubcontractorsViewModel(3, "Charles, C", "Yes", "Unknown", "Unknown")
    )

    val userAnswersWithRequiredPages = emptyUserAnswers
      .set(CisIdPage, testCisId)
      .success
      .value
      .set(DateConfirmPaymentsPage, testTaxDate)
      .success
      .value

    "onPageLoad" - {

      "must return OK and pre-select subcontractors included in the last submitted monthly return when required user answers are present" in {
        val mockService        = mock[MonthlyReturnService]
        val monthlyReturnItems = Seq(
          createMonthlyReturnItem(1L, Some(1L)),
          createMonthlyReturnItem(2L, Some(3L))
        )
        when(
          mockService.retrieveMonthlyReturnForEditDetails(eqTo(testCisId), eqTo(testTaxMonth), eqTo(testTaxYear))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(mockGetAllMonthlyReturnDetailsResponse(backendSubcontractors, monthlyReturnItems))
          )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            new AbstractModule {
              override def configure(): Unit =
                bind(classOf[MonthlyReturnService]).toInstance(mockService)
            }
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
          )

          val result           = route(application, request).value
          val view             = application.injector.instanceOf[SelectSubcontractorsView]
          val expectedFormData =
            SelectSubcontractorsFormData(confirmation = false, subcontractorsToInclude = List(1, 3))
          val expectedForm     = form.fill(expectedFormData)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(expectedForm, expectedViewModels)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK with all checkboxes selected when defaultSelection is true" in {
        val mockService        = mock[MonthlyReturnService]
        val monthlyReturnItems = Seq(
          createMonthlyReturnItem(1L, Some(1L))
        )
        when(
          mockService.retrieveMonthlyReturnForEditDetails(eqTo(testCisId), eqTo(testTaxMonth), eqTo(testTaxYear))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(mockGetAllMonthlyReturnDetailsResponse(backendSubcontractors, monthlyReturnItems))
          )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            new AbstractModule {
              override def configure(): Unit =
                bind(classOf[MonthlyReturnService]).toInstance(mockService)
            }
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(Some(true)).url
          )

          val result             = route(application, request).value
          val view               = application.injector.instanceOf[SelectSubcontractorsView]
          val expectedFormData   = SelectSubcontractorsFormData(false, List(1, 2, 3))
          val expectedFilledForm = form.fill(expectedFormData)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(expectedFilledForm, expectedViewModels)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK with no checkboxes selected when defaultSelection is false" in {
        val mockService        = mock[MonthlyReturnService]
        val monthlyReturnItems = Seq(
          createMonthlyReturnItem(1L, Some(1L))
        )
        when(
          mockService.retrieveMonthlyReturnForEditDetails(eqTo(testCisId), eqTo(testTaxMonth), eqTo(testTaxYear))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(mockGetAllMonthlyReturnDetailsResponse(backendSubcontractors, monthlyReturnItems))
          )

        val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            new AbstractModule {
              override def configure(): Unit =
                bind(classOf[MonthlyReturnService]).toInstance(mockService)
            }
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(Some(false)).url
          )

          val result = route(application, request).value
          val view   = application.injector.instanceOf[SelectSubcontractorsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, expectedViewModels)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return OK with empty subcontractors list when backend returns no subcontractors" in {
        val mockService = mock[MonthlyReturnService]
        when(
          mockService.retrieveMonthlyReturnForEditDetails(eqTo(testCisId), eqTo(testTaxMonth), eqTo(testTaxYear))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(mockGetAllMonthlyReturnDetailsResponse(Seq.empty)))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            new AbstractModule {
              override def configure(): Unit =
                bind(classOf[MonthlyReturnService]).toInstance(mockService)
            }
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
          )

          val result = route(application, request).value
          val view   = application.injector.instanceOf[SelectSubcontractorsView]

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, Seq.empty)(request, messages(application)).toString
        }
      }

      "must use 'Unknown' for tradingName when subcontractor has no tradingName" in {
        val subcontractorWithNoName = createSubcontractor(99, None)
        val mockService             = mock[MonthlyReturnService]
        when(
          mockService.retrieveMonthlyReturnForEditDetails(eqTo(testCisId), eqTo(testTaxMonth), eqTo(testTaxYear))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(mockGetAllMonthlyReturnDetailsResponse(Seq(subcontractorWithNoName))))

        val application = applicationBuilder(userAnswers = Some(userAnswersWithRequiredPages))
          .overrides(
            new AbstractModule {
              override def configure(): Unit =
                bind(classOf[MonthlyReturnService]).toInstance(mockService)
            }
          )
          .build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
          )

          val result            = route(application, request).value
          val view              = application.injector.instanceOf[SelectSubcontractorsView]
          val expectedViewModel = SelectSubcontractorsViewModel(99, "Unknown", "Yes", "Unknown", "Unknown")

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(form, Seq(expectedViewModel))(request, messages(application)).toString
        }
      }

      "must redirect to JourneyRecovery when CisIdPage is missing" in {
        val userAnswersMissingCisId = emptyUserAnswers
          .set(DateConfirmPaymentsPage, testTaxDate)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswersMissingCisId)).build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecovery when DateConfirmPaymentsPage is missing" in {
        val userAnswersMissingDate = emptyUserAnswers
          .set(CisIdPage, testCisId)
          .success
          .value

        val application = applicationBuilder(userAnswers = Some(userAnswersMissingDate)).build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }

      "must redirect to JourneyRecovery when no user answers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(
            GET,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onPageLoad(None).url
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onSubmit" - {

      val staticSubcontractors = Seq(
        SelectSubcontractorsViewModel(1, "Alice, A", "Yes", "Unknown", "Unknown"),
        SelectSubcontractorsViewModel(2, "Bob, B", "Yes", "Unknown", "Unknown"),
        SelectSubcontractorsViewModel(3, "Charles, C", "Yes", "Unknown", "Unknown"),
        SelectSubcontractorsViewModel(4, "Dave, D", "Yes", "Unknown", "Unknown"),
        SelectSubcontractorsViewModel(5, "Elise, E", "Yes", "Unknown", "Unknown"),
        SelectSubcontractorsViewModel(6, "Frank, F", "Yes", "Unknown", "Unknown")
      )

      "must return OK and retain form data when form is valid and confirmation is true" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url
          ).withBody(
            AnyContentAsFormUrlEncoded(
              Map(
                "confirmation"              -> Seq("true"),
                "subcontractorsToInclude.0" -> Seq("1"),
                "subcontractorsToInclude.1" -> Seq("2"),
                "subcontractorsToInclude.2" -> Seq("3")
              )
            )
          )

          val result             = route(application, request).value
          val view               = application.injector.instanceOf[SelectSubcontractorsView]
          val expectedFormData   = SelectSubcontractorsFormData(true, List(1, 2, 3))
          val expectedFilledForm = form.fill(expectedFormData)

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(expectedFilledForm, staticSubcontractors)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return BAD_REQUEST when form has binding errors" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url
          ).withBody(
            AnyContentAsFormUrlEncoded(
              Map(
                "confirmation"              -> Seq("false"),
                "subcontractorsToInclude.0" -> Seq("invalid")
              )
            )
          )

          val result    = route(application, request).value
          val view      = application.injector.instanceOf[SelectSubcontractorsView]
          val boundForm = form.bind(
            Map(
              "confirmation"              -> "false",
              "subcontractorsToInclude.0" -> "invalid"
            )
          )

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(boundForm, staticSubcontractors)(
            request,
            messages(application)
          ).toString
        }
      }

      "must return BAD_REQUEST with error when confirmation is false" in {
        val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url
          ).withBody(
            AnyContentAsFormUrlEncoded(
              Map(
                "confirmation"              -> Seq("false"),
                "subcontractorsToInclude.0" -> Seq("1"),
                "subcontractorsToInclude.1" -> Seq("2"),
                "subcontractorsToInclude.2" -> Seq("3")
              )
            )
          )

          val result           = route(application, request).value
          val view             = application.injector.instanceOf[SelectSubcontractorsView]
          val expectedFormData = SelectSubcontractorsFormData(
            confirmation = false,
            subcontractorsToInclude = List(1, 2, 3)
          )
          val formWithError    = form
            .withError("confirmation", "monthlyreturns.selectSubcontractors.confirmation.required")
            .fill(expectedFormData)

          status(result) mustEqual BAD_REQUEST
          contentAsString(result) mustEqual view(formWithError, staticSubcontractors)(
            request,
            messages(application)
          ).toString
        }
      }

      "must redirect to JourneyRecovery when no user answers exist" in {
        val application = applicationBuilder(userAnswers = None).build()

        running(application) {
          val request = FakeRequest(
            POST,
            controllers.monthlyreturns.routes.SelectSubcontractorsController.onSubmit().url
          ).withBody(
            AnyContentAsFormUrlEncoded(
              Map("confirmation" -> Seq("true"))
            )
          )

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual controllers.routes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }
  }
}
