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

package views.finalvalidations

import base.SpecBase
import models.finalvalidation.UpdateSubcontractorDetailsPageModel
import play.api.test.CSRFTokenHelper
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import views.html.finalvalidations.UpdateSubcontractorDetailsView

class UpdateSubcontractorDetailsViewSpec extends SpecBase {

  private val view =
    app.injector.instanceOf[UpdateSubcontractorDetailsView]

  private val request =
    CSRFTokenHelper.addCSRFToken(
      FakeRequest(GET, "/")
    )

  private val model =
    UpdateSubcontractorDetailsPageModel(
      subcontractorId = 1L,
      subcontractorName = "Test Subcontractor",
      rows = Seq.empty
    )

  "UpdateSubcontractorDetailsView" - {

    "render the page" in {
      val result =
        view(model)(request, messages(app)).toString

      result must include(
        messages(app)(
          "finalvalidations.updateSubcontractorDetails.heading",
          "Test Subcontractor"
        )
      )

      result must include(
        messages(app)(
          "finalvalidations.updateSubcontractorDetails.acceptAndSubmit"
        )
      )

      result must include(
        controllers.finalvalidations.routes.UpdateSubcontractorDetailsController
          .onSubmit(1L)
          .url
      )

      result must include(
        controllers.finalvalidations.routes.ReviewSubcontractorDetailsController
          .onPageLoad()
          .url
      )
    }
  }
}
