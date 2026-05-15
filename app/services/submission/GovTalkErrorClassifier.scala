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

package services.submission

import models.submission.GovTalkErrorStatus
import models.submission.GovTalkErrorStatus.*
import play.api.libs.json.JsValue

object GovTalkErrorClassifier {

  private val RecoverableCodes = Set("3000", "2005", "1000")

  def classify(status: String, error: Option[JsValue]): GovTalkErrorStatus = {
    val errorCode = error.flatMap(js => (js \ "number").asOpt[String])
    val errorText = error.flatMap(js => (js \ "text").asOpt[String]).getOrElse("")

    status match {
      case "DEPARTMENTAL_ERROR"                                         =>
        DepartmentalError(errorText)
      case "STARTED" if errorCode.exists(RecoverableCodes.contains)     =>
        RecoverableError(errorCode.get, errorText)
      case "FATAL_ERROR" if errorCode.exists(RecoverableCodes.contains) =>
        RecoverableError(errorCode.get, errorText)
      case "FATAL_ERROR"                                                =>
        errorCode match {
          case Some(code) => FatalError(code, errorText)
          case None       => OtherStatus
        }
      case _                                                            =>
        OtherStatus
    }
  }
}
