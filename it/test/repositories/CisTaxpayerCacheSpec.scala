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

package repositories

import models.CisTaxpayerSearchResult
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext

class CisTaxpayerCacheSpec
    extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[CisTaxpayerCache.Entity]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with TableDrivenPropertyChecks {
  import play.api.Configuration

  private given ExecutionContext = ExecutionContext.global

  private val UNIX_EPOCH = Instant parse "1970-01-01T00:00:00Z"
  private val fakeClock  = Clock.fixed(UNIX_EPOCH, ZoneId.systemDefault())
  private val fakeConfig = Configuration from Map("mongodb.timeToLiveInSeconds" -> 900)

  protected val repository: CisTaxpayerCache = new CisTaxpayerCache(fakeClock, fakeConfig, mongoComponent)

  private val expectedCisTaxpayers = Table(
    "CIS ID" -> "CIS Taxpayer",
    "1111"   -> buildCisTaxpayer("1111", "111", "AA11111"),
    "1234"   -> buildCisTaxpayer("1234", "123", "AB12345"),
    "4321"   -> buildCisTaxpayer("4321", "321", "BA54321")
  )

  private val givenCisTaxPayers = for (_, cisTaxpayer) <- expectedCisTaxpayers yield cisTaxpayer

  "CIS taxpayer cache should" - {
    "acknowledge insertions and find employer refs for all inserted CIS IDs" in {
      val insertWasAcknowledged = repository.insert(givenCisTaxPayers).futureValue

      assert(insertWasAcknowledged)

      forAll(expectedCisTaxpayers) { (cisId, expectedEmployerRef) =>
        val retrievedEmployerRef = repository.find(cisId).futureValue.value

        retrievedEmployerRef mustBe expectedEmployerRef
      }
    }
  }

  private def buildCisTaxpayer(cisId: String, ton: String, tor: String) =
    CisTaxpayerSearchResult(cisId, ton, tor, None, None, None)
}
