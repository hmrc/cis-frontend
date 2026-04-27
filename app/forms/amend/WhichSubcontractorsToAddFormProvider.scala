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

package forms.amend

import javax.inject.Inject
import forms.mappings.Mappings
import models.amend.Subcontractor
import play.api.data.{Form, Mapping}
import play.api.data.Forms.{of, set}
import play.api.data.format.Formatter

class WhichSubcontractorsToAddFormProvider @Inject() extends Mappings {

  def apply(subcontractors: Seq[Subcontractor]): Form[Set[String]] = {
    val validIds = subcontractors.map(_.id).toSet

    val subcontractorIdMapping: Mapping[String] = of(new Formatter[String] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[play.api.data.FormError], String] =
        data.get(key).filter(_.nonEmpty) match {
          case Some(value) if validIds.contains(value) => Right(value)
          case Some(_)                                 => Left(Seq(play.api.data.FormError(key, "error.invalid")))
          case None                                    => Left(Seq(play.api.data.FormError(key, "amend.whichSubcontractorsToAdd.error.required")))
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    })

    Form(
      "value" -> set(subcontractorIdMapping)
        .verifying(nonEmptySet("amend.whichSubcontractorsToAdd.error.required"))
    )
  }
}
