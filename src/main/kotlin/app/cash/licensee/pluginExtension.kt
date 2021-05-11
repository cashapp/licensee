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
   * Allow artifacts with a license which matches a SPDX identifier.
   *
   * ```
   * licensee {
   *   allowIdentifier("Apache-2.0")
   * }
   * ```
   *
   * A full list of identifiers is available at [https://spdx.org/licenses/].
   *
   */
  fun allowIdentifier(id: String)

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

  fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
    options: Action<AllowDependencyOptions>,
  )

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
    options: Closure<AllowDependencyOptions>,
  ) {
    allowDependency(groupId, artifactId, version, ClosureBackedAction.of(options))
  }

  fun allowDependency(groupId: String, artifactId: String, version: String) {
    allowDependency(groupId, artifactId, version, {})
  }

  fun ignoreDependencies(
    groupId: String,
    artifactId: String?,
    options: Action<IgnoreDependencyOptions>,
  )

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(
    groupId: String,
    artifactId: String?,
    options: Closure<IgnoreDependencyOptions>,
  ) {
    ignoreDependencies(groupId, artifactId, ClosureBackedAction.of(options))
  }

  fun ignoreDependencies(groupId: String) {
    ignoreDependencies(groupId, null, {})
  }

  fun ignoreDependencies(groupId: String, options: Action<IgnoreDependencyOptions>) {
    ignoreDependencies(groupId, null, options)
  }

  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String, options: Closure<IgnoreDependencyOptions>) {
    ignoreDependencies(groupId, null, ClosureBackedAction.of(options))
  }

  fun ignoreDependencies(groupId: String, artifactId: String?) {
    ignoreDependencies(groupId, artifactId, {})
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

  override fun allowIdentifier(id: String) {
    allowedIdentifiers += id
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
    var setTransitive: Boolean = false
    options.execute(object : IgnoreDependencyOptions {
      override fun because(reason: String) {
        setReason = reason
      }

      override fun transitive(transitive: Boolean) {
        setTransitive = transitive
      }
    })
    val ignoredData = IgnoredData(setReason, setTransitive)
    if (artifactId == null) {
      ignoredGroupIds[groupId] = ignoredData
    } else {
      ignoredCoordinates.getOrPut(groupId, ::LinkedHashMap)[artifactId] = ignoredData
    }
  }
}
