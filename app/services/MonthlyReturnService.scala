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
import models.monthlyreturns.*
import pages.monthlyreturns.*
import models.{ReturnType, UserAnswers}
import models.agent.AgentClientData
import models.requests.GetMonthlyReturnForEditRequest
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.http.HeaderCarrier
import viewmodels.SelectSubcontractorsViewModel

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class MonthlyReturnService @Inject() (
  cisConnector: ConstructionIndustrySchemeConnector,
  sessionRepository: SessionRepository
)(implicit ec: ExecutionContext)
    extends Logging {

  def resolveAndStoreCisId(ua: UserAnswers, isAgent: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[(String, UserAnswers)] =
    ua.get(CisIdPage) match {
      case Some(cisId) => Future.successful((cisId, ua))

      case None if isAgent =>
        Future.failed(new RuntimeException("Missing cisId for agent journey"))

      case None =>
        logger.info("[resolveAndStoreCisId] cache-miss: fetching CIS taxpayer from backend")
        cisConnector.getCisTaxpayer().flatMap { tp =>
          logger.info(s"[resolveAndStoreCisId] taxpayer payload:\n${Json.prettyPrint(Json.toJson(tp))}")
          val cisId = tp.uniqueId.trim
          if (cisId.isEmpty) {
            Future.failed(new RuntimeException("Empty cisId (uniqueId) returned from /cis/taxpayer"))
          } else {
            val contractorName = tp.schemeName.getOrElse("")
            for {
              updatedUaWithCisId      <- Future.fromTry(ua.set(CisIdPage, cisId))
              updatedUaWithContractor <- Future.fromTry(updatedUaWithCisId.set(ContractorNamePage, contractorName))
              _                       <- sessionRepository.set(updatedUaWithContractor)
            } yield (cisId, updatedUaWithContractor)
          }
        }
    }

  def retrieveAllMonthlyReturns(cisId: String)(implicit hc: HeaderCarrier): Future[MonthlyReturnResponse] =
    cisConnector.retrieveMonthlyReturns(cisId)

  def retrieveMonthlyReturnForEditDetails(
    instanceId: String,
    taxMonth: Int,
    taxYear: Int
  )(implicit
    hc: HeaderCarrier
  ): Future[GetAllMonthlyReturnDetailsResponse] =
    cisConnector.retrieveMonthlyReturnForEditDetails(instanceId, taxMonth, taxYear)

  def getSchemeEmail(cisId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    cisConnector.getSchemeEmail(cisId)

  def isDuplicate(cisId: String, year: Int, month: Int)(implicit hc: HeaderCarrier): Future[Boolean] =
    retrieveAllMonthlyReturns(cisId).map { res =>
      res.monthlyReturnList.exists(mr => mr.taxYear == year && mr.taxMonth == month)
    }

  def hasClient(taxOfficenumber: String, taxOfficeReference: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    cisConnector.hasClient(taxOfficenumber, taxOfficeReference)

  def getAgentClient(
    userId: String
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[AgentClientData]] =
    cisConnector.getAgentClient(userId).map {
      case Some(jsValue) =>
        jsValue.validate[AgentClientData] match {
          case JsSuccess(agentClientData, _) => Some(agentClientData)
          case JsError(_)                    => None
        }
      case None          => None
    }

  def createNilMonthlyReturn(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[UserAnswers] = {
    logger.info("[MonthlyReturnService] Starting FormP monthly nil return creation process")

    for {
      cisId         <- getCisId(userAnswers)
      year          <- getTaxYear(userAnswers)
      month         <- getTaxMonth(userAnswers)
      infoCorrect   <- getInfoCorrectOrDefault(userAnswers)
      nilNoPayments <- getNilNoPaymentsOrDefault(userAnswers)
      resp          <- callBackendToCreate(cisId, year, month, infoCorrect, nilNoPayments)
      saved         <- persistStatus(userAnswers, resp.status)
    } yield saved
  }

  def updateMonthlyReturn(request: UpdateMonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    cisConnector.updateMonthlyReturn(request)

  def createMonthlyReturn(request: MonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    cisConnector.createMonthlyReturn(request)

  def updateMonthlyReturnItem(request: UpdateMonthlyReturnItemRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    cisConnector.updateMonthlyReturnItem(request)

  def syncMonthlyReturnItems(
    instanceId: String,
    taxYear: Int,
    taxMonth: Int,
    selectedSubcontractorIds: Seq[Long]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    cisConnector.syncMonthlyReturnItems(
      SelectedSubcontractorsRequest(instanceId, taxYear, taxMonth, selectedSubcontractorIds)
    )

  def deleteMonthlyReturnItem(payload: DeleteMonthlyReturnItemRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    cisConnector.deleteMonthlyReturnItem(payload)

  def storeAndSyncSelectedSubcontractors(
    ua: UserAnswers,
    cisId: String,
    taxYear: Int,
    taxMonth: Int,
    selected: Seq[SelectSubcontractorsViewModel]
  )(implicit hc: HeaderCarrier): Future[UserAnswers] = {

    val selectedIds: Seq[Long]         = selected.map(_.id)
    val existingSelectedSubcontractors =
      ua.get(SelectedSubcontractorPage.all).getOrElse(Map.empty)
    val cleared: Try[UserAnswers]      = ua.remove(SelectedSubcontractorPage.all)
    val updatedTry: Try[UserAnswers]   =
      selected.zipWithIndex
        .foldLeft(cleared) { case (uaTry, (vm, index)) =>
          uaTry.flatMap { answers =>
            val existing = existingSelectedSubcontractors.values.find(_.id == vm.id)
            answers.set(
              SelectedSubcontractorPage(index + 1),
              SelectedSubcontractor(
                vm.id,
                vm.name,
                existing.flatMap(_.totalPaymentsMade),
                existing.flatMap(_.costOfMaterials),
                existing.flatMap(_.totalTaxDeducted)
              )
            )
          }
        }
        .flatMap(_.remove(VerifySubcontractorsPage))

    Future
      .fromTry(updatedTry)
      .flatMap { updatedUa =>
        sessionRepository.set(updatedUa).flatMap {
          case false =>
            Future.failed(new RuntimeException("Failed to persist selected subcontractors in session"))
          case true  =>
            cisConnector
              .syncMonthlyReturnItems(
                SelectedSubcontractorsRequest(
                  instanceId = cisId,
                  taxYear = taxYear,
                  taxMonth = taxMonth,
                  selectedSubcontractorIds = selectedIds
                )
              )
              .map(_ => updatedUa)
        }
      }
  }

  def populateUserAnswersForContinueJourney(
    ua: UserAnswers,
    editRequest: GetMonthlyReturnForEditRequest
  )(implicit hc: HeaderCarrier): Future[Either[String, UserAnswers]] =
    retrieveMonthlyReturnForEditDetails(
      instanceId = editRequest.instanceId,
      taxMonth = editRequest.taxMonth,
      taxYear = editRequest.taxYear
    ).map { response =>
      for {
        monthlyReturn <- response.monthlyReturn.headOption.toRight("Missing monthly return")
        updatedUa     <- populateContinueJourneyAnswers(
                           ua = ua,
                           instanceId = editRequest.instanceId,
                           monthlyReturn = monthlyReturn,
                           monthlyReturnItems = response.monthlyReturnItems,
                           submissions = response.submission
                         )
      } yield updatedUa
    }

  private def populateContinueJourneyAnswers(
    ua: UserAnswers,
    instanceId: String,
    monthlyReturn: MonthlyReturn,
    monthlyReturnItems: Seq[MonthlyReturnItem],
    submissions: Seq[Submission]
  ): Either[String, UserAnswers] = {
    val emailRecipient = submissions.headOption.flatMap(_.emailRecipient)

    monthlyReturn.nilReturnIndicator match {
      case Some("Y") =>
        populateNilReturnAnswers(
          ua = ua,
          instanceId = instanceId,
          monthlyReturn = monthlyReturn,
          emailRecipient = emailRecipient
        )

      case Some("N") =>
        populateStandardReturnAnswers(
          ua = ua,
          instanceId = instanceId,
          monthlyReturn = monthlyReturn,
          monthlyReturnItems = monthlyReturnItems,
          emailRecipient = emailRecipient
        )

      case _ =>
        Left("Missing nil return indicator")
    }
  }

  private def populateNilReturnAnswers(
    ua: UserAnswers,
    instanceId: String,
    monthlyReturn: MonthlyReturn,
    emailRecipient: Option[String]
  ): Either[String, UserAnswers] = {
    val declarationSet =
      if (monthlyReturn.decInformationCorrect.contains("Y")) Set(Declaration.Confirmed) else Set.empty[Declaration]

    for {
      ua1 <- ua.set(CisIdPage, instanceId).toEither.left.map(_.getMessage)
      ua2 <- ua1.set(ReturnTypePage, ReturnType.MonthlyNilReturn).toEither.left.map(_.getMessage)
      ua3 <- ua2.set(
                 DateConfirmPaymentsPage,
                 LocalDate.of(monthlyReturn.taxYear, monthlyReturn.taxMonth, 5)
               )
               .toEither.left.map(_.getMessage)
      ua4 <- ua3.set(DeclarationPage, declarationSet).toEither.left.map(_.getMessage)
      ua5 <- ua4.set(
                 SubmitInactivityRequestPage,
                 monthlyReturn.decNilReturnNoPayments.contains("Y")
               )
               .toEither.left.map(_.getMessage)
      ua6 <- emailRecipient match {
               case Some(email) if email.nonEmpty =>
                 ua5.set(EnterYourEmailAddressPage, email).toEither.left.map(_.getMessage)
               case None                          => Right(ua5)
             }
    } yield ua6
  }

  private def populateStandardReturnAnswers(
    ua: UserAnswers,
    instanceId: String,
    monthlyReturn: MonthlyReturn,
    monthlyReturnItems: Seq[MonthlyReturnItem],
    emailRecipient: Option[String]
  ): Either[String, UserAnswers] =
    for {
      ua1 <- ua.set(CisIdPage, instanceId).toEither.left.map(_.getMessage)
      ua2 <- ua1.set(ReturnTypePage, ReturnType.MonthlyStandardReturn).toEither.left.map(_.getMessage)
      ua3 <- ua2.set(
                 DateConfirmPaymentsPage,
                 LocalDate.of(monthlyReturn.taxYear, monthlyReturn.taxMonth, 5)
               )
               .toEither.left.map(_.getMessage)
      ua4 <- ua3.set(
                 EmploymentStatusDeclarationPage,
                 monthlyReturn.decEmpStatusConsidered.contains("Y")
               )
               .toEither.left.map(_.getMessage)
      ua5 <- ua4.set(
                 VerifiedStatusDeclarationPage,
                 monthlyReturn.decAllSubsVerified.contains("Y")
               )
               .toEither.left.map(_.getMessage)
      ua6 <- ua5.set(
                 DeclarationPage,
                 if (monthlyReturn.decInformationCorrect.contains("Y")) {
                   Set(Declaration.Confirmed)
                 } else {
                   Set.empty[Declaration]
                 }
               )
               .toEither.left.map(_.getMessage)
      ua7 <- ua6.set(
                 SubmitInactivityRequestPage,
                 monthlyReturn.decNilReturnNoPayments.contains("Y")
               )
               .toEither.left.map(_.getMessage)
      ua8 <- populateStandardReturnItems(ua7, monthlyReturnItems)
      ua9 <- emailRecipient match {
               case Some(email) if email.nonEmpty =>
                 ua3.set(EnterYourEmailAddressPage, email).toEither.left.map(_.getMessage)
               case None                          =>
                 Right(ua8)
             }
    } yield ua9

  private def populateStandardReturnItems(
    ua: UserAnswers,
    items: Seq[MonthlyReturnItem]
  ): Either[String, UserAnswers] =
    for {
      cleared <- ua.remove(SelectedSubcontractorPage.all).toEither.left.map(_.getMessage)
      updated <- items.zipWithIndex.foldLeft[Either[String, UserAnswers]](Right(cleared)) {
                   case (accEither, (item, index)) =>
                     for {
                       acc  <- accEither
                       next <- acc
                                 .set(
                                   SelectedSubcontractorPage(index + 1),
                                   SelectedSubcontractor(
                                     id = item.subcontractorId.getOrElse(0L),
                                     name = item.subcontractorName.getOrElse(""),
                                     totalPaymentsMade = toBigDecimal(item.totalPayments),
                                     costOfMaterials = toBigDecimal(item.costOfMaterials),
                                     totalTaxDeducted = toBigDecimal(item.totalDeducted)
                                   )
                                 )
                                 .toEither.left.map(_.getMessage)
                     } yield next
                 }
    } yield updated

  private def toBigDecimal(value: Option[String]): Option[BigDecimal] =
    value.flatMap(v => Try(BigDecimal(v)).toOption)

  private def getCisId(ua: UserAnswers): Future[String] =
    ua.get(CisIdPage) match {
      case Some(id) => Future.successful(id)
      case None     => Future.failed(new RuntimeException("CIS ID not found in session data"))
    }

  private def getTaxYear(ua: UserAnswers): Future[Int] =
    ua.get(DateConfirmPaymentsPage) match {
      case Some(date) => Future.successful(date.getYear)
      case None       => Future.failed(new RuntimeException("Date confirm nil payments not found in session data"))
    }

  private def getTaxMonth(ua: UserAnswers): Future[Int] =
    ua.get(DateConfirmPaymentsPage) match {
      case Some(date) => Future.successful(date.getMonthValue)
      case None       => Future.failed(new RuntimeException("Date confirm nil payments not found in session data"))
    }

  private def getInfoCorrectOrDefault(ua: UserAnswers): Future[String] =
    ua.get(DeclarationPage) match {
      case Some(selections: Set[Declaration]) if selections.contains(Declaration.Confirmed) =>
        Future.successful("Y")
      case _                                                                                =>
        Future.successful("N")
    }

  private def getNilNoPaymentsOrDefault(ua: UserAnswers): Future[String] =
    ua.get(InactivityRequestPage) match {
      case Some(ir) => Future.successful(mapInactivityRequestToYN(ir))
      case None     => Future.successful("N")
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
