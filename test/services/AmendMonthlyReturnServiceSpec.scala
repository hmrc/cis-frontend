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

package services

import base.SpecBase
import connectors.ConstructionIndustrySchemeConnector
import models.amend.CreateAmendedMonthlyReturnRequest
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AmendMonthlyReturnServiceSpec extends SpecBase {

  "AmendMonthlyReturnService" - {

    "createAmendedMonthlyReturn should delegate to the CIS connector" in {
      implicit val hc: HeaderCarrier = HeaderCarrier()

      val mockConnector = mock[ConstructionIndustrySchemeConnector]

      val request = CreateAmendedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      when(
        mockConnector.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest]())(
          any[HeaderCarrier]()
        )
      ) thenReturn Future.successful(())

      val service = new AmendMonthlyReturnService(mockConnector)

      service.createAmendedMonthlyReturn(request).futureValue mustBe ((): Unit)

      verify(mockConnector).createAmendedMonthlyReturn(request)(hc)
    }
  }
}
