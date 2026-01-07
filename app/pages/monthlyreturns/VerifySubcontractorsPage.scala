package pages.monthlyreturns

import models.VerifySubcontractors
import pages.QuestionPage
import play.api.libs.json.JsPath

case object VerifySubcontractorsPage extends QuestionPage[VerifySubcontractors] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "verifySubcontractors"
}
