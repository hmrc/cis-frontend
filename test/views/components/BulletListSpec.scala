/*
 * Copyright 2025 HM Revenue & Customs
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

package views.components

import base.SpecBase
import org.scalatest.matchers.must.Matchers
import views.html.components.BulletList
import play.api.test.FakeRequest
import play.api.i18n.Messages
import org.jsoup.Jsoup

class BulletListSpec extends SpecBase with Matchers {

  "BulletList" - {
    "must render all bullet points in the output HTML" in new Setup {
      val items   = Seq("First bullet", "Second bullet", "Third bullet")
      val html    = bulletList(items)
      val bullets = getBulletElements(html)
      bullets.size mustBe 3
      bullets.get(0).text mustBe "First bullet"
      bullets.get(1).text mustBe "Second bullet"
      bullets.get(2).text mustBe "Third bullet"
    }

    "must render with default CSS class" in new Setup {
      val items = Seq("First bullet", "Second bullet")
      val html  = bulletList(items)
      val ul    = getUlElement(html)
      ul.attr("class") mustBe "govuk-list govuk-list--bullet"
    }

    "must render with custom CSS class" in new Setup {
      val customClass = "custom-bullet-list-class"
      val items       = Seq("First bullet", "Second bullet")
      val html        = bulletList(items, classes = customClass)
      val ul          = getUlElement(html)
      ul.attr("class") mustBe customClass
    }

    "must render with ID when provided" in new Setup {
      val id    = "bullet-list"
      val items = Seq("First bullet", "Second bullet")
      val html  = bulletList(items, id = Some(id))
      val ul    = getUlElement(html)
      ul.attr("id") mustBe id
    }

    "must not render ID attribute when not provided" in new Setup {
      val items = Seq("First bullet", "Second bullet")
      val html  = bulletList(items)
      val ul    = getUlElement(html)
      ul.hasAttr("id") mustBe false
    }

    "must render with both custom class and ID" in new Setup {
      val customClass = "custom-bullet-list-class"
      val id          = "bullet-list"
      val items       = Seq("First bullet", "Second bullet")
      val html        = bulletList(items, classes = customClass, id = Some(id))
      val ul          = getUlElement(html)
      ul.attr("class") mustBe customClass
      ul.attr("id") mustBe id
    }

    "must handle empty list" in new Setup {
      val items   = Seq.empty[String]
      val html    = bulletList(items)
      val ul      = getUlElement(html)
      val bullets = getBulletElements(html)
      ul.size mustBe 1
      bullets.size mustBe 0
    }

    "must handle single item list" in new Setup {
      val items   = Seq("Single bullet")
      val html    = bulletList(items)
      val bullets = getBulletElements(html)
      bullets.size mustBe 1
      bullets.get(0).text mustBe "Single bullet"
    }

    "must handle large list" in new Setup {
      val items   = (1 to 10).map(i => s"Bullet $i")
      val html    = bulletList(items)
      val bullets = getBulletElements(html)
      bullets.size mustBe 10
      bullets.get(9).text mustBe "Bullet 10"
    }
  }

  trait Setup {
    val app                                       = applicationBuilder().build()
    val bulletList                                = app.injector.instanceOf[BulletList]
    implicit val request: play.api.mvc.Request[_] = FakeRequest()
    implicit val messages: Messages               = play.api.i18n.MessagesImpl(
      play.api.i18n.Lang.defaultLang,
      app.injector.instanceOf[play.api.i18n.MessagesApi]
    )

    def getBulletElements(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("ul li")
    }

    def getUlElement(html: play.twirl.api.Html): org.jsoup.select.Elements = {
      val doc = Jsoup.parse(html.body)
      doc.select("ul")
    }

  }
}
