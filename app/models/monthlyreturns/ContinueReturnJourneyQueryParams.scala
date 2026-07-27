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

import play.api.mvc.QueryStringBindable

case class ContinueReturnJourneyQueryParams(
  instanceId: String,
  taxYear: Int,
  taxMonth: Int,
  isOriginalNilReturn: Option[Boolean] = None
)

object ContinueReturnJourneyQueryParams {
  implicit def queryStringBindable(implicit
    stringBinder: QueryStringBindable[String],
    intBinder: QueryStringBindable[Int],
    boolBinder: QueryStringBindable[Boolean]
  ): QueryStringBindable[ContinueReturnJourneyQueryParams] =
    new QueryStringBindable[ContinueReturnJourneyQueryParams] {

      override def bind(
        key: String,
        params: Map[String, Seq[String]]
      ): Option[Either[String, ContinueReturnJourneyQueryParams]] =
        for {
          instanceId <- stringBinder.bind("instanceId", params)
          taxYear    <- intBinder.bind("taxYear", params)
          taxMonth   <- intBinder.bind("taxMonth", params)
        } yield {
          val isOriginalNilReturn = boolBinder.bind("isOriginalNilReturn", params) match {
            case None            => Right(None)
            case Some(Right(v))  => Right(Some(v))
            case Some(Left(err)) => Left(err)
          }

          (instanceId, taxYear, taxMonth, isOriginalNilReturn) match {
            case (Right(instanceId), Right(taxYear), Right(taxMonth), Right(isOriginalNilReturn)) =>
              Right(ContinueReturnJourneyQueryParams(instanceId, taxYear, taxMonth, isOriginalNilReturn))
            case (_, _, _, Left(err))                                                             =>
              Left(err)
            case _                                                                                =>
              Left("Unable to bind ContinueReturnJourneyQueryParams")
          }
        }

      override def unbind(key: String, value: ContinueReturnJourneyQueryParams): String = {
        val base = stringBinder.unbind("instanceId", value.instanceId) + "&" +
          intBinder.unbind("taxYear", value.taxYear) + "&" +
          intBinder.unbind("taxMonth", value.taxMonth)
        value.isOriginalNilReturn match {
          case Some(v) => base + "&" + boolBinder.unbind("isOriginalNilReturn", v)
          case None    => base
        }
      }
    }
}
