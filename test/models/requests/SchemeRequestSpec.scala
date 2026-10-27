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

package models.requests

import base.SpecBase
import models.monthlyreturns.CisTaxpayer
import models.requests.CisPath.{CisId, CisOrg}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContent

class SchemeRequestSpec extends SpecBase with MockitoSugar {

  private val cisId = "123456789"

  private def cisTaxpayer: CisTaxpayer = {
    val taxpayer = mock[CisTaxpayer]

    when(taxpayer.uniqueId).thenReturn(cisId)
    when(taxpayer.taxOfficeNumber).thenReturn("163")
    when(taxpayer.taxOfficeRef).thenReturn("AB0063")

    taxpayer
  }

  "SchemeRequest" - {

    "return CisOrg for an organisation user" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer

      when(identifier.isAgent).thenReturn(false)

      val request = new SchemeRequest(identifier, taxpayer)

      request.cisPath mustBe CisOrg
    }

    "return CisId for an agent user" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer

      when(identifier.isAgent).thenReturn(true)

      val request = new SchemeRequest(identifier, taxpayer)

      request.cisPath mustBe CisId(cisId)
    }

    "return the CIS ID from the taxpayer" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer

      val request = new SchemeRequest(identifier, taxpayer)

      request.cisId mustBe cisId
    }

    "return the employer reference" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer

      val request = new SchemeRequest(identifier, taxpayer)

      request.employerRef mustBe "163/AB0063"
    }

    "retain the identifier and taxpayer" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer

      val request = new SchemeRequest(identifier, taxpayer)

      request.identifier mustBe identifier
      request.cisTaxpayer mustBe taxpayer
    }
  }

  "JourneyRequest" - {

    "retain the scheme information and scoped user answers" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer
      val scheme     = new SchemeRequest(identifier, taxpayer)

      val subJourneyId = "org_12345678-1234-1234-1234-123456789abc"
      val userAnswers  = emptyUserAnswers

      val request = new JourneyRequest(scheme, subJourneyId, userAnswers)

      request.identifier mustBe identifier
      request.cisTaxpayer mustBe taxpayer
      request.cisId mustBe cisId
      request.cisPath mustBe CisOrg
      request.subJourneyId mustBe subJourneyId
      request.userAnswers mustBe userAnswers
    }

    "preserve the agent CIS path" in {
      val identifier = mock[IdentifierRequest[AnyContent]]
      val taxpayer   = cisTaxpayer

      when(identifier.isAgent).thenReturn(true)

      val scheme = new SchemeRequest(identifier, taxpayer)

      val request = new JourneyRequest(
        scheme,
        "123456789_12345678-1234-1234-1234-123456789abc",
        emptyUserAnswers
      )

      request.cisPath mustBe CisId(cisId)
    }
  }
}
