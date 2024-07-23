/*
 * Copyright (C) 2021 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.licensee

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

class SpdxLicensesGeneratorTest {
  @Test fun httpUrlGetsHttpsVariant() {
    // language=json
    val json = """
      |{"licenses":[
      |  {
      |    "reference": "https://spdx.org/licenses/FOO-1.0.html",
      |    "isDeprecatedLicenseId": false,
      |    "detailsUrl": "https://ignor.ed",
      |    "referenceNumber": 42,
      |    "name": "Foo License",
      |    "licenseId": "FOO-1.0",
      |    "seeAlso": [
      |      "http://example.com/foo"
      |    ],
      |    "isOsiApproved": false
      |  },
      |  {
      |    "reference": "https://spdx.org/licenses/BAR-1.0.html",
      |    "isDeprecatedLicenseId": false,
      |    "detailsUrl": "https://ignor.ed",
      |    "referenceNumber": 42,
      |    "name": "Bar License",
      |    "licenseId": "BAR-1.0",
      |    "seeAlso": [
      |      "https://example.com/bar"
      |    ],
      |    "isOsiApproved": false
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json) {}
    assertThat(
      spdxLicenses.findByUrl("http://example.com/foo"),
    ).isNotNull().containsExactly(
      SpdxLicenseJson("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html", listOf("http://example.com/foo")),
    )
    assertThat(
      spdxLicenses.findByUrl("https://example.com/foo"),
    ).isNotNull().containsExactly(
      SpdxLicenseJson("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html", listOf("http://example.com/foo")),
    )
    assertThat(spdxLicenses.findByUrl("http://example.com/bar")).isNull()
    assertThat(
      spdxLicenses.findByUrl("https://example.com/bar"),
    ).isNotNull().containsExactly(
      SpdxLicenseJson("BAR-1.0", "Bar License", "https://spdx.org/licenses/BAR-1.0.html", listOf("https://example.com/bar")),
    )
  }

  @Test fun spdxUrlSupported() {
    // language=json
    val json = """
      |{"licenses":[
      |  {
      |    "reference": "https://spdx.org/licenses/FOO-1.0.html",
      |    "isDeprecatedLicenseId": false,
      |    "detailsUrl": "https://ignor.ed",
      |    "referenceNumber": 42,
      |    "name": "Foo License",
      |    "licenseId": "FOO-1.0",
      |    "seeAlso": [
      |      "http://example.com/foo"
      |    ],
      |    "isOsiApproved": false
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json) {}
    assertThat(
      spdxLicenses.findByUrl("http://example.com/foo"),
    ).isNotNull().containsExactly(
      SpdxLicenseJson("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html", listOf("http://example.com/foo")),
    )
    assertThat(
      spdxLicenses.findByUrl("https://spdx.org/licenses/FOO-1.0.html"),
    ).isNotNull().containsExactly(
      SpdxLicenseJson("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html", listOf("http://example.com/foo")),
    )
  }

  @Test fun spdxUrlFallback() {
    // language=json
    val json = """
      |{"licenses":[
      |  {
      |   "reference": "https://spdx.org/licenses/FOO-1.0.html",
      |   "isDeprecatedLicenseId": false,
      |    "detailsUrl": "https://ignor.ed",
      |    "referenceNumber": 42,
      |    "name": "Foo License",
      |    "licenseId": "FOO-1.0",
      |    "seeAlso": [],
      |    "isOsiApproved": false
      |  },
      |  {
      |    "reference": "https://spdx.org/licenses/BAR-1.0.html",
      |    "isDeprecatedLicenseId": false,
      |    "detailsUrl": "https://ignor.ed",
      |    "referenceNumber": 42,
      |    "name": "Bar License",
      |    "licenseId": "BAR-1.0",
      |    "seeAlso": [],
      |    "isOsiApproved": false
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json) {}
    assertThat(
      spdxLicenses.findByIdentifier("FOO-1.0"),
    ).isEqualTo(
      SpdxLicenseJson("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html", emptyList()),
    )
    assertThat(
      spdxLicenses.findByIdentifier("BAR-1.0"),
    ).isEqualTo(
      SpdxLicenseJson("BAR-1.0", "Bar License", "https://spdx.org/licenses/BAR-1.0.html", emptyList()),
    )
  }

  @Test fun customUrlFallback() {
    val json = """
      |{"licenses":[
      |  {
      |    "licenseId": "FOO-1.0",
      |    "name": "Foo License",
      |    "reference": "https://spdx.org/licenses/FOO-1.0.html",
      |    "seeAlso": []
      |  },
      |  {
      |    "licenseId": "BAR-1.0",
      |    "name": "Bar License",
      |    "reference": "https://spdx.org/licenses/BAR-1.0.html",
      |    "seeAlso": []
      |  },
      |  {
      |    "licenseId": "BAR-2.0",
      |    "name": "Bar 2 License",
      |    "reference": "https://spdx.org/licenses/BAR-2.0.html",
      |    "seeAlso": []
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json) {
      putLicense("BAR-1.0", "BAR-2.0") {
        add("https://example.org/bar")
        add("https://example.com/bar")
      }
    }
    val foo = SpdxLicenseJson("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html", emptyList())
    val bar1 = SpdxLicenseJson("BAR-1.0", "Bar License", "https://spdx.org/licenses/BAR-1.0.html", emptyList())
    val bar2 = SpdxLicenseJson("BAR-2.0", "Bar 2 License", "https://spdx.org/licenses/BAR-2.0.html", emptyList())
    assertThat(
      spdxLicenses.findByUrl("https://spdx.org/licenses/FOO-1.0.html"),
    ).isNotNull().containsExactly(
      foo,
    )
    assertThat(
      spdxLicenses.findByUrl("https://example.com/bar"),
    ).isNotNull().containsExactly(
      bar1,
      bar2,
    )
    assertThat(
      spdxLicenses.findByUrl("https://example.org/bar"),
    ).isNotNull().containsExactly(
      bar1,
      bar2,
    )
    assertThat(
      spdxLicenses.simplified,
    ).containsExactly(
      listOf("https://example.com/bar", "https://example.org/bar") to listOf(bar1, bar2),
      listOf("https://spdx.org/licenses/BAR-1.0.html") to listOf(bar1),
      listOf("https://spdx.org/licenses/BAR-2.0.html") to listOf(bar2),
      listOf("https://spdx.org/licenses/FOO-1.0.html") to listOf(foo),
    )
  }
}
