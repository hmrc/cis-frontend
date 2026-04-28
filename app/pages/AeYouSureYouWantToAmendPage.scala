package pages

import models.AeYouSureYouWantToAmend
import play.api.libs.json.JsPath

case object AeYouSureYouWantToAmendPage extends QuestionPage[AeYouSureYouWantToAmend] {

  override def path: JsPath = JsPath \ toString

  override def toString: String = "aeYouSureYouWantToAmend"
}
