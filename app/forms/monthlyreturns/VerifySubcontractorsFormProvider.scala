package forms.monthlyreturns

import forms.mappings.Mappings
import models.VerifySubcontractors
import play.api.data.Form

import javax.inject.Inject

class VerifySubcontractorsFormProvider @Inject() extends Mappings {

  def apply(): Form[VerifySubcontractors] =
    Form(
      "value" -> enumerable[VerifySubcontractors]("verifySubcontractors.error.required")
    )
}
