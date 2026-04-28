package forms

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import models.AeYouSureYouWantToAmend

class AeYouSureYouWantToAmendFormProvider @Inject() extends Mappings {

  def apply(): Form[AeYouSureYouWantToAmend] =
    Form(
      "value" -> enumerable[AeYouSureYouWantToAmend]("aeYouSureYouWantToAmend.error.required")
    )
}
