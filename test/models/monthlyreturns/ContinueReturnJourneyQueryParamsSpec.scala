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

package models.monthlyreturns

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.QueryStringBindable

class ContinueReturnJourneyQueryParamsSpec extends AnyWordSpec with Matchers {

  private val binder = implicitly[QueryStringBindable[ContinueReturnJourneyQueryParams]]

  "ContinueReturnJourneyQueryParams.queryStringBindable" should {

    "bind valid query params" in {
      val params = Map(
        "instanceId" -> Seq("CIS-123"),
        "taxYear"    -> Seq("2025"),
        "taxMonth"   -> Seq("7")
      )

      binder.bind("", params) mustBe Some(
        Right(
          ContinueReturnJourneyQueryParams(
            instanceId = "CIS-123",
            taxYear = 2025,
            taxMonth = 7
          )
        )
      )
    }

    "return None when a required query param is missing" in {
      val params = Map(
        "instanceId" -> Seq("CIS-123"),
        "taxYear"    -> Seq("2025")
      )

      binder.bind("", params) mustBe None
    }

    "return Left when a query param cannot be parsed" in {
      val params = Map(
        "instanceId" -> Seq("CIS-123"),
        "taxYear"    -> Seq("not-a-year"),
        "taxMonth"   -> Seq("7")
      )

      binder.bind("", params) mustBe Some(
        Left("Unable to bind ContinueReturnJourneyQueryParams")
      )
    }

    "unbind query params" in {
      val queryParams = ContinueReturnJourneyQueryParams(
        instanceId = "CIS-123",
        taxYear = 2025,
        taxMonth = 7
      )

      binder.unbind("", queryParams) mustBe
        "instanceId=CIS-123&taxYear=2025&taxMonth=7"
    }
  }
}
