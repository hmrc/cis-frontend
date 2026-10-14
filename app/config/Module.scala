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

package config

import controllers.actions.*
import play.api.inject.{Binding, Module as PlayModule}
import play.api.{Configuration, Environment}
import services.{FormpRdsReconcileService, FormpRdsReconcileServiceImpl, MonthlyReturnItemPayloadBuilder, MonthlyReturnItemPayloadBuilderImpl}
import services.guard.{DuplicateMRCreationGuard, DuplicateMRCreationGuardImpl, SubmissionSuccessfulServiceGuard, SubmissionSuccessfulServiceGuardImpl}
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import utils.{ReferenceGenerator, ReferenceGeneratorImpl}

import java.time.{Clock, ZoneId}

class Module extends PlayModule {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[?]] = Seq(
    bind[DataRetrievalAction].to[DataRetrievalActionImpl].eagerly(),
    bind[DataRequiredAction].to[DataRequiredActionImpl].eagerly(),
    bind[CisIdRequiredAction].to[CisIdRequiredActionImpl].eagerly(),
    bind[ReferenceGenerator].to[ReferenceGeneratorImpl].eagerly(),
    // For session based storage instead of cred based, change to SessionIdentifierAction
    bind[IdentifierAction].to[AuthenticatedIdentifierAction].eagerly(),
    bind[IdentifierAction].qualifiedWith("ContractorIdentifier").to[ContractorIdentifierAction].eagerly(),
    bind[IdentifierAction].qualifiedWith("AgentIdentifier").to[AgentIdentifierAction].eagerly(),
    bind[DuplicateMRCreationGuard].to[DuplicateMRCreationGuardImpl],
    bind[SubmissionSuccessfulServiceGuard].to[SubmissionSuccessfulServiceGuardImpl],
    bind[Clock].toInstance(Clock.system(ZoneId.of("Europe/London"))),
    bind[MonthlyReturnItemPayloadBuilder].to[MonthlyReturnItemPayloadBuilderImpl],
    bind[FormpRdsReconcileService].to[FormpRdsReconcileServiceImpl],
    bind[Encrypter with Decrypter].toProvider[CryptoProvider]
  )
}
