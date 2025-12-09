package forms.monthlyreturns

import forms.behaviours.BooleanFieldBehaviours
import forms.mappings.Mappings
import models.monthlyreturns.SelectSubcontractorsFormData
import play.api.data.Forms.single
import play.api.data.{Form, FormError}

class SelectSubcontractorsFormProviderSpec extends BooleanFieldBehaviours with Mappings {
// TODO: TEST NEEDS TO BE FINISHED
  val form: Form[SelectSubcontractorsFormData] = SelectSubcontractorsFormProvider()()

  ".confirmation" - {
    val fieldName   = "confirmation"
    val requiredKey = "monthlyreturns.selectSubcontractors.confirmation"

    val booleanMappingForm = Form(single(fieldName -> boolean(requiredKey)))

    behave like booleanField(
      booleanMappingForm,
      fieldName,
      invalidError = FormError(fieldName, requiredKey)
    )

    behave like mandatoryField(
      booleanMappingForm,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
