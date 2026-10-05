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

package controllers

import models.monthlyreturns.CisTaxpayer
import models.{UserAnswers, requests}
import models.requests.{CisPath, IdentifierRequest, JourneyRequest, SchemeRequest}
import play.api.mvc.{ActionRefiner, Result}

import scala.concurrent.{ExecutionContext, Future}

package object actions {

  final class FakeSchemeAction(cisTaxpayer: CisTaxpayer)(using ec: ExecutionContext) extends SchemeAction {

    def apply(cisPath: CisPath): ActionRefiner[IdentifierRequest, SchemeRequest] =
      new ActionRefiner {
        val executionContext: ExecutionContext = ec

        def refine[A](request: IdentifierRequest[A]): Future[Either[Result, SchemeRequest[A]]] =
          Future.successful(Right(new SchemeRequest(request, cisTaxpayer)))
      }
  }

  final class FakeMonthlyReturnAction(ua: UserAnswers)(using val executionContext: ExecutionContext)
      extends MonthlyReturnAction {

    def transform[A](request: SchemeRequest[A]): Future[JourneyRequest[A]] =
      Future.successful(new JourneyRequest(request, ua))
  }
}
