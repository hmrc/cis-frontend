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

package controllers.finalvalidations

import base.SpecBase
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import views.html.finalvalidations.UpdateSubcontractorDetailsView
import models.finalvalidation.*
import models.UserAnswers
import pages.finalvalidations.FinalValidationDraftIdPage
import services.finalvalidation.{FinalValidationDraftService, FinalValidationService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import pages.monthlyreturns.CisIdPage
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.i18n.Messages
import play.api.test.CSRFTokenHelper
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future
import scala.util.Success

class UpdateSubcontractorDetailsControllerSpec extends SpecBase {

  private val cisId =
    "CIS-123"

  private val draftId =
    "draft-123"

  private val subcontractorId =
    10903L

  private lazy val updateSubcontractorDetailsRoute =
    controllers.finalvalidations.routes.UpdateSubcontractorDetailsController
      .onPageLoad(subcontractorId)
      .url

  private lazy val submitSubcontractorDetailsRoute =
    controllers.finalvalidations.routes.UpdateSubcontractorDetailsController
      .onSubmit(subcontractorId)
      .url

  private val userAnswers =
    emptyUserAnswers
      .setOrException(CisIdPage, cisId)
      .setOrException(
        FinalValidationDraftIdPage,
        draftId
      )

  private def draft(
    readiness: String = "Incomplete",
    id: Long = subcontractorId
  ): FinalValidationDraft =
    Json
      .obj(
        "subcontractors" -> Json.arr(
          Json.obj(
            "subcontractorId"   -> id,
            "subbieResourceRef" -> 7L,
            "baseVersion"       -> 12,
            "subcontractorType" -> "soletrader",
            "displayName"       -> "Hooper And Associates",
            "base"              -> Json.obj(
              "firstName" -> "A",
              "surname"   -> "Alice",
              "utr"       -> "1111111111",
              "nino"      -> "PX123456A"
            ),
            "proposed"          -> Json.obj(
              "firstName" -> "A",
              "surname"   -> "Alice",
              "utr"       -> "2234567890",
              "nino"      -> "PX123456A"
            ),
            "changedTargets"    -> Json.arr("utr"),
            "issues"            -> Json.arr(),
            "readiness"         -> readiness,
            "commitStatus"      -> "Pending"
          )
        )
      )
      .as[FinalValidationDraft]

  "UpdateSubcontractorDetails Controller" - {

    "must return OK and the correct view for a GET" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      val incompleteDraft =
        draft()

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(incompleteDraft)
      )

      val application =
        applicationWith(
          userAnswers = Some(userAnswers),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          CSRFTokenHelper.addCSRFToken(
            FakeRequest(
              GET,
              updateSubcontractorDetailsRoute
            )
          )

        val result =
          route(application, request).value

        val view =
          application.injector
            .instanceOf[UpdateSubcontractorDetailsView]

        val pageModelBuilder =
          application.injector
            .instanceOf[UpdateSubcontractorDetailsPageModelBuilder]

        val subcontractor =
          incompleteDraft
            .subcontractor(subcontractorId)
            .value

        given Messages =
          messages(application)

        val rows =
          pageModelBuilder.build(
            subcontractor,
            (field, target) =>
              controllers.finalvalidations.routes.FinalValidationChangeController
                .onPageLoad(
                  subcontractorId,
                  field.key,
                  target.key
                )
                .url
          )

        val model =
          UpdateSubcontractorDetailsPageModel(
            subcontractorId,
            "Hooper And Associates",
            rows
          )

        status(result) mustBe OK

        contentAsString(result) mustBe
          view(model)(
            request,
            messages(application)
          ).toString
      }
    }

    "must redirect to Review Subcontractor Details for a GET when the subcontractor is complete" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(readiness = "Complete")
        )
      )

      val application =
        applicationWith(
          userAnswers = Some(userAnswers),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            GET,
            updateSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.finalvalidations.routes.ReviewSubcontractorDetailsController
            .onPageLoad()
            .url
      }
    }

    "must redirect to Journey Recovery for a GET when the subcontractor is not in the draft" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(id = 99999L)
        )
      )

      val application =
        applicationWith(
          userAnswers = Some(userAnswers),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            GET,
            updateSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }

    "must redirect to Journey Recovery for a GET when the draft id is not found" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      val answersWithoutDraftId =
        emptyUserAnswers
          .setOrException(CisIdPage, cisId)

      val application =
        applicationWith(
          userAnswers = Some(answersWithoutDraftId),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            GET,
            updateSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      val application =
        applicationWith(
          userAnswers = None,
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            GET,
            updateSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url
      }
    }

    "must validate the draft subcontractor, update readiness and redirect for a POST" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      val incompleteDraft =
        draft()

      val issues =
        Seq.empty[FinalValidationDraftIssue]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(incompleteDraft)
      )

      when(
        finalValidationService.validateDraftSubcontractor(
          incompleteDraft,
          subcontractorId
        )
      ).thenReturn(
        Success(issues)
      )

      when(
        finalValidationDraftService.updateReadiness(
          any[String],
          any[String],
          any[Long],
          any[Seq[FinalValidationDraftIssue]]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(incompleteDraft)
      )

      val application =
        applicationWith(
          userAnswers = Some(userAnswers),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.finalvalidations.routes.ReviewSubcontractorDetailsController
            .onPageLoad()
            .url

        verify(finalValidationService)
          .validateDraftSubcontractor(
            incompleteDraft,
            subcontractorId
          )

        verify(finalValidationDraftService)
          .updateReadiness(
            any[String],
            any[String],
            any[Long],
            any[Seq[FinalValidationDraftIssue]]
          )(any[HeaderCarrier])
      }
    }

    "must redirect to Review Subcontractor Details for a POST when the subcontractor is already complete" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(readiness = "Complete")
        )
      )

      val application =
        applicationWith(
          userAnswers = Some(userAnswers),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.finalvalidations.routes.ReviewSubcontractorDetailsController
            .onPageLoad()
            .url

        verifyNoInteractions(finalValidationService)
      }
    }

    "must redirect to Journey Recovery for a POST when the subcontractor is not in the draft" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      when(
        finalValidationDraftService.get(
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          draft(id = 99999L)
        )
      )

      val application =
        applicationWith(
          userAnswers = Some(userAnswers),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url

        verifyNoInteractions(finalValidationService)
      }
    }

    "must redirect to Journey Recovery for a POST when the draft id is not found" in {

      val finalValidationDraftService =
        mock[FinalValidationDraftService]

      val finalValidationService =
        mock[FinalValidationService]

      val answersWithoutDraftId =
        emptyUserAnswers
          .setOrException(CisIdPage, cisId)

      val application =
        applicationWith(
          userAnswers = Some(answersWithoutDraftId),
          finalValidationDraftService = finalValidationDraftService,
          finalValidationService = finalValidationService
        )

      running(application) {
        val request =
          FakeRequest(
            POST,
            submitSubcontractorDetailsRoute
          )

        val result =
          route(application, request).value

        status(result) mustBe SEE_OTHER

        redirectLocation(result).value mustBe
          controllers.routes.JourneyRecoveryController
            .onPageLoad()
            .url

        verifyNoInteractions(
          finalValidationDraftService,
          finalValidationService
        )
      }
    }
  }

  private def applicationWith(
    userAnswers: Option[UserAnswers],
    finalValidationDraftService: FinalValidationDraftService,
    finalValidationService: FinalValidationService
  ) =
    applicationBuilder(
      userAnswers = userAnswers,
      additionalBindings = Seq(
        bind[FinalValidationDraftService]
          .toInstance(finalValidationDraftService),
        bind[FinalValidationService]
          .toInstance(finalValidationService)
      )
    ).build()
}
