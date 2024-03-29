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
import org.junit.Test

class SpdxLicensesTest {
  @Test fun embeddedDatabaseLitmusTest() {
    assertThat(
      SpdxLicenses.embedded.findByIdentifier("MIT-0"),
    ).isEqualTo(
      SpdxLicense("MIT-0", "MIT No Attribution", "https://github.com/aws/mit-0"),
    )
  }

  @Test fun fallbackDatabaseLitmusTest() {
    assertThat(
      fallbackUrls["https://api.github.com/licenses/bsd-2-clause"],
    ).isNotNull().containsExactly(
      SpdxLicense("BSD-2-Clause", "BSD 2-Clause \"Simplified\" License", "https://opensource.org/licenses/BSD-2-Clause"),
    )
    assertThat(
      fallbackUrls["https://api.github.com/licenses/gpl-2.0"],
    ).isNotNull().containsExactly(
      SpdxLicense("GPL-2.0", "GNU General Public License v2.0 only", "https://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html"),
      SpdxLicense("GPL-2.0-or-later", "GNU General Public License v2.0 or later", "https://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html"),
    )
  }

  @Test fun httpUrlGetsHttpsVariant() {
    val json = """
      |{"licenses":[
      |  {
      |    "licenseId": "FOO-1.0",
      |    "name": "Foo License",
      |    "reference": "https://spdx.org/licenses/FOO-1.0.html",
      |    "seeAlso": [
      |      "http://example.com/foo"
      |    ]
      |  },
      |  {
      |    "licenseId": "BAR-1.0",
      |    "name": "Bar License",
      |    "reference": "https://spdx.org/licenses/BAR-1.0.html",
      |    "seeAlso": [
      |      "https://example.com/bar"
      |    ]
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json)
    assertThat(
      spdxLicenses.findByUrl("http://example.com/foo"),
    ).isNotNull().containsExactly(
      SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo"),
    )
    assertThat(
      spdxLicenses.findByUrl("https://example.com/foo"),
    ).isNotNull().containsExactly(
      SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo"),
    )
    assertThat(spdxLicenses.findByUrl("http://example.com/bar")).isNull()
    assertThat(
      spdxLicenses.findByUrl("https://example.com/bar"),
    ).isNotNull().containsExactly(
      SpdxLicense("BAR-1.0", "Bar License", "https://example.com/bar"),
    )
  }

  @Test fun spdxUrlSupported() {
    val json = """
      |{"licenses":[
      |  {
      |    "licenseId": "FOO-1.0",
      |    "name": "Foo License",
      |    "reference": "https://spdx.org/licenses/FOO-1.0.html",
      |    "seeAlso": [
      |      "http://example.com/foo"
      |    ]
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json)
    assertThat(
      spdxLicenses.findByUrl("http://example.com/foo"),
    ).isNotNull().containsExactly(
      SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo"),
    )
    assertThat(
      spdxLicenses.findByUrl("https://spdx.org/licenses/FOO-1.0.html"),
    ).isNotNull().containsExactly(
      SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo"),
    )
  }

  @Test fun spdxUrlFallback() {
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
      |    "seeAlso": [
      |      "https://example.com/bar"
      |    ]
      |  }
      |]}
      |
    """.trimMargin()
    val spdxLicenses = SpdxLicenses.parseJson(json)
    assertThat(
      spdxLicenses.findByIdentifier("FOO-1.0"),
    ).isEqualTo(
      SpdxLicense("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html"),
    )
    assertThat(
      spdxLicenses.findByIdentifier("BAR-1.0"),
    ).isEqualTo(
      SpdxLicense("BAR-1.0", "Bar License", "https://example.com/bar"),
    )
  }
}
