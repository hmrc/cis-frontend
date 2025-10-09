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
import models.UserAnswers
import models.monthlyreturns.{MonthlyReturn, MonthlyReturnEntity, MonthlyReturnResponse, NilMonthlyReturnRequest}
import pages.monthlyreturns.{CisIdPage, DateConfirmNilPaymentsPage, DeclarationPage, MonthlyReturnEntityPage}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

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
        logger.info("[resolveAndStoreCisId] cache-miss: fetching CIS taxpayer from backend")
        cisConnector.getCisTaxpayer().flatMap { tp =>
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

  def createNilMonthlyReturn(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    logger.info("[MonthlyReturnService] Starting monthly nil return creation process")

    for {
      cisId         <- getCisId(userAnswers)
      year          <- getTaxYear(userAnswers)
      month         <- getTaxMonth(userAnswers)
      monthlyReturn <- callBackendToCreate(cisId, year, month, userAnswers)
      updated       <- mirrorEntityToSession(userAnswers, monthlyReturn)
    } yield updated
  }

  private def getCisId(ua: UserAnswers): Future[String] =
    ua.get(CisIdPage) match {
      case Some(id) => Future.successful(id)
      case None     => Future.failed(new RuntimeException("CIS ID not found in session data"))
    }

  private def getTaxYear(ua: UserAnswers): Future[Int] =
    ua.get(DateConfirmNilPaymentsPage) match {
      case Some(date) => Future.successful(date.getYear)
      case None       => Future.failed(new RuntimeException("Date confirm nil payments not found in session data"))
    }

  private def getTaxMonth(ua: UserAnswers): Future[Int] =
    ua.get(DateConfirmNilPaymentsPage) match {
      case Some(date) => Future.successful(date.getMonthValue)
      case None       => Future.failed(new RuntimeException("Date confirm nil payments not found in session data"))
    }

  private def callBackendToCreate(cisId: String, year: Int, month: Int, ua: UserAnswers)(implicit
    hc: HeaderCarrier
  ): Future[MonthlyReturn] = {
    val payload = NilMonthlyReturnRequest(
      instanceId = cisId,
      taxYear = year,
      taxMonth = month,
      decEmpStatusConsidered = None,
      decInformationCorrect = ua.get(DeclarationPage).map(_.toString)
    )
    cisConnector.createNilMonthlyReturn(payload)
  }

  private def mirrorEntityToSession(ua: UserAnswers, monthlyReturn: MonthlyReturn): Future[UserAnswers] = {
    val now    = java.time.LocalDateTime.now()
    val entity = MonthlyReturnEntity(
      monthlyReturnId = monthlyReturn.monthlyReturnId,
      schemeId = 0L, // Not provided by backend
      taxYear = monthlyReturn.taxYear,
      taxMonth = monthlyReturn.taxMonth,
      taxYearPrevious = None,
      taxMonthPrevious = None,
      nilReturnIndicator = monthlyReturn.nilReturnIndicator.getOrElse("Y"),
      decNilReturnNoPayments = monthlyReturn.decNilReturnNoPayments.getOrElse("Y"),
      decInformationCorrect = monthlyReturn.decInformationCorrect.getOrElse("Y"),
      decNoMoreSubPayments = monthlyReturn.decNoMoreSubPayments.getOrElse("Y"),
      decAllSubsVerified = monthlyReturn.decAllSubsVerified.getOrElse("Y"),
      decEmpStatusConsidered = monthlyReturn.decEmpStatusConsidered.getOrElse("N"),
      status = monthlyReturn.status.getOrElse("STARTED"),
      createDate = now,
      lastUpdate = monthlyReturn.lastUpdate.getOrElse(now),
      version = 1, // Not provided by backend
      lMigrated = None,
      amendment = monthlyReturn.amendment.getOrElse("N"),
      supersededBy = monthlyReturn.supersededBy
    )

    ua.set(MonthlyReturnEntityPage, entity) match {
      case scala.util.Success(updated) => sessionRepository.set(updated).map(_ => updated)
      case scala.util.Failure(err)     => Future.failed(err)
    }
  }
}
