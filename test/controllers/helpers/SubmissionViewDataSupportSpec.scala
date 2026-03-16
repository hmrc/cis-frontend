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

package controllers.helpers

import models.ReturnType.{MonthlyNilReturn, MonthlyStandardReturn}
import models.agent.AgentClientData
import models.{EmployerReference, ReturnType, UserAnswers}
import models.requests.DataRequest
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{ConfirmEmailAddressPage, ContractorNamePage, DateConfirmNilPaymentsPage, DateConfirmPaymentsPage, EnterYourEmailAddressPage}

import java.time.LocalDate
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

class SubmissionViewDataSupportSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private object Harness extends SubmissionViewDataSupport {
    def requiredPublic[A](opt: Option[A], err: String): A                  = required(opt, err)
    def emailPublic(ua: UserAnswers, t: ReturnType): Option[String]        = emailfromUserAnswers(ua, t)
    def periodEndPublic(ua: UserAnswers, t: ReturnType): Option[LocalDate] = periodEndFromUserAnswers(ua, t)
    def contractorNamePublic(r: DataRequest[_]): String                    = contractorNameFrom(r)
    def employerRefPublic(r: DataRequest[_]): String                       = employerRefFrom(r)
  }

  private def uaEmpty: UserAnswers = UserAnswers("id", Json.obj())

  private def dataRequest(
    ua: UserAnswers,
    isAgent: Boolean,
    employerRef: Option[EmployerReference] = None,
    userId: String = "user-1"
  ): DataRequest[AnyContent] = {
    val dr = mock[DataRequest[AnyContent]]
    when(dr.userAnswers).thenReturn(ua)
    when(dr.isAgent).thenReturn(isAgent)
    when(dr.employerReference).thenReturn(employerRef)
    when(dr.userId).thenReturn(userId)
    when(dr.request).thenReturn(FakeRequest())
    dr
  }

  "SubmissionViewDataSupport" should {

    "required returns value and throws when None" in {
      Harness.requiredPublic(Some(1), "boom") mustBe 1

      val ex = intercept[IllegalStateException] {
        Harness.requiredPublic(None, "boom-err")
      }
      ex.getMessage must include("boom-err")
    }

    "emailfromUserAnswers trims and uses correct page per return type" in {
      val uaNil = uaEmpty.set(ConfirmEmailAddressPage, "  test@test.com  ").get
      Harness.emailPublic(uaNil, MonthlyNilReturn) mustBe Some("test@test.com")

      val uaStd = uaEmpty.set(EnterYourEmailAddressPage, "   ").get
      Harness.emailPublic(uaStd, MonthlyStandardReturn) mustBe None
    }

    "periodEndFromUserAnswers uses correct page per return type" in {
      val d1 = LocalDate.of(2025, 9, 1)
      val d2 = LocalDate.of(2025, 10, 1)

      val ua = uaEmpty
        .set(DateConfirmNilPaymentsPage, d1)
        .get
        .set(DateConfirmPaymentsPage, d2)
        .get

      Harness.periodEndPublic(ua, MonthlyNilReturn) mustBe Some(d1)
      Harness.periodEndPublic(ua, MonthlyStandardReturn) mustBe Some(d2)
    }

    "contractorNameFrom uses ContractorNamePage for contractor" in {
      val ua  = uaEmpty.set(ContractorNamePage, "ACME LTD").get
      val req = dataRequest(ua, isAgent = false)

      Harness.contractorNamePublic(req) mustBe "ACME LTD"
    }

    "contractorNameFrom uses AgentClientData.schemeName for agent (else throws)" in {
      val agentData = AgentClientData("CLIENT-1", "123", "AB456", Some("PAL 355 Scheme"))
      val uaOk      = uaEmpty.set(AgentClientDataPage, agentData).get

      Harness.contractorNamePublic(dataRequest(uaOk, isAgent = true)) mustBe "PAL 355 Scheme"

      val uaBad = uaEmpty.set(AgentClientDataPage, agentData.copy(schemeName = None)).get
      val ex    = intercept[IllegalStateException] {
        Harness.contractorNamePublic(dataRequest(uaBad, isAgent = true))
      }
      ex.getMessage must include("contractorName missing")
    }

    "employerRefFrom uses employerReference for contractor and agent client data for agent" in {
      val contractorReq =
        dataRequest(
          ua = uaEmpty,
          isAgent = false,
          employerRef = Some(EmployerReference("123", "AB456"))
        )
      Harness.employerRefPublic(contractorReq) mustBe "123/AB456"

      val agentData = AgentClientData("CLIENT-1", "999", "XYZ123", Some("PAL 355 Scheme"))
      val uaAgent   = uaEmpty.set(AgentClientDataPage, agentData).get

      Harness.employerRefPublic(dataRequest(uaAgent, isAgent = true)) mustBe "999/XYZ123"

      val uaMissing = uaEmpty.set(AgentClientDataPage, agentData.copy(taxOfficeNumber = "")).get
      val ex        = intercept[IllegalStateException] {
        Harness.employerRefPublic(dataRequest(uaMissing, isAgent = true))
      }
      ex.getMessage must include("employerReference missing")
    }
  }
}
