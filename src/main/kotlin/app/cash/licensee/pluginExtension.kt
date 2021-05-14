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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.util.ClosureBackedAction

@Suppress("unused") // Public API for Gradle build scripts.
interface LicenseeExtension {
  /**
   * Allow artifacts with a license that matches a SPDX identifier.
   *
   * ```
   * licensee {
   *   allow("Apache-2.0")
   * }
   * ```
   *
   * A full list of supported identifiers is available at [https://spdx.org/licenses/].
   */
  fun allow(spdxId: String)

  /**
   * Allow artifacts with an unknown (non-SPDX) license which matches a URL.
   *
   * ```
   * licensee {
   *   allowUrl("https://example.com/license.html")
   * }
   * ```
   */
  fun allowUrl(url: String)

  /**
   * Allow an artifact with a specific groupId, artifactId, and version.
   * This is useful for artifacts which contain no license data or have invalid/incorrect license data.
   *
   * ```groovy
   * licensee {
   *   allowDependency('com.example', 'example', '1.0')
   * }
   * ```
   *
   * A reason string can be supplied to document why the dependency is being allowed despite missing or invalid license data.
   *
   * ```groovy
   * licensee {
   *   allowDependency('com.jetbrains', 'annotations', '16.0.1') {
   *     because 'Apache-2.0, but typo in license URL fixed in newer versions'
   *   }
   * }
   * ```
   *
   * Reason strings will be included in validation reports.
   */
  fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
    options: Action<AllowDependencyOptions> = Action { },
  )

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
  ) {
    allowDependency(groupId, artifactId, version, {})
  }

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
    options: Closure<AllowDependencyOptions>,
  ) {
    allowDependency(groupId, artifactId, version, ClosureBackedAction.of(options))
  }

  /**
   *
   * Ignore a single dependency or group of dependencies during dependency graph resolution.
   * Artifacts targeted with this method will not be analyzed for license information and will not show up in any report files.
   *
   * This function can be used to ignore internal, closed-source libraries and commercial libraries for which you've purchased a license.
   *
   * There are overloads which accept either a groupId or a groupId:artifactId pair.
   *
   * ```groovy
   * licensee {
   *   ignoreDependencies('com.mycompany.internal')
   *   ignoreDependencies('com.mycompany.utils', 'utils')
   * }
   * ```
   *
   * A reason string can be supplied to document why the dependencies are being ignored.
   *
   * ```groovy
   * licensee {
   *   ignoreDependencies('com.example.sdk', 'sdk') {
   *     because "commercial SDK"
   *   }
   * }
   * ```
   *
   * An ignore can be marked as transitive which will ignore an entire branch of the dependency tree.
   * This will ignore the target artifact's dependencies regardless of the artifact coordinates or license info.
   * Since it is especially dangerous, a reason string is required.
   *
   * ```groovy
   * licensee {
   *   ignoreDependencies('com.other.sdk', 'sdk') {
   *     transitive true
   *     because "commercial SDK"
   *   }
   * }
   * ```
   */
  fun ignoreDependencies(
    groupId: String,
    artifactId: String? = null,
    options: Action<IgnoreDependencyOptions> = Action { },
  )

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String) {
    ignoreDependencies(groupId, options = {})
  }

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String, options: Closure<IgnoreDependencyOptions>) {
    ignoreDependencies(groupId, options = ClosureBackedAction.of(options))
  }

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String, artifactId: String) {
    ignoreDependencies(groupId, artifactId, {})
  }

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(
    groupId: String,
    artifactId: String,
    options: Closure<IgnoreDependencyOptions>,
  ) {
    ignoreDependencies(groupId, artifactId, ClosureBackedAction.of(options))
  }
}

interface AllowDependencyOptions {
  fun because(reason: String)
}

interface IgnoreDependencyOptions {
  fun because(reason: String)
  fun transitive(transitive: Boolean)
}

internal class MutableLicenseeExtension : LicenseeExtension {
  private val allowedIdentifiers = mutableSetOf<String>()
  private val allowedUrls = mutableSetOf<String>()
  private val allowedDependencies = mutableMapOf<DependencyCoordinates, String?>()
  private val ignoredGroupIds = mutableMapOf<String, IgnoredData>()
  private val ignoredCoordinates = mutableMapOf<String, MutableMap<String, IgnoredData>>()

  fun toDependencyTreeConfig(): DependencyConfig {
    return DependencyConfig(
      ignoredGroupIds.toMap(),
      ignoredCoordinates.mapValues { it.value.toMap() },
    )
  }

  fun toLicenseValidationConfig(): ValidationConfig {
    return ValidationConfig(
      allowedIdentifiers.toSet(),
      allowedUrls.toSet(),
      allowedDependencies.toMap(),
    )
  }

  override fun allow(spdxId: String) {
    allowedIdentifiers += spdxId
  }

  override fun allowUrl(url: String) {
    allowedUrls += url
  }

  override fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
    options: Action<AllowDependencyOptions>,
  ) {
    var setReason: String? = null
    options.execute(object : AllowDependencyOptions {
      override fun because(reason: String) {
        setReason = reason
      }
    })
    allowedDependencies[DependencyCoordinates(groupId, artifactId, version)] = setReason
  }

  override fun ignoreDependencies(
    groupId: String,
    artifactId: String?,
    options: Action<IgnoreDependencyOptions>,
  ) {
    var setReason: String? = null
    var setTransitive = false
    options.execute(object : IgnoreDependencyOptions {
      override fun because(reason: String) {
        setReason = reason
      }

      override fun transitive(transitive: Boolean) {
        setTransitive = transitive
      }
    })

    if (setTransitive && setReason == null) {
      throw RuntimeException(
        buildString {
          append("Transitive dependency ignore on '")
          append(groupId)
          if (artifactId != null) {
            append(':')
            append(artifactId)
          }
          append("' is dangerous and requires a reason string")
        }
      )
    }

    val ignoredData = IgnoredData(setReason, setTransitive)
    if (artifactId == null) {
      ignoredGroupIds[groupId] = ignoredData
    } else {
      ignoredCoordinates.getOrPut(groupId, ::LinkedHashMap)[artifactId] = ignoredData
    }
  }
}
