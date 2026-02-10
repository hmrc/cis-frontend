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

package generators

import models.monthlyreturns.AddSubcontractorDetails
import models.monthlyreturns.{Declaration, InactivityRequest}
import org.scalacheck.{Arbitrary, Gen}

trait ModelGenerators {

  implicit lazy val arbitraryAddSubcontractorDetails: Arbitrary[AddSubcontractorDetails] =
    Arbitrary {
      Gen.oneOf(AddSubcontractorDetails.values.toSeq)
    }

  implicit lazy val arbitraryVerifySubcontractors: Arbitrary[Boolean] =
    Arbitrary {
      Gen.oneOf(true, false)
    }

  implicit lazy val arbitraryDeclaration: Arbitrary[Declaration] =
    Arbitrary {
      Gen.oneOf(Declaration.values)
    }

  implicit lazy val arbitraryInactivityRequest: Arbitrary[InactivityRequest] =
    Arbitrary {
      Gen.oneOf(InactivityRequest.values.toSeq)
    }
}
