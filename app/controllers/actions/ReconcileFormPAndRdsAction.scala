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

package controllers.actions

import com.google.inject.{ImplementedBy, Inject, Singleton}
import models.requests.SchemeRequest
import play.api.Logging
import play.api.http.Status
import play.api.mvc.{ActionFilter, Result, Results}
import services.FormpRdsReconcileService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@ImplementedBy(classOf[ReconcileFormPAndRdsActionImpl])
abstract class ReconcileFormPAndRdsAction extends ActionFilter[SchemeRequest]

@Singleton
final class ReconcileFormPAndRdsActionImpl @Inject() (
  formPAndRdsReconciler: FormpRdsReconcileService
)(using val executionContext: ExecutionContext)
    extends ReconcileFormPAndRdsAction
    with Logging
    with Status
    with Results
    with FrontendHeaderCarrierProvider {

  def filter[B](request: SchemeRequest[B]): Future[Option[Result]] = {
    given SchemeRequest[?] = request

    formPAndRdsReconciler
      .reconcile(request.cisId, request.cisTaxpayer.taxOfficeNumber, request.cisTaxpayer.taxOfficeRef)
      .map(_ => None)
      .recover {
        case e: UpstreamErrorResponse if e.statusCode == PRECONDITION_FAILED || e.statusCode == NOT_FOUND =>
          logger.warn(s"Contractor known facts missing in RDS (status ${e.statusCode})")
          Some(Redirect(controllers.routes.UnauthorisedOrganisationAffinityController.onPageLoad()))
        case NonFatal(e)                                                                                  =>
          logger.error(s"FormP/RDS reconciliation failed: ${e.getMessage}", e)
          Some(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
  }
}
