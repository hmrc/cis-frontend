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

package services

import base.SpecBase
import connectors.ConstructionIndustrySchemeConnector
import models.EmployerReference
import models.monthlyreturns.CisTaxpayer
import models.requests.IdentifierRequest
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.GET
import repositories.CisTaxpayerCache
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class CisTaxpayerServiceSpec extends SpecBase with MockitoSugar {

  private given ExecutionContext = ExecutionContext.global

  private val cisId = "123456789"

  private def taxpayer(id: String = cisId): CisTaxpayer = {
    val result = mock[CisTaxpayer]

    when(result.uniqueId).thenReturn(id)
    when(result.taxOfficeNumber).thenReturn("163")
    when(result.taxOfficeRef).thenReturn("AB0063")

    result
  }

  private def identifierRequest: IdentifierRequest[AnyContentAsEmpty.type] =
    IdentifierRequest(
      FakeRequest(GET, "/"),
      userAnswersId,
      Some(EmployerReference("163", "AB0063")),
      None
    )

  "CisTaxpayerService" - {

    "return a cached taxpayer for an organisation" in {
      given IdentifierRequest[AnyContentAsEmpty.type] = identifierRequest

      val connector = mock[ConstructionIndustrySchemeConnector]
      val cache     = mock[CisTaxpayerCache]
      val data      = taxpayer()
      val service   = new CisTaxpayerService(connector, cache)

      when(cache.findBy("163", "AB0063"))
        .thenReturn(Future.successful(Some(data)))

      service.findForOrganisation.futureValue mustBe data

      verifyNoInteractions(connector)
    }

    "fetch the taxpayer from the backend when the organisation cache misses" in {
      given IdentifierRequest[AnyContentAsEmpty.type] = identifierRequest

      val connector = mock[ConstructionIndustrySchemeConnector]
      val cache     = mock[CisTaxpayerCache]
      val data      = taxpayer()
      val service   = new CisTaxpayerService(connector, cache)

      when(cache.findBy("163", "AB0063"))
        .thenReturn(Future.successful(None))

      when(connector.getCisTaxpayer()(using any[HeaderCarrier]))
        .thenReturn(Future.successful(data))

      when(cache.insert(any[Seq[CisTaxpayer]]))
        .thenReturn(Future.successful(true))

      service.findForOrganisation.futureValue mustBe data

      verify(connector).getCisTaxpayer()(using any[HeaderCarrier])
      verify(cache).insert(Seq(data))
    }

    "return None when a cached agent client is no longer authorised" in {
      given IdentifierRequest[AnyContentAsEmpty.type] = identifierRequest

      val connector = mock[ConstructionIndustrySchemeConnector]
      val cache     = mock[CisTaxpayerCache]
      val data      = taxpayer()
      val service   = new CisTaxpayerService(connector, cache)

      when(cache.findBy(cisId))
        .thenReturn(Future.successful(Some(data)))

      when(connector.hasClient(any[String], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(false))

      service.findForAgent(cisId).futureValue mustBe None

      verify(connector).hasClient(any[String], any[String])(using any[HeaderCarrier])
    }

    "find an agent client in the client list when the cache misses" in {
      given IdentifierRequest[AnyContentAsEmpty.type] = identifierRequest

      val connector = mock[ConstructionIndustrySchemeConnector]
      val cache     = mock[CisTaxpayerCache]
      val data      = taxpayer()
      val other     = taxpayer("987654321")
      val clients   = List(other, data)
      val service   = new CisTaxpayerService(connector, cache)

      when(cache.findBy(cisId))
        .thenReturn(Future.successful(None))

      when(connector.getAllClients(using any[HeaderCarrier]))
        .thenReturn(Future.successful(clients))

      when(cache.insert(any[Seq[CisTaxpayer]]))
        .thenReturn(Future.successful(true))

      service.findForAgent(cisId).futureValue mustBe Some(data)

      verify(connector).getAllClients(using any[HeaderCarrier])
      verify(cache).insert(clients)
    }
  }
}
