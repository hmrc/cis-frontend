package utils

import base.SpecBase

class ReferenceGeneratorSpec extends SpecBase {

  val referenceGeneratorImpl = new ReferenceGeneratorImpl()

  "ReferenceGenerator" - {

    "should generate a 16-character alphanumeric string and not be empty" in {
      val referenceNumber = referenceGeneratorImpl.generateReference()

      referenceNumber.length mustBe 16
      referenceNumber must fullyMatch regex "^[A-Za-z0-9]+$"
      referenceNumber must not be empty
    }
  }
}
