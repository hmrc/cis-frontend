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

package models.requests

import base.SpecBase
import models.requests.CisPath.{CisId, CisOrg}
import play.api.mvc.PathBindable

class CisPathSpec extends SpecBase {

  "CisPath" - {

    "return CisOrg when the path is org" in {
      CisPath.from("org") mustBe Some(CisOrg)
    }

    "return CisId when the path contains a valid CIS ID" in {
      CisPath.from("123ABC") mustBe Some(CisId("123ABC"))
    }

    "return None when the path is empty" in {
      CisPath.from("") mustBe None
    }

    "return None when the path contains invalid characters" in {
      CisPath.from("123-ABC") mustBe None
      CisPath.from("123_ABC") mustBe None
      CisPath.from("123/ABC") mustBe None
    }

    "return the correct URL values" in {
      CisOrg.toUrl mustBe "org"
      CisId("123ABC").toUrl mustBe "123ABC"
    }
  }

  "CisPath PathBindable" - {

    val pathBindable = summon[PathBindable[CisPath]]

    "bind an organisation path" in {
      pathBindable.bind("cisPath", "org") mustBe Right(CisOrg)
    }

    "bind a valid CIS ID" in {
      pathBindable.bind("cisPath", "123ABC") mustBe Right(CisId("123ABC"))
    }

    "return an error when the path is invalid" in {
      pathBindable.bind("cisPath", "123-ABC") mustBe
        Left("Could not resolve CIS path from: 123-ABC")
    }

    "unbind an organisation path" in {
      pathBindable.unbind("cisPath", CisOrg) mustBe "org"
    }

    "unbind a CIS ID" in {
      pathBindable.unbind("cisPath", CisId("123ABC")) mustBe "123ABC"
    }
  }
}
