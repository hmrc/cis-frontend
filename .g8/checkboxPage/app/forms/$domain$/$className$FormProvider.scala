package forms.$domain$

import javax.inject.Inject

import forms.mappings.Mappings
import play.api.data.Form
import play.api.data.Forms.set
import models.$domain$.$className$

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[Set[$className$]] =
    Form(
      "value" -> set(enumerable[$className$]("$domain$.$className;format="decap"$.error.required")).verifying(nonEmptySet("$domain$.$className;format="decap"$.error.required"))
    )
}
