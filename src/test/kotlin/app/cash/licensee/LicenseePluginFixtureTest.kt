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

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(TestParameterInjector::class)
class LicenseePluginFixtureTest {
  @Test fun success(
    @TestParameter(
      "artifact-with-classifier",
      "artifact-with-extension",
      "compile-only-ignored",
      "coordinate-allow-unused",
      "coordinate-allowed",
      "coordinate-allowed-override-spdx",
      "coordinate-allowed-override-url",
      "coordinate-allowed-with-reason",
      "dependency-substitution-replace-local-with-remote",
      "dependency-substitution-replace-remote-with-local-ignored",
      "dependency-substitution-replace-remote-with-include-build-ignored",
      "exclude-ignored",
      "ignore-group",
      "ignore-group-artifact",
      "ignore-group-artifact-transitive",
      "ignore-group-transitive",
      "implementation",
      "local-file-ignored",
      "local-file-tree-ignored",
      "pom-with-inheritance-from-both",
      "pom-with-inheritance-from-child",
      "pom-with-inheritance-from-parent",
      "project-android-ignored",
      "project-java-ignored",
      "repository-include-exclude",
      "spdx-allow-unused",
      "spdx-allowed",
      "spdx-allowed-but-no-match",
      "transitive-android-project-api",
      "transitive-android-project-implementation",
      "transitive-java-project-api",
      "transitive-java-project-implementation",
      "url-allow-unused",
      "url-allowed",
      "url-allowed-but-is-spdx",
      "url-allowed-but-no-match",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    createRunner(fixtureDir).build()
    assertExpectedFiles(fixtureDir)

    // Ensure up-to-date functionality works.
    val secondRun = GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withArguments("licensee", "--stacktrace", versionProperty)
      .forwardOutput()
      .build()
    secondRun.tasks.filter { it.path.contains(":licensee") }.forEach {
      assertEquals("Second invocation of ${it.path}", UP_TO_DATE, it.outcome)
    }
  }

  @Test fun failure(
    @TestParameter(
      "coordinate-version-mismatch",
      "no-license-not-allowed",
      "spdx-not-allowed",
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
      "ignore-group-transitive-requires-reason",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).containsMatch(
      "Transitive dependency ignore on 'com\\.example(:example)?' is dangerous and requires a reason string"
    )
  }

  @Test fun unsupportedPlugin(
    @TestParameter(
      "android-library-plugin-fails",
      "java-library-plugin-fails",
      "java-plugin-fails",
      "no-plugin-fails",
    ) fixtureName: String,
  ) {
    val fixtureDir = File(fixturesDir, fixtureName)
    val result = createRunner(fixtureDir).buildAndFail()
    assertThat(result.output).contains(
      "'app.cash.licensee' plugin only works with 'com.android.application' plugin"
    )
  }

  @Test fun allFixturesCovered() {
    val expectedDirs = javaClass.declaredMethods
      .filter { it.isAnnotationPresent(Test::class.java) }
      .filter { it.parameterCount == 1 } // Assume single parameter means test parameter.
      .flatMap { it.parameters[0].getAnnotation(TestParameter::class.java).value.toList() }
    val actualDirs = fixturesDir.listFiles().filter { it.isDirectory }.map { it.name }
    assertThat(expectedDirs).containsExactlyElementsIn(actualDirs)
  }

  private fun createRunner(fixtureDir: File): GradleRunner {
    val gradleRoot = File(fixtureDir, "gradle").also { it.mkdir() }
    File("gradle/wrapper").copyRecursively(File(gradleRoot, "wrapper"), true)
    return GradleRunner.create()
      .withProjectDir(fixtureDir)
      .withArguments("clean", "assemble", "licensee", "--stacktrace", "--continue", versionProperty)
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
