package forms

import forms.behaviours.OptionFieldBehaviours
import models.monthlyreturns.AddSubcontractorDetails
import play.api.data.FormError

class AddSubcontractorDetailsFormProviderSpec extends OptionFieldBehaviours {

  val form = new AddSubcontractorDetailsFormProvider()()

  ".value" - {

    val fieldName   = "value"
    val requiredKey = "addSubcontractorDetails.error.required"

    behave like optionsField[AddSubcontractorDetails](
      form,
      fieldName,
      validValues = AddSubcontractorDetails.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
