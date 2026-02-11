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
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant

class SubcontractorDetailsAddedControllerSpec extends SpecBase {

  "SubcontractorDetailsAddedController" - {

    "must return OK on GET when builder returns a view model" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId"  -> 1001L,
              "name"             -> "TyneWear Ltd",
              "totalPaymentsMade" -> 1000.00,
              "costOfMaterials"  -> 200.00,
              "totalTaxDeducted" -> 200.00
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router"           -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout"     -> 900,
            "timeout-dialog.countdown"   -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host"                       -> "http://localhost"
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, "/monthly-return/subcontractor-details-added")
        val result  = route(application, request).value

        status(result) mustBe OK
      }
    }

    "must redirect to SystemError on GET when there are 0 subcontractors (builder returns None)" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj("subcontractors" -> Json.arr()),
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router"           -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout"     -> 900,
            "timeout-dialog.countdown"   -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host"                       -> "http://localhost"
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, "/monthly-return/subcontractor-details-added")
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must return BadRequest on POST Yes when viewModel.hasIncomplete = true (incomplete error)" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId"  -> 1001L,
              "name"             -> "Complete Ltd",
              "totalPaymentsMade" -> 1000.00,
              "costOfMaterials"  -> 200.00,
              "totalTaxDeducted" -> 200.00
            ),
            Json.obj(
              "subcontractorId"  -> 1002L,
              "name"             -> "Incomplete Ltd"
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router"           -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout"     -> 900,
            "timeout-dialog.countdown"   -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host"                       -> "http://localhost"
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, "/monthly-return/subcontractor-details-added")
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST

        contentAsString(result) must include(
          "You have not entered payment details for all of your selected subcontractors"
        )
      }
    }

    "must redirect to SystemError on POST when builder returns None" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj("subcontractors" -> Json.arr()), // builder None
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router" -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout" -> 900,
            "timeout-dialog.countdown" -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host" -> "http://localhost"
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, "/monthly-return/subcontractor-details-added")
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must return BadRequest on POST when form has errors" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId" -> 1001L,
              "name" -> "TyneWear Ltd",
              "totalPaymentsMade" -> 1000.00,
              "costOfMaterials" -> 200.00,
              "totalTaxDeducted" -> 200.00
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router" -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout" -> 900,
            "timeout-dialog.countdown" -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host" -> "http://localhost"
          )
          .build()

      running(application) {
        val request = FakeRequest(POST, "/monthly-return/subcontractor-details-added")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "must redirect on POST Yes when viewModel.hasIncomplete = false" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId" -> 1001L,
              "name" -> "Complete Ltd",
              "totalPaymentsMade" -> 1000.00,
              "costOfMaterials" -> 200.00,
              "totalTaxDeducted" -> 200.00
            ),
            Json.obj(
              "subcontractorId" -> 1002L,
              "name" -> "Also Complete Ltd",
              "totalPaymentsMade" -> 500.00,
              "costOfMaterials" -> 50.00,
              "totalTaxDeducted" -> 50.00
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router" -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout" -> 900,
            "timeout-dialog.countdown" -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host" -> "http://localhost"
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, "/monthly-return/subcontractor-details-added")
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoad(NormalMode)
            .url
      }
    }

    "must redirect on POST No (answer == false)" in {

      val ua = UserAnswers(
        id = userAnswersId,
        data = Json.obj(
          "subcontractors" -> Json.arr(
            Json.obj(
              "subcontractorId" -> 1001L,
              "name" -> "Complete Ltd",
              "totalPaymentsMade" -> 1000.00,
              "costOfMaterials" -> 200.00,
              "totalTaxDeducted" -> 200.00
            )
          )
        ),
        lastUpdated = Instant.now
      )

      val application =
        applicationBuilder(userAnswers = Some(ua))
          .configure(
            "play.http.router" -> "testOnly.TestRoutes",
            "features.welsh-translation" -> false,
            "timeout-dialog.timeout" -> 900,
            "timeout-dialog.countdown" -> 120,
            "contact-frontend.serviceId" -> "cis-frontend",
            "host" -> "http://localhost"
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, "/monthly-return/subcontractor-details-added")
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SubcontractorDetailsAddedController
            .onPageLoad(NormalMode)
            .url
      }
    }
  }
}
