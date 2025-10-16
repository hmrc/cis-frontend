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
import models.monthlyreturns.{Declaration, NilMonthlyReturnRequest, NilMonthlyReturnResponse}
import pages.monthlyreturns.{DateConfirmNilPaymentsPage, DeclarationPage, InactivityRequestPage, NilReturnStatusPage}
import models.UserAnswers
import models.monthlyreturns.{InactivityRequest, MonthlyReturnResponse}
import pages.monthlyreturns.CisIdPage
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
    logger.info("[MonthlyReturnService] Starting FormP monthly nil return creation process")

    for {
      cisId         <- getCisId(userAnswers)
      year          <- getTaxYear(userAnswers)
      month         <- getTaxMonth(userAnswers)
      infoCorrect   <- getInfoCorrect(userAnswers)
      nilNoPayments <- getNilNoPayments(userAnswers)
      resp          <- callBackendToCreate(cisId, year, month, infoCorrect, nilNoPayments)
      saved         <- persistStatus(userAnswers, resp.status)
    } yield saved
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

  private def getInfoCorrect(ua: UserAnswers): Future[String] =
    ua.get(DeclarationPage) match {
      case Some(selections: Set[Declaration]) =>
        if (selections.contains(Declaration.Confirmed)) { Future.successful("Y") }
        else { Future.failed(new IllegalArgumentException("Declaration must be confirmed")) }
      case None                               =>
        Future.failed(new IllegalArgumentException("declaration missing"))
    }

  private def getNilNoPayments(ua: UserAnswers): Future[String] =
    ua.get(InactivityRequestPage) match {
      case Some(ir) => Future.successful(mapInactivityRequestToYN(ir))
      case None     => Future.failed(new IllegalArgumentException("C2 (InactivityRequest) missing"))
    }

  private def mapInactivityRequestToYN(ir: InactivityRequest): String = ir match {
    case InactivityRequest.Option1 => "Y"
    case InactivityRequest.Option2 => "N"
  }

  private def callBackendToCreate(
    cisId: String,
    year: Int,
    month: Int,
    infoCorrect: String,
    nilNoPayments: String
  )(implicit hc: HeaderCarrier): Future[NilMonthlyReturnResponse] = {
    val payload = NilMonthlyReturnRequest(
      instanceId = cisId,
      taxYear = year,
      taxMonth = month,
      decInformationCorrect = infoCorrect,
      decNilReturnNoPayments = nilNoPayments
    )

    logger.info(s"[MonthlyReturnService] Calling BE  to create FormP monthly nil return for $cisId $year/$month")
    cisConnector.createNilMonthlyReturn(payload).andThen {
      case scala.util.Success(r) =>
        logger.info(s"[MonthlyReturnService] FormP monthly nil return creation completed successfully")
      case scala.util.Failure(e) => logger.error("[MonthlyReturnService] BE call failed", e)
    }
  }

  private def persistStatus(ua: UserAnswers, status: String): Future[UserAnswers] =
    ua.set(NilReturnStatusPage, status) match {
      case scala.util.Success(updated) => sessionRepository.set(updated).map(_ => updated)
      case scala.util.Failure(err)     => Future.failed(err)
    }
}
