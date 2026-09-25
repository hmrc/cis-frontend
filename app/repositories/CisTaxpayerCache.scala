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

import com.google.inject.{Inject, Singleton}
import com.mongodb.client.model.*
import models.CisTaxpayerSearchResult
import org.mongodb.scala.model.Filters
import play.api.Configuration
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{OFormat, __}
import repositories.CisTaxpayerCache.*
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Clock, Instant}
import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CisTaxpayerCache @Inject() (
  clock: Clock,
  config: Configuration,
  mongoComponent: MongoComponent
)(using ExecutionContext)
    extends PlayMongoRepository[Entity](
      mongoComponent,
      "cisTaxpayers",
      entityFormat,
      Seq(
        IndexModel(
          Indexes.ascending(LAST_USED),
          IndexOptions().expireAfter(config.get[Long]("mongodb.timeToLiveInSeconds"), TimeUnit.SECONDS)
        )
      )
    ) {

  def insert(cisTaxpayers: Seq[CisTaxpayerSearchResult]): Future[Boolean] =
    val now      = clock.instant()
    val entities = for taxpayer <- cisTaxpayers yield Entity(taxpayer.uniqueId, now, taxpayer)

    collection
      .insertMany(entities)
      .toFuture()
      .map(_.wasAcknowledged())

  def find(cisId: String): Future[Option[CisTaxpayerSearchResult]] =
    val now = clock.instant()

    collection
      .findOneAndUpdate(
        Filters.eq(ID, cisId),
        Updates.set(LAST_USED, now)
      )
      .toFutureOption()
      .map(_.map(_.data))
}

private object CisTaxpayerCache extends MongoJavatimeFormats {
  private val ID        = "_id"
  private val LAST_USED = "lastUsed"
  private val DATA      = "data"

  final case class Entity(cisId: String, lastUsed: Instant, data: CisTaxpayerSearchResult)

  private val entityFormat: OFormat[Entity] = (
    (__ \ ID).format[String] ~
      (__ \ LAST_USED).format(instantFormat) ~
      (__ \ DATA).format[CisTaxpayerSearchResult]
  )(Entity.apply, entity => (entity.cisId, entity.lastUsed, entity.data))
}
