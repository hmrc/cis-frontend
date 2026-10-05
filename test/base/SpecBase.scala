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

package base

import config.FrontendAppConfig
import controllers.actions.*
import models.monthlyreturns.CisTaxpayer
import models.requests.CisPath.CisOrg
import models.{JourneyId, UserAnswers}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.{Binding, bind}
import play.api.libs.json.Json
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.stubControllerComponents
import repositories.SessionRepository
import services.{FakeFormpRdsReconcileService, FormpRdsReconcileService}

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with GuiceOneAppPerSuite
    with IntegrationPatience {

  implicit lazy val applicationConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  protected val UNIX_EPOCH: Instant = Instant ofEpochMilli 0

  protected val taxOfficeNum = "123"
  protected val taxOfficeRef = "AB1234"
  protected val employerRef  = s"$taxOfficeNum/$taxOfficeRef"
  protected val cisTaxpayer  =
    CisTaxpayer(
      "12345",
      taxOfficeNum,
      taxOfficeRef,
      Some("AO District"),
      Some("AO Pay Type"),
      Some("AO Check Code"),
      Some("AO Reference"),
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None,
      None
    )

  val userAnswersId: String    = "id" // Used for legacy UserAnswers objects which aren't keyed to a particular journey
  val journeyId: String        = JourneyId("some_user_id", "MonthlyReturn", CisOrg.toUrl).asString
  val parsers: PlayBodyParsers = stubControllerComponents().parsers

  def emptyUserAnswers: UserAnswers     = UserAnswers(journeyId, lastUpdated = UNIX_EPOCH)
  def userAnswersWithCisId: UserAnswers = UserAnswers(journeyId, Json.obj("cisId" -> "1"), lastUpdated = UNIX_EPOCH)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def mockSessionRepository(userAnswers: Option[UserAnswers]): SessionRepository = {
    val repository = mock[SessionRepository]
    when(repository.get(any[String])).thenReturn(Future.successful(userAnswers))
    repository
  }

  protected def applicationBuilder(
    userAnswers: Option[UserAnswers] = None,
    additionalBindings: Seq[Binding[_]] = Nil,
    isAgent: Boolean = false,
    hasAgentRef: Boolean = true,
    hasEmployeeRef: Boolean = true,
    formpRdsReconcileService: FormpRdsReconcileService = new FakeFormpRdsReconcileService,
    agentCode: Option[String] = Some("agentCode"),
    schemeAction: SchemeAction = new FakeSchemeAction(cisTaxpayer)(using ExecutionContext.global),
    clock: Clock = Clock.systemDefaultZone()
  ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure("play.http.router" -> "app.Routes")
      .overrides(
        Seq(
          bind[Clock].toInstance(clock),
          bind[DataRequiredAction].to[DataRequiredActionImpl],
          bind[IdentifierAction].to(new FakeIdentifierAction(isAgent, hasAgentRef, hasEmployeeRef)(parsers)),
          bind[IdentifierAction]
            .qualifiedWith("AgentIdentifier")
            .to(new FakeIdentifierAction(true, true, false, agentCode)(parsers)),
          bind[IdentifierAction]
            .qualifiedWith("ContractorIdentifier")
            .to(new FakeIdentifierAction(false, false, true)(parsers)),
          bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
          bind[FormpRdsReconcileService].toInstance(formpRdsReconcileService),
          bind[SchemeAction] toInstance schemeAction,
          bind[MonthlyReturnAction] toInstance new FakeMonthlyReturnAction(userAnswers getOrElse emptyUserAnswers)(using
            ExecutionContext.global
          )
        ) ++ additionalBindings
      )
}
