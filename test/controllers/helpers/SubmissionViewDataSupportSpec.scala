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

import models.agent.AgentClientData
import models.{EmployerReference, UserAnswers}
import models.requests.{CisIdDataRequest, DataRequest}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import pages.agent.AgentClientDataPage
import pages.monthlyreturns.{DateConfirmPaymentsPage, EnterYourEmailAddressPage}

import java.time.LocalDate
import play.api.libs.json.Json
import play.api.mvc.AnyContent
import play.api.test.FakeRequest

class SubmissionViewDataSupportSpec extends AnyWordSpec with Matchers with MockitoSugar {

  private object Harness extends SubmissionViewDataSupport {
    def requiredPublic[A](opt: Option[A], err: String): A   = required(opt, err)
    def emailPublic(ua: UserAnswers): Option[String]        = emailfromUserAnswers(ua)
    def periodEndPublic(ua: UserAnswers): Option[LocalDate] = periodEndFromUserAnswers(ua)
    def employerRefPublic(r: CisIdDataRequest[_]): String   = employerRefFrom(r)

    def emailPublic(ua: UserAnswers, id: String): Option[String]        = emailfromUserAnswers(ua, id)
    def periodEndPublic(ua: UserAnswers, id: String): Option[LocalDate] = periodEndFromUserAnswers(ua, id)
    def employerRefPublic(r: DataRequest[_], id: String): String        = employerRefFrom(r, id)
  }

  private def uaEmpty: UserAnswers = UserAnswers("id", Json.obj())

  private def dataRequest(
    ua: UserAnswers,
    isAgent: Boolean,
    employerRef: Option[EmployerReference] = None,
    userId: String = "user-1"
  ): CisIdDataRequest[AnyContent] = {
    val dr = mock[CisIdDataRequest[AnyContent]]
    when(dr.userAnswers).thenReturn(ua)
    when(dr.isAgent).thenReturn(isAgent)
    when(dr.employerReference).thenReturn(employerRef)
    when(dr.userId).thenReturn(userId)
    when(dr.request).thenReturn(FakeRequest())
    dr
  }

  private def scopedDataRequest(
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

    "emailfromUserAnswers trims and filters empty values from EnterYourEmailAddressPage" in {
      val uaWithEmail = uaEmpty.set(EnterYourEmailAddressPage, "  test@test.com  ").get
      Harness.emailPublic(uaWithEmail) mustBe Some("test@test.com")
      Harness.emailPublic(uaWithEmail) mustBe Some("test@test.com")

      val uaBlank = uaEmpty.set(EnterYourEmailAddressPage, "   ").get
      Harness.emailPublic(uaBlank) mustBe None
      Harness.emailPublic(uaBlank) mustBe None
    }

    "periodEndFromUserAnswers uses correct page per return type" in {
      val date = LocalDate.of(2025, 9, 1)

      val ua = uaEmpty
        .set(DateConfirmPaymentsPage, date)
        .get

      Harness.periodEndPublic(ua) mustBe Some(date)
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

  "SubmissionViewDataSupport with id" should {

    "retrieve and trim email addresses from the correct ID-scoped journey" in {
      val ua = uaEmpty
        .setOrException(EnterYourEmailAddressPage("first-id"), "  first@test.com  ")
        .setOrException(EnterYourEmailAddressPage("second-id"), "  second@test.com  ")
        .setOrException(EnterYourEmailAddressPage, "legacy@test.com")

      Harness.emailPublic(ua, "first-id") mustBe Some("first@test.com")
      Harness.emailPublic(ua, "second-id") mustBe Some("second@test.com")
      Harness.emailPublic(ua, "third-id") mustBe None
      Harness.emailPublic(ua) mustBe Some("legacy@test.com")
    }

    "return None when the ID-scoped email address is blank" in {
      val ua = uaEmpty
        .setOrException(EnterYourEmailAddressPage("test-id"), "   ")
        .setOrException(EnterYourEmailAddressPage, "legacy@test.com")

      Harness.emailPublic(ua, "test-id") mustBe None
      Harness.emailPublic(ua) mustBe Some("legacy@test.com")
    }

    "retrieve the period end from the correct ID-scoped journey" in {
      val firstDate  = LocalDate.of(2025, 9, 1)
      val secondDate = LocalDate.of(2025, 10, 1)
      val legacyDate = LocalDate.of(2025, 11, 1)

      val ua = uaEmpty
        .setOrException(DateConfirmPaymentsPage("first-id"), firstDate)
        .setOrException(DateConfirmPaymentsPage("second-id"), secondDate)
        .setOrException(DateConfirmPaymentsPage, legacyDate)

      Harness.periodEndPublic(ua, "first-id") mustBe Some(firstDate)
      Harness.periodEndPublic(ua, "second-id") mustBe Some(secondDate)
      Harness.periodEndPublic(ua, "third-id") mustBe None
      Harness.periodEndPublic(ua) mustBe Some(legacyDate)
    }

    "use the employer reference from the request for an ID-scoped contractor journey" in {
      val request = scopedDataRequest(
        ua = uaEmpty,
        isAgent = false,
        employerRef = Some(EmployerReference("123", "AB456"))
      )

      Harness.employerRefPublic(request, "test-id") mustBe "123/AB456"

      val missingRequest = scopedDataRequest(
        ua = uaEmpty,
        isAgent = false
      )

      val ex = intercept[IllegalStateException] {
        Harness.employerRefPublic(missingRequest, "test-id")
      }

      ex.getMessage must include("employerReference missing")
    }

    "use agent client data from the correct ID-scoped journey" in {
      val firstAgent  = AgentClientData("CLIENT-1", "111", "ABC123", Some("First Scheme"))
      val secondAgent = AgentClientData("CLIENT-2", "222", "XYZ456", Some("Second Scheme"))
      val legacyAgent = AgentClientData("CLIENT-3", "999", "LEGACY", Some("Legacy Scheme"))

      val ua = uaEmpty
        .setOrException(AgentClientDataPage("first-id"), firstAgent)
        .setOrException(AgentClientDataPage("second-id"), secondAgent)
        .setOrException(AgentClientDataPage, legacyAgent)

      val request = scopedDataRequest(ua, isAgent = true)

      Harness.employerRefPublic(request, "first-id") mustBe "111/ABC123"
      Harness.employerRefPublic(request, "second-id") mustBe "222/XYZ456"

      val ex = intercept[IllegalStateException] {
        Harness.employerRefPublic(request, "third-id")
      }

      ex.getMessage must include("employerReference missing")
    }
  }
}
