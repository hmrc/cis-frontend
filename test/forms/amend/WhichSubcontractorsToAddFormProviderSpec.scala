package forms.amend

import forms.behaviours.CheckboxFieldBehaviours
import models.amend.WhichSubcontractorsToAdd
import play.api.data.FormError

class WhichSubcontractorsToAddFormProviderSpec extends CheckboxFieldBehaviours {

  private val subcontractors = WhichSubcontractorsToAdd.mockSubcontractors
  val form                   = new WhichSubcontractorsToAddFormProvider()(subcontractors)

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "amend.whichSubcontractorsToAdd.error.required"

    behave like checkboxField[String](
      form,
      fieldName,
      validValues  = subcontractors.map(_.id),
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
