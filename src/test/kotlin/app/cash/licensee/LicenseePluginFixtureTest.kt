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

import assertk.Assert
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsMatch
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.support.expected
import assertk.assertions.support.show
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class LicenseePluginFixtureTest {
  @Test fun success(
    @TestParameter(
      "artifact-with-classifier",
      "artifact-with-extension",
      "artifact-with-gradle-metadata-android",
      "artifact-with-gradle-metadata-jvm",
      "compile-only-ignored",
      "coordinate-allow-unused",
      "coordinate-allowed",
      "coordinate-allowed-kts",
      "coordinate-allowed-override-spdx",
      "coordinate-allowed-override-url",
      "coordinate-allowed-with-reason",
      "coordinate-allowed-with-reason-kts",
      "coordinate-allowed-with-reason-version-catalog",
      "coordinate-allowed-with-reason-version-catalog-kts",
      "dependency-substitution-replace-local-with-remote",
      "dependency-substitution-replace-remote-with-local-ignored",
      "dependency-substitution-replace-remote-with-include-build-ignored",
      // TODO The following test does not work. See https://github.com/cashapp/licensee/issues/78.
      "dependency-verification-disabled",
      "exclude-ignored",
      "flat-dir-repository-ignored",
      "gson-broken-plugin",
      "ignore-group",
      "ignore-group-artifact",
      "ignore-group-artifact-kts",
      "ignore-group-artifact-transitive",
      "ignore-group-artifact-transitive-kts",
      "ignore-group-kts",
      "ignore-group-transitive",
      "ignore-group-transitive-kts",
      "local-file-ignored",
      "local-file-tree-ignored",
      "multiple-with-spdx-allowed",
      "multiple-with-url-allowed",
      "platform-pom-is-excluded",
      "plugin-android-application",
      "plugin-android-application-product-flavors",
      "plugin-android-dynamic-feature",
      "plugin-android-dynamic-feature-product-flavors",
      "plugin-android-library",
      "plugin-android-library-product-flavors",
      "plugin-android-report-dir",
      "plugin-java",
      "plugin-java-library",
      "plugin-java-report-dir",
      "plugin-kotlin-js",
      "plugin-kotlin-jvm",
      "plugin-kotlin-mpp",
      "plugin-kotlin-mpp-jvm-with-java",
      "plugin-kotlin-mpp-native",
      "plugin-kotlin-mpp-report-dir",
      "plugin-kotlin-mpp-with-android-application",
      "plugin-kotlin-mpp-with-android-application-after-licensee",
      "plugin-kotlin-mpp-with-android-application-product-flavors",
      "plugin-kotlin-mpp-with-android-library",
      "plugin-kotlin-mpp-with-android-library-product-flavors",
      "plugin-kotlin-mpp-with-application",
      "pom-with-inheritance-from-both",
      "pom-with-inheritance-from-child",
      "pom-with-inheritance-from-parent",
      "pom-with-inheritance-from-parent-and-variable-substitution",
      "pom-with-inheritance-from-parent-with-child-path",
      "pom-with-inheritance-from-parent-without-child-path",
      "project-android-to-android-ignored",
      "project-android-to-java-ignored",
      "project-java-to-java-ignored",
      "repository-include-exclude",
      "spdx-allow-unused",
      "spdx-allowed",
      "spdx-allowed-but-no-match",
      "spdx-allowed-kts",
      "transitive-android-to-android-api",
      "transitive-android-to-android-implementation",
      "transitive-android-to-java-api",
      "transitive-android-to-java-implementation",
      "transitive-java-to-java-api",
      "transitive-java-to-java-implementation",
      "url-allow-unused",
      "url-allowed",
      "url-allowed-reason",
      "url-allowed-but-is-spdx",
      "url-allowed-but-no-match",
      "url-allowed-kts",
      "url-missing-name-fallback-is-spdx",
      "url-mapping-to-multiple-licenses",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    createRunner(fixtureDir).build()
    assertExpectedFiles(fixtureDir)

    // Ensure up-to-date functionality works.
    val secondRun = GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withDebug(true) // Run in-process
      .withArguments("licensee", "--stacktrace", "--configuration-cache", versionProperty)
      .forwardOutput()
      .build()
    secondRun.tasks.filter { it.path.contains(":licensee") }.forEach {
      assertEquals("Second invocation of ${it.path}", UP_TO_DATE, it.outcome)
    }
  }

  @Test
  fun customConfig(
    @TestParameter(
      "plugin-java-custom-config",
      "plugin-java-custom-config-kts",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)

    GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withDebug(true) // Run in-process
      .withArguments("clean", "licenseeFoo", "--configuration-cache", versionProperty)
      .forwardOutput()
      .build()
    assertExpectedFiles(fixtureDir)
  }

  @Test
  fun consumeArtifactsJson(
    @TestParameter(
      "consume-artifact-json-as-task-input",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)

    GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withDebug(true) // Run in-process
      .withArguments("clean", "myCopy", "--configuration-cache", versionProperty)
      .forwardOutput()
      .build()
    assertExpectedFiles(fixtureDir)
  }

  @Test fun failure(
    @TestParameter(
      "coordinate-version-mismatch",
      "multiple-not-allowed-reports-all",
      "no-license-not-allowed",
      "spdx-not-allowed",
      "url-missing-name-fallback-no-match",
      "url-not-allowed",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    createRunner(fixtureDir).buildAndFail()
    assertExpectedFiles(fixtureDir)
  }

  @Test fun transitiveReasonRequired(
    @TestParameter(
      "ignore-group-artifact-transitive-requires-reason",
      "ignore-group-artifact-transitive-requires-reason-kts",
      "ignore-group-transitive-requires-reason",
      "ignore-group-transitive-requires-reason-kts",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).containsMatch(
      "Transitive dependency ignore on 'com\\.example(:example)?' is dangerous and requires a reason string".toRegex(),
    )
  }

  @Test fun invalidSpdxFails(
    @TestParameter("allow-with-invalid-spdx") fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).contains(
      "ASDF is not a valid SPDX id.",
    )
  }

  @Test fun pluginMissingOnRootFails(
    @TestParameter("plugin-missing-on-root-fails") fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).contains(
      "'app.cash.licensee' requires compatible language/platform plugin to be applied (root project)",
    )
  }

  @Test fun pluginMissingOnSubprojectFails(
    @TestParameter("plugin-missing-on-subproject-fails") fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).contains(
      "'app.cash.licensee' requires compatible language/platform plugin to be applied (project :some:thing)",
    )
  }

  @Test fun allowDependencyWithoutVersionFails(
    @TestParameter(
      "coordinate-allowed-with-reason-version-catalog-null-version",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).contains(
      "version was null in allowDependency for exam",
    )
  }

  @Test fun violationsLogged(
    @TestParameter(
      "spdx-not-allowed-log",
      "spdx-not-allowed-log-kts",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).build()
    assertExpectedFiles(fixtureDir)
    assertThat(result.output).contains(
      """
      |com.example:example:1.0.0
      | - ERROR: SPDX identifier 'Apache-2.0' is NOT allowed
      |
      """.trimMargin(),
    )
  }

  @Test fun violationsIgnored(
    @TestParameter(
      "spdx-not-allowed-ignore",
      "spdx-not-allowed-ignore-kts",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).build()
    assertExpectedFiles(fixtureDir)
    assertThat(result.output).doesNotContain(
      """
      |com.example:example:1.0.0
      | - ERROR: SPDX identifier 'Apache-2.0' is NOT allowed
      |
      """.trimMargin(),
    )
  }

  @Test fun unusedIgnored(
    @TestParameter(
      "coordinate-allow-unused-ignored",
      "coordinate-allow-unused-ignored-kts",
      "spdx-allow-unused-ignored",
      "url-allow-unused-ignored",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).build()
    assertExpectedFiles(fixtureDir)
    assertThat(result.output).doesNotContainMatch(
      """
      |WARNING: Allowed .*? is unused
      """.trimMargin().toRegex(),
    )
    assertThat(result.output).doesNotContain("\n\n> Task :licensee")
  }

  @Test fun unusedWarn(
    @TestParameter(
      "coordinate-allow-unused-warn",
      "coordinate-allow-unused-warn-kts",
      "spdx-allow-unused-warn",
      "url-allow-unused-warn",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).build()
    assertExpectedFiles(fixtureDir)
    assertThat(result.output).containsMatch(
      """
      |WARNING: Allowed .*? is unused
      """.trimMargin().toRegex(),
    )
  }

  @Test fun allFixturesCovered() {
    val expectedDirs = javaClass.declaredMethods
      .filter { it.isAnnotationPresent(Test::class.java) }
      .filter { it.parameterCount == 1 } // Assume single parameter means test parameter.
      .flatMap { it.parameters[0].getAnnotation(TestParameter::class.java).value.toList() }
      .sorted()
    val actualDirs = fixturesDir.listFiles()!!
      .filter { it.isDirectory }
      .map { it.name }
      .sorted()
    assertThat(actualDirs).isEqualTo(expectedDirs)
  }

  private fun createRunner(fixtureDir: File): GradleRunner {
    val gradleRoot = File(fixtureDir, "gradle").also { it.mkdir() }
    File("gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
    val androidSdkFile = File("local.properties")
    if (androidSdkFile.exists()) {
      androidSdkFile.copyTo(File(fixtureDir, "local.properties"), overwrite = true)
    }
    return GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withDebug(true) // Run in-process
      .withArguments("clean", "assemble", "licensee", "--stacktrace", "--continue", "--configuration-cache", versionProperty)
      .forwardOutput()
  }

  private fun assertExpectedFiles(fixtureDir: File) {
    val expectedDir = File(fixtureDir, "expected")
    if (!expectedDir.exists()) {
      throw AssertionError("Missing expected/ directory")
    }

    val expectedFiles = expectedDir.walk().filter { it.isFile }.toList()
    assertThat(expectedFiles).isNotEmpty()
    for (expectedFile in expectedFiles) {
      val actualFile = File(fixtureDir, expectedFile.relativeTo(expectedDir).toString())
      if (!actualFile.exists()) {
        throw AssertionError("Expected $actualFile but does not exist")
      }
      assertThat(actualFile.readText()).isEqualTo(expectedFile.readText())
    }
  }
}

private val fixturesDir = File("src/test/fixtures")
private val versionProperty = "-PlicenseeVersion=${System.getProperty("licenseeVersion")!!}"

/**
 * TODO: remove this after https://github.com/willowtreeapps/assertk/issues/515 is fixed.
 */
private fun Assert<CharSequence>.doesNotContainMatch(regex: Regex) = given { actual ->
  if (!regex.containsMatchIn(actual)) return
  expected("to not contain match:${show(regex)} but was:${show(actual)}")
}
