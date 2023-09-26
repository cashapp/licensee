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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpdxLicensesTest {
  @Test fun embeddedDatabaseLitmusTest() {
    assertEquals(
      SpdxLicense("MIT-0", "MIT No Attribution", "https://github.com/aws/mit-0"),
      SpdxLicenses.embedded.findByIdentifier("MIT-0"),
    )
  }

  @Test fun fallbackDatabaseLitmusTest() {
    assertEquals(
      SpdxLicense("BSD-2-Clause", "BSD 2-Clause \"Simplified\" License", "https://opensource.org/licenses/BSD-2-Clause"),
      fallbackIds["https://api.github.com/licenses/bsd-2-clause"],
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
    assertEquals(
      listOf(SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo")),
      spdxLicenses.findByUrl("http://example.com/foo"),
    )
    assertEquals(
      listOf(SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo")),
      spdxLicenses.findByUrl("https://example.com/foo"),
    )
    assertNull(spdxLicenses.findByUrl("http://example.com/bar"))
    assertEquals(
      listOf(SpdxLicense("BAR-1.0", "Bar License", "https://example.com/bar")),
      spdxLicenses.findByUrl("https://example.com/bar"),
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
    assertEquals(
      listOf(SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo")),
      spdxLicenses.findByUrl("http://example.com/foo"),
    )
    assertEquals(
      listOf(SpdxLicense("FOO-1.0", "Foo License", "https://example.com/foo")),
      spdxLicenses.findByUrl("https://spdx.org/licenses/FOO-1.0.html"),
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
    assertEquals(
      SpdxLicense("FOO-1.0", "Foo License", "https://spdx.org/licenses/FOO-1.0.html"),
      spdxLicenses.findByIdentifier("FOO-1.0"),
    )
    assertEquals(
      SpdxLicense("BAR-1.0", "Bar License", "https://example.com/bar"),
      spdxLicenses.findByIdentifier("BAR-1.0"),
    )
  }
}
