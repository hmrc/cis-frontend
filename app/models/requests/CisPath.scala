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

import play.api.mvc.PathBindable

/** [[CisPath]] tells the app how to locate a CIS account for different users.
  *   - [[CisOrg]] uses an Organisation user's HMRC-CIS-ORG enrolment.
  *   - [[CisId]] identifies an Agent's CIS client.
  */
enum CisPath(val toUrl: String) {
  case CisOrg extends CisPath("org")
  case CisId(value: String) extends CisPath(value)
}

object CisPath {

  def from(urlPath: String): Option[CisPath] =
    if urlPath == CisOrg.toUrl then Some(CisOrg)
    else if urlPath.nonEmpty && urlPath.forall(_.isLetterOrDigit) then Some(CisId(urlPath))
    else None

  given PathBindable[CisPath] = new PathBindable {
    def bind(key: String, value: String): Either[String, CisPath] =
      CisPath.from(value).toRight(s"Could not resolve CIS path from: $value")

    def unbind(key: String, value: CisPath): String = value.toUrl
  }
}
