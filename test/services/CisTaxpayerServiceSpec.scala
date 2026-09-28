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

import connectors.ConstructionIndustrySchemeConnector
import models.SimpleCisTaxpayer
import org.mockito.Mockito.never
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.DefaultAwaitTimeout
import repositories.CisTaxpayerCache

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class CisTaxpayerServiceSpec
    extends AnyFreeSpec
    with must.Matchers
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaFutures
    with DefaultAwaitTimeout
    with ScalaCheckPropertyChecks {
  import models.requests.IdentifierRequest
  import org.mockito.ArgumentMatchers.{any, eq as eqTo}
  import org.mockito.Mockito.{reset, verify, when}
  import play.api.test.FakeRequest

  private given ExecutionContext     = ExecutionContext.global
  private given IdentifierRequest[?] = IdentifierRequest(FakeRequest(), Random.nextString(16), None, None)

  private val mockConnector = mock[ConstructionIndustrySchemeConnector]
  private val mockCache     = mock[CisTaxpayerCache]

  private val serviceUnderTest = new CisTaxpayerService(mockConnector, mockCache)

  "method isClient must" - {
    "return same result as connector method hasClient when" - {
      "cache returns some CIS taxpayer" in forAll { (connectorHasClient: Boolean) =>
        val cisTaxpayer = randomCisTaxPayer
        when(mockCache.find(any)) thenReturn Future.successful(Some(cisTaxpayer))

        val connectorHasClient = Random.nextBoolean()
        when(mockConnector.hasClient(any, any)(any)) thenReturn Future.successful(connectorHasClient)

        val serviceHasClient = serviceUnderTest.isClient(cisTaxpayer.id).futureValue
        serviceHasClient mustBe connectorHasClient

        verify(mockCache).find(cisTaxpayer.id)
        verify(mockCache, never).insert(any)

        verify(mockConnector).hasClient(eqTo(cisTaxpayer.taxOfficeNumber), eqTo(cisTaxpayer.taxOfficeRef))(any)
        verify(mockConnector, never).getAllClients(any)
      }
    }

    "must return true when client list contains scheme ID and" - {
      "cache method find yields None" in {
        when(mockCache.find(any)) thenReturn Future.successful(None)
        when(mockCache.insert(any)) thenReturn Future.failed(new RuntimeException("Ouch"))

        val cisTaxpayer = randomCisTaxPayer
        when(mockConnector.getAllClients(any)) thenReturn Future.successful(List(cisTaxpayer))

        val serviceHasClient = serviceUnderTest.isClient(cisTaxpayer.id).futureValue
        serviceHasClient mustBe true

        verify(mockCache).find(cisTaxpayer.id)
        verify(mockCache).insert(List(cisTaxpayer))

        verify(mockConnector).getAllClients(any)
        verify(mockConnector, never).hasClient(any, any)(any)
      }
      "cache method 'find' throws error" in {
        when(mockCache.find(any)) thenReturn Future.failed(new RuntimeException("Oof"))
        when(mockCache.insert(any)) thenReturn Future.failed(new RuntimeException("Ouch"))

        val cisTaxpayer = randomCisTaxPayer
        when(mockConnector.getAllClients(any)) thenReturn Future.successful(List(cisTaxpayer))

        val serviceHasClient = serviceUnderTest.isClient(cisTaxpayer.id).futureValue
        serviceHasClient mustBe true

        verify(mockCache).find(cisTaxpayer.id)
        verify(mockCache).insert(List(cisTaxpayer))

        verify(mockConnector).getAllClients(any)
        verify(mockConnector, never).hasClient(any, any)(any)
      }
    }

    "must return false when client list does NOT contain scheme ID and" - {
      "cache method 'find' yields None" in {
        when(mockCache.find(any)) thenReturn Future.successful(None)
        when(mockCache.insert(any)) thenReturn Future.failed(new RuntimeException("Ouch"))

        when(mockConnector.getAllClients(any)) thenReturn Future.successful(List.empty)

        val schemeId         = randomSchemeId
        val serviceHasClient = serviceUnderTest.isClient(schemeId).futureValue
        serviceHasClient mustBe false

        verify(mockCache).find(schemeId)
        verify(mockCache).insert(List.empty)

        verify(mockConnector).getAllClients(any)
        verify(mockConnector, never).hasClient(any, any)(any)
      }
      "cache method 'find' throws error" in {
        when(mockCache.find(any)) thenReturn Future.failed(new RuntimeException("Oof"))
        when(mockCache.insert(any)) thenReturn Future.failed(new RuntimeException("Ouch"))

        when(mockConnector.getAllClients(any)) thenReturn Future.successful(List.empty)

        val schemeId         = randomSchemeId
        val serviceHasClient = serviceUnderTest.isClient(schemeId).futureValue
        serviceHasClient mustBe false

        verify(mockCache).find(schemeId)
        verify(mockCache).insert(List.empty)

        verify(mockConnector).getAllClients(any)
        verify(mockConnector, never).hasClient(any, any)(any)
      }
    }
  }

  override protected def afterEach(): Unit = reset(mockCache, mockConnector)

  private def randomCisTaxPayer = SimpleCisTaxpayer(
    id = randomSchemeId,
    taxOfficeNumber = Random.nextString(3),
    taxOfficeRef = Random.nextString(7),
    agentOwnRef = None,
    schemeName = None,
    utr = None
  )

  private def randomSchemeId = Random.nextString(5)
}
