/*
 * Copyright 2026 HM Revenue & Customs
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

package forms

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class ValidationSpec extends AnyFreeSpec with Matchers {

  "emailRegex" - {

    def matches(email: String): Boolean =
      email.matches(Validation.emailRegex)

    "valid emails" - {

      "must match a standard address" in {
        matches("test@example.com") mustBe true
      }

      "must match a subdomain" in {
        matches("test@mail.example.com") mustBe true
      }

      "must match a ccTLD" in {
        matches("test@example.co.uk") mustBe true
      }

      "must match with special characters in local part" in {
        matches("test.name+tag@example.com") mustBe true
      }

      "must match a hyphenated domain" in {
        matches("user@my-domain.com") mustBe true
      }

      "must match a short address" in {
        matches("a@b.co") mustBe true
      }

      "must match allowed special characters" in {
        matches("user!#$%&*+-/=?^_`{|}~@example.com") mustBe true
      }

      "must match special characters in the domain" in {
        matches("user@domain!#$%&*+-/=?^_`{|}~.com") mustBe true
      }

      "must match an IPv4 address" in {
        matches("test@192.168.1.1") mustBe true
      }

      "must match when domain starts with a dot" in {
        matches("test@.example.com") mustBe true
      }
    }

    "missing structure" - {

      "must not match with no local part" in {
        matches("@example.com") mustBe false
      }

      "must not match with no domain" in {
        matches("test@") mustBe false
      }

      "must not match with no at sign" in {
        matches("testexample.com") mustBe false
      }

      "must not match an empty string" in {
        matches("") mustBe false
      }

      "must not match with multiple at signs" in {
        matches("test@@example.com") mustBe false
      }
    }

    "non-ASCII characters" - {

      "must not match a non-ASCII character" in {
        matches("test@Ã«xample.com") mustBe false
      }
    }
  }

  "phoneRegex" - {

    def matches(phoneNumber: String): Boolean =
      phoneNumber.matches(Validation.phoneRegex)

    "match values allowed by the supplied regex" in {
      Seq(
        "07777777777",
        "  07777 77777 ",
        "(44)77777777777",
        "44-777-777",
        "12345",
        "()",
        "-",
        "0191\t1234567"
      ).foreach { phoneNumber =>
        matches(phoneNumber) mustBe true
      }
    }

    "reject values containing unsupported characters" in {
      Seq(
        "+44 7700 900 999",
        "0191/1234567",
        "0191 PHONE"
      ).foreach { phoneNumber =>
        matches(phoneNumber) mustBe false
      }
    }
  }

  "mobileRegex" - {

    "use the same supplied validation rule as phone numbers" in {
      Validation.mobileRegex mustBe Validation.phoneRegex
    }
  }

  "addressRegex" - {

    def matches(addressLine: String): Boolean =
      addressLine.matches(Validation.addressRegex)

    "match supported address characters" in {
      matches("1 High Street, Newcastle") mustBe true
      matches("Flat 2/A & B") mustBe true
    }

    "reject unsupported address characters" in {
      matches("Tyne | Wear") mustBe false
    }
  }

  "firstCharLetterOrDigitRegex" - {

    "match a value beginning with a letter or digit" in {
      "High Street".matches(Validation.firstCharLetterOrDigitRegex) mustBe true
      "1 High Street".matches(Validation.firstCharLetterOrDigitRegex) mustBe true
    }

    "reject a value that does not begin with a letter or digit" in {
      "-High Street".matches(Validation.firstCharLetterOrDigitRegex) mustBe false
    }
  }

  "ukPostcodeRegex" - {

    def matches(postcode: String): Boolean =
      postcode.matches(Validation.ukPostcodeRegex)

    "match supported postcode characters" in {
      matches("NE1 1AA") mustBe true
      matches("NE1~1AA") mustBe true
    }

    "reject unsupported postcode characters" in {
      matches("NE1`1AA") mustBe false
    }
  }
}
