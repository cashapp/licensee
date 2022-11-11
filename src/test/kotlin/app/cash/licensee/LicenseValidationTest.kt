/*
 * Copyright (C) 2022 Square, Inc.
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

import junit.framework.TestCase.assertEquals
import org.junit.Test

class LicenseValidationTest {

  private val mitLicensedArtifact = ArtifactDetail(
    "foo.bar",
    "baz",
    "1.0.0",
    spdxLicenses = setOf(
      SpdxLicense(
        "MIT",
        "MIT License",
        "https://spdx.org/licenses/MIT.html",
      ),
    ),
  )

  @Test fun ignoreUnusedAllow() {
    val configUnderTest = ValidationConfig(
      allowedIdentifiers = setOf("MIT-0", "MIT"),
      allowedUrls = emptySet(),
      allowedCoordinates = emptyMap(),
      unusedAllowAction = UnusedLicenseConfigurationAction.IGNORE,
      unusedAllowUrlAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowDependencyAction = UnusedLicenseConfigurationAction.LOG,
    )

    assertEquals(
      validateArtifacts(configUnderTest, listOf(mitLicensedArtifact)).configResults,
      emptyList<ValidationResult>(),
    )
  }

  @Test fun reportUnusedAllow() {
    val configUnderTest = ValidationConfig(
      allowedIdentifiers = setOf("MIT-0", "MIT"),
      allowedUrls = emptySet(),
      allowedCoordinates = emptyMap(),
      unusedAllowAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowUrlAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowDependencyAction = UnusedLicenseConfigurationAction.LOG,
    )

    assertEquals(
      validateArtifacts(configUnderTest, listOf(mitLicensedArtifact)).configResults,
      listOf(ValidationResult.Warning("Allowed SPDX identifier 'MIT-0' is unused")),
    )
  }

  @Test fun ignoreUnusedAllowUrl() {
    val configUnderTest = ValidationConfig(
      allowedIdentifiers = setOf("MIT"),
      allowedUrls = setOf("https://some-url.com/foo"),
      allowedCoordinates = emptyMap(),
      unusedAllowAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowUrlAction = UnusedLicenseConfigurationAction.IGNORE,
      unusedAllowDependencyAction = UnusedLicenseConfigurationAction.LOG,
    )

    assertEquals(
      validateArtifacts(configUnderTest, listOf(mitLicensedArtifact)).configResults,
      emptyList<ValidationResult>(),
    )
  }

  @Test fun reportUnusedAllowUrl() {
    val configUnderTest = ValidationConfig(
      allowedIdentifiers = setOf("MIT"),
      allowedUrls = setOf("https://some-url.com/foo"),
      allowedCoordinates = emptyMap(),
      unusedAllowAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowUrlAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowDependencyAction = UnusedLicenseConfigurationAction.LOG,
    )

    assertEquals(
      validateArtifacts(configUnderTest, listOf(mitLicensedArtifact)).configResults,
      listOf(ValidationResult.Warning("Allowed license URL 'https://some-url.com/foo' is unused")),
    )
  }

  @Test fun ignoreUnusedAllowDependency() {
    val configUnderTest = ValidationConfig(
      allowedIdentifiers = setOf("MIT"),
      allowedUrls = emptySet(),
      allowedCoordinates = mapOf(DependencyCoordinates("some.unused", "artifact", "1.0.0") to "Awesome."),
      unusedAllowAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowUrlAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowDependencyAction = UnusedLicenseConfigurationAction.IGNORE,
    )

    assertEquals(
      validateArtifacts(configUnderTest, listOf(mitLicensedArtifact)).configResults,
      emptyList<ValidationResult>(),
    )
  }

  @Test fun reportUnusedAllowDependency() {
    val configUnderTest = ValidationConfig(
      allowedIdentifiers = setOf("MIT"),
      allowedUrls = emptySet(),
      allowedCoordinates = mapOf(DependencyCoordinates("some.unused", "artifact", "1.0.0") to "Awesome."),
      unusedAllowAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowUrlAction = UnusedLicenseConfigurationAction.LOG,
      unusedAllowDependencyAction = UnusedLicenseConfigurationAction.LOG,
    )

    assertEquals(
      validateArtifacts(configUnderTest, listOf(mitLicensedArtifact)).configResults,
      listOf(ValidationResult.Warning("Allowed dependency 'some.unused:artifact:1.0.0' is unused")),
    )
  }
}
