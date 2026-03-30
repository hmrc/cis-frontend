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

package models.history

import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.Helpers.stubMessagesApi

class SubmittedReturnsChooseTaxYearSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with OptionValues {

  private val messageApi: MessagesApi = stubMessagesApi()
  implicit val messages: Messages     = messageApi.preferred(Seq.empty)

  "SubmittedReturnsChooseTaxYear" - {

    "must return a radio item for each tax year, plus a divider, plus a view-all option" in {
      val taxYears: Seq[String] =
        Seq("2021 to 2202", "2022 to 2023", "2023 to 2024", "2024 to 2025")
      val result                = SubmittedReturnsChooseTaxYear.options(taxYears)
      result.length mustEqual taxYears.length + 2 // divider + viewAll
    }

    "must create radio items for each tax year" in {
      val taxYears = Seq("2021 to 2022", "2022 to 2023")
      val result   = SubmittedReturnsChooseTaxYear.options(taxYears)

      result.head.value mustBe Some("2021 to 2022")
      result.head.id mustBe Some("value_0")

      result(1).value mustBe Some("2022 to 2023")
      result(1).id mustBe Some("value_1")
    }

    "must include a divider after the tax years" in {
      val taxYears = Seq("2021 to 2022")
      val result   = SubmittedReturnsChooseTaxYear.options(taxYears)

      val divider = result(1)
      divider.divider mustBe defined
    }

    "must include a 'view all' option" in {
      val taxYears = Seq("2021 to 2022")
      val result   = SubmittedReturnsChooseTaxYear.options(taxYears)

      val viewAll = result.last
      viewAll.value mustBe Some("all")
      viewAll.id mustBe Some("value_all")
    }
  }
}
