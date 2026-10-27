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

import models.monthlyreturns.CisTaxpayer
import org.mongodb.scala.model.Filters
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import play.api.Configuration
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.time.temporal.ChronoUnit
import scala.concurrent.ExecutionContext

class CisTaxpayerCacheSpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[CisTaxpayerCache.Entity]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault())

  private val config = Configuration(
    "mongodb.timeToLiveInSeconds" -> 3600L
  )

  implicit val ec: ExecutionContext = ExecutionContext.global

  protected override val repository: CisTaxpayerCache = new CisTaxpayerCache(
    clock          = stubClock,
    config         = config,
    mongoComponent = mongoComponent
  )

  private val cisTaxpayer = CisTaxpayer(
    uniqueId          = "CIS-123",
    taxOfficeNumber   = "163",
    taxOfficeRef      = "AB0063",
    aoDistrict        = None,
    aoPayType         = None,
    aoCheckCode       = None,
    aoReference       = None,
    validBusinessAddr = None,
    correlation       = None,
    ggAgentId         = None,
    employerName1     = Some("ABC Construction Ltd"),
    employerName2     = None,
    agentOwnRef       = None,
    schemeName        = None,
    utr               = None,
    enrolledSig       = None
  )

  ".insert" - {

    "must insert a CIS taxpayer" in {
      val result = repository.insert(Seq(cisTaxpayer)).futureValue

      val storedRecord = find(Filters.equal("_id", cisTaxpayer.uniqueId))
        .futureValue
        .headOption
        .value

      result mustBe true
      storedRecord.cisId mustBe cisTaxpayer.uniqueId
      storedRecord.employerRef mustBe "163/AB0063"
      storedRecord.lastUsed mustBe instant
      storedRecord.data mustBe cisTaxpayer
    }
  }

  ".findBy(cisId)" - {

    "must return the taxpayer when it exists" in {
      repository.insert(Seq(cisTaxpayer)).futureValue

      val result = repository.findBy(cisTaxpayer.uniqueId).futureValue

      result mustBe Some(cisTaxpayer)
    }

    "must return None when the taxpayer does not exist" in {
      repository.findBy("unknown-cis-id").futureValue mustBe None
    }
  }

  ".findBy(ton, tor)" - {

    "must return the taxpayer when the employer reference matches" in {
      repository.insert(Seq(cisTaxpayer)).futureValue

      val result = repository
        .findBy(cisTaxpayer.taxOfficeNumber, cisTaxpayer.taxOfficeRef)
        .futureValue

      result mustBe Some(cisTaxpayer)
    }

    "must return None when the employer reference does not exist" in {
      repository.findBy("999", "UNKNOWN").futureValue mustBe None
    }
  }
}
