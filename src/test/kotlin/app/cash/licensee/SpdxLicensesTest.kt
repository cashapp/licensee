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
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import java.io.File
import org.junit.Test

class SpdxLicensesTest {
  @Test fun embeddedDatabaseLitmusTest() {
    assertThat(
      SpdxId.findByIdentifier("MIT-0")?.toSpdxLicense(),
    ).isEqualTo(
      SpdxLicense("MIT-0", "MIT No Attribution", "https://github.com/aws/mit-0"),
    )
  }

  @Test fun fallbackDatabaseLitmusTest() {
    assertThat(
      SpdxId.findByUrl("https://api.github.com/licenses/bsd-2-clause").map { it.toSpdxLicense() },
    ).containsExactly(
      SpdxLicense("BSD-2-Clause", "BSD 2-Clause \"Simplified\" License", "https://opensource.org/licenses/BSD-2-Clause"),
    )
    assertThat(
      SpdxId.findByUrl("https://api.github.com/licenses/gpl-2.0").map { it.toSpdxLicense() },
    ).containsExactly(
      SpdxLicense("GPL-2.0", "GNU General Public License v2.0 only", "https://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html"),
      SpdxLicense("GPL-2.0-or-later", "GNU General Public License v2.0 or later", "https://www.gnu.org/licenses/old-licenses/gpl-2.0-standalone.html"),
    )
  }

  @Test fun spdxIdsAreValidGroovy() {
    val file = File(System.getProperty("generatedSpdxFile")).readText()
    assertThat(file).doesNotContain("`")
  }
}
