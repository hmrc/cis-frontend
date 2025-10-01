/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.Logging
import connectors.ConstructionIndustrySchemeConnector
import repositories.SessionRepository
import models.{ChrisSubmissionRequest, UserAnswers}
import models.monthlyreturns.{InactivityRequest, MonthlyReturnResponse}
import pages.monthlyreturns.CisIdPage
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import java.time.YearMonth
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MonthlyReturnService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def resolveAndStoreCisId(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[(String, UserAnswers)] =
    ua.get(CisIdPage) match {
      case Some(cisId) => Future.successful((cisId, ua))
      case None        =>
        // temporarily added for testing
        logger.info("[resolveAndStoreCisId] cache-miss: fetching CIS taxpayer from backend")
        cisConnector.getCisTaxpayer().flatMap { tp =>
          // temporarily added for testing
          logger.info(s"[resolveAndStoreCisId] taxpayer payload:\n${Json.prettyPrint(Json.toJson(tp))}")
          val cisId = tp.uniqueId.trim
          if (cisId.isEmpty) {
            Future.failed(new RuntimeException("Empty cisId (uniqueId) returned from /cis/taxpayer"))
          } else {
            ua.set(CisIdPage, cisId)
              .fold(
                err => Future.failed(err),
                updatedUa => sessionRepository.set(updatedUa).map(_ => (cisId, updatedUa))
              )
          }
        }
    }

  def retrieveAllMonthlyReturns(cisId: String)(implicit hc: HeaderCarrier): Future[MonthlyReturnResponse] =
    cisConnector.retrieveMonthlyReturns(cisId)

  def isDuplicate(cisId: String, year: Int, month: Int)(implicit hc: HeaderCarrier): Future[Boolean] =
    retrieveAllMonthlyReturns(cisId).map { res =>
      res.monthlyReturnList.exists(mr => mr.taxYear == year && mr.taxMonth == month)
    }

  def submitNilMonthlyReturn(ua: UserAnswers)(implicit hc: HeaderCarrier): Future[Boolean] = {
    for {
      tp          <- cisConnector.getCisTaxpayer()
      utr         <- valueOrFail(tp.utr.map(_.trim).filter(_.nonEmpty), "CIS taxpayer utr was empty/missing from /cis/taxpayer")
      aoReference <- valueOrFail(
                       tp.aoReference.map(_.trim).filter(_.nonEmpty),
                       "CIS taxpayer AOref was empty/missing from /cis/taxpayer"
                     )

      inactivityB <- valueOrFail(readInactivityBool(ua), "InactivityRequest was not answered")
      periodYm    <- valueOrFail(readMonthYearYm(ua), "Month/Year was not answered")

      dto = ChrisSubmissionRequest.from(
              utr = utr,
              aoReference = aoReference,
              informationCorrect = true,
              inactivity = inactivityB,
              period = periodYm
            )

      _ = logger.info(s"[submitNilMonthlyReturn] payload=${Json.stringify(Json.toJson(dto))}")

      ok <- cisConnector.submitChris(dto)
    } yield ok
  }.recover { case t =>
    logger.error("[submitNilMonthlyReturn] building/submitting payload failed", t)
    false
  }

  private def valueOrFail[A](opt: Option[A], err: => String): Future[A] =
    opt match {
      case Some(v) => Future.successful(v)
      case None    => Future.failed(new RuntimeException(err))
    }

  private def readInactivityBool(ua: UserAnswers): Option[Boolean] =
    ua.get(pages.monthlyreturns.InactivityRequestPage).map {
      case InactivityRequest.Option1 => true
      case InactivityRequest.Option2 => false
    }

  private def readMonthYearYm(ua: UserAnswers): Option[YearMonth] =
    ua.get(pages.monthlyreturns.DateConfirmNilPaymentsPage).map(YearMonth.from)
}
