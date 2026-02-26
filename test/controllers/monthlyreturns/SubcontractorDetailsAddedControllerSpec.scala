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
import models.UserAnswers
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*

import java.time.Instant

class SubcontractorDetailsAddedControllerSpec extends SpecBase {

  private val now: Instant = Instant.now

  private def uaWithSubcontractors(subs: (Int, JsObject)*): UserAnswers =
    UserAnswers(
      id = userAnswersId,
      data = Json.obj(
        "subcontractors" -> JsObject(subs.map { case (i, o) => i.toString -> o })
      ),
      lastUpdated = now
    )

  private def completeSub(id: Long, name: String): JsObject =
    Json.obj(
      "id"                -> id,
      "name"              -> name,
      "totalPaymentsMade" -> 1000.00,
      "costOfMaterials"   -> 200.00,
      "totalTaxDeducted"  -> 200.00
    )

  private def incompleteSub(id: Long, name: String): JsObject =
    Json.obj(
      "id"   -> id,
      "name" -> name
    )

  private def buildApp(ua: UserAnswers) =
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

  private val getUrl  = "/monthly-return/subcontractor-details-added"
  private val postUrl = "/monthly-return/subcontractor-details-added"

  "SubcontractorDetailsAddedController" - {

    "must return OK on GET when builder returns a view model" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe OK
      }
    }

    "must redirect to SystemError on GET when builder returns None (no details-added rows)" in {
      val ua = uaWithSubcontractors(
        1 -> incompleteSub(1001L, "Incomplete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(GET, getUrl)
        val result  = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must return BadRequest on POST Yes when viewModel.hasIncomplete = true (incomplete error)" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd"),
        2 -> incompleteSub(1002L, "Incomplete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe BAD_REQUEST
        contentAsString(result) must include(
          "You have not entered payment details for all of your selected subcontractors"
        )
      }
    }

    "must redirect to SystemError on POST when builder returns None" in {
      val ua = uaWithSubcontractors(
        1 -> incompleteSub(1001L, "Incomplete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe controllers.routes.SystemErrorController.onPageLoad().url
      }
    }

    "must return BadRequest on POST when form has errors (no value)" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "TyneWear Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request = FakeRequest(POST, postUrl)
        val result  = route(application, request).value

        status(result) mustBe BAD_REQUEST
      }
    }

    "must redirect on POST Yes when viewModel.hasIncomplete = false" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd"),
        2 -> completeSub(1002L, "Also Complete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "true")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SummarySubcontractorPaymentsController
            .onPageLoad()
            .url
      }
    }

    "must redirect on POST No (answer == false)" in {
      val ua = uaWithSubcontractors(
        1 -> completeSub(1001L, "Complete Ltd")
      )

      val application = buildApp(ua)

      running(application) {
        val request =
          FakeRequest(POST, postUrl)
            .withFormUrlEncodedBody("value" -> "false")

        val result = route(application, request).value

        status(result) mustBe SEE_OTHER
        redirectLocation(result).value mustBe
          controllers.monthlyreturns.routes.SelectSubcontractorsController
            .onPageLoad(None)
            .url
      }
    }
  }
}
