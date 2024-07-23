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

import app.cash.licensee.LicenseeExtension.AllowDependencyOptions
import app.cash.licensee.LicenseeExtension.IgnoreDependencyOptions
import java.util.Optional
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty

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
   *   allowUrl("https://example.com/license.html") {
   *     because 'is Apache-2.0'
   *   }
   * }
   * ```
   */
  fun allowUrl(url: String, options: Action<AllowUrlOptions> = Action { })

  /** @suppress */
  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun allowUrl(url: String) = allowUrl(url, {})

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

  fun allowDependency(
    dependencyProvider: Provider<out Dependency>,
    options: Action<AllowDependencyOptions> = Action { },
  )

  /** @suppress */
  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
  ) {
    allowDependency(groupId, artifactId, version, {})
  }

  /** @suppress */
  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun allowDependency(
    dependencyProvider: Provider<out Dependency>,
  ) {
    allowDependency(dependencyProvider, {})
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
   *     transitive = true
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

  /** @suppress */
  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String) {
    ignoreDependencies(groupId, options = {})
  }

  /** @suppress */
  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String, options: Action<IgnoreDependencyOptions>) {
    ignoreDependencies(groupId = groupId, artifactId = null, options = options)
  }

  /** @suppress */
  @JvmSynthetic // For Groovy build scripts, hide from normal callers.
  fun ignoreDependencies(groupId: String, artifactId: String) {
    ignoreDependencies(groupId, artifactId, {})
  }

  /**
   * Build behavior when a license violation is found.
   *
   * ```
   * licensee {
   *   violationAction(LOG)
   * }
   * ```
   *
   * The default behavior is to [fail][ViolationAction.FAIL].
   *
   * Note: Setting this to [ignore][ViolationAction.IGNORE] does not affect the contents of the
   * `validation.txt` file which always contains all artifacts and their validation status.
   *
   * @see ViolationAction
   */
  fun violationAction(level: ViolationAction)

  /**
   * Build behavior when a declared license is not found in any dependencies of the project.
   *
   * This is intended for the case where you have a restricted list of allowed licenses for your
   * organisation, so you can simply list them all in the Licensee config block and not have to
   * fish through unnecessary warnings in your Gradle logs.
   *
   * ```
   * licensee {
   *   unusedAction(IGNORE)
   * }
   * ```
   *
   * The default behavior is to [warn][UnusedAction.LOG].
   *
   * Note: Setting this to [ignore][UnusedAction.IGNORE] does not affect the contents of the
   * `validation.txt` file which always contains all artifacts and their validation status.
   *
   * @see UnusedAction
   */
  fun unusedAction(level: UnusedAction)

  interface AllowUrlOptions {
    fun because(reason: String)
  }

  interface AllowDependencyOptions {
    fun because(reason: String)
  }

  interface IgnoreDependencyOptions {
    fun because(reason: String)
    var transitive: Boolean
  }
}

@Suppress("unused") // Public API.
enum class ViolationAction {
  FAIL,
  LOG,
  IGNORE,
}

@Suppress("unused") // Public API.
enum class UnusedAction {
  LOG,
  IGNORE,
}

internal abstract class IgnoredCoordinate : Named {
  abstract val ignoredDatas: MapProperty<String, IgnoredData>
}

internal abstract class MutableLicenseeExtension : LicenseeExtension {
  internal abstract val allowedIdentifiers: SetProperty<String>
  internal abstract val allowedUrls: MapProperty<String, Optional<String>>
  internal abstract val allowedDependencies: MapProperty<DependencyCoordinates, Optional<String>>
  internal abstract val ignoredGroupIds: MapProperty<String, IgnoredData>
  internal abstract val ignoredCoordinates: NamedDomainObjectContainer<IgnoredCoordinate>
  internal abstract val violationAction: Property<ViolationAction>
  internal abstract val unusedAction: Property<UnusedAction>

  init {
    violationAction.convention(ViolationAction.FAIL)
    unusedAction.convention(UnusedAction.LOG)
  }

  fun toDependencyTreeConfig(): Provider<DependencyConfig> {
    return ignoredGroupIds.map { ignoredGroupIds ->
      DependencyConfig(
        ignoredGroupIds.toMap(),
        ignoredCoordinates.groupBy({ it.name }) {
          it.ignoredDatas.get()
        }.mapValues {
          it.value.single()
        },
      )
    }
  }

  fun toLicenseValidationConfig(): Provider<ValidationConfig> {
    return allowedIdentifiers.zip(allowedUrls, allowedDependencies) { allowedIdentifiers, allowedUrls, allowedDependencies ->
      ValidationConfig(
        allowedIdentifiers,
        allowedUrls.mapValues {
          it.value.orElse(null)
        },
        allowedDependencies.mapValues {
          it.value.orElse(null)
        },
      )
    }
  }

  override fun allow(spdxId: String) {
    requireNotNull(SpdxId.findByIdentifier(spdxId)) {
      "$spdxId is not a valid SPDX id."
    }
    allowedIdentifiers.add(spdxId)
  }

  override fun allowUrl(url: String, options: Action<LicenseeExtension.AllowUrlOptions>) {
    val option = object : LicenseeExtension.AllowUrlOptions {
      var setReason: String? = null
      override fun because(reason: String) {
        setReason = reason
      }
    }
    options.execute(option)
    allowedUrls.put(url, Optional.ofNullable(option.setReason))
  }

  override fun allowDependency(
    groupId: String,
    artifactId: String,
    version: String,
    options: Action<AllowDependencyOptions>,
  ) {
    val optionsImpl = AllowDependencyOptionsImpl()
    options.execute(optionsImpl)
    allowedDependencies.put(DependencyCoordinates(group = groupId, artifact = artifactId, version = version), Optional.ofNullable(optionsImpl.setReason))
  }

  private class AllowDependencyOptionsImpl : AllowDependencyOptions {
    var setReason: String? = null
    override fun because(reason: String) {
      setReason = reason
    }
  }

  override fun allowDependency(
    dependencyProvider: Provider<out Dependency>,
    options: Action<AllowDependencyOptions>,
  ) {
    val optionsImpl = AllowDependencyOptionsImpl()
    options.execute(optionsImpl)

    allowedDependencies.putAll(
      dependencyProvider.map {
        mapOf(
          DependencyCoordinates(
            group = requireNotNull(it.group) { "group was null in allowDependency for ${it.name}" },
            artifact = it.name,
            version = requireNotNull(it.version) { "version was null in allowDependency for ${it.name}" },
          ) to Optional.ofNullable(optionsImpl.setReason),
        )
      },
    )
  }

  override fun ignoreDependencies(groupId: String, artifactId: String?, options: Action<IgnoreDependencyOptions>) {
    val option = object : IgnoreDependencyOptions {
      var setReason: String? = null
      override fun because(reason: String) {
        setReason = reason
      }

      override var transitive: Boolean = false
    }

    options.execute(option)
    if (option.transitive && option.setReason == null) {
      throw RuntimeException(
        buildString {
          append("Transitive dependency ignore on '")
          append(groupId)
          if (artifactId != null) {
            append(':')
            append(artifactId)
          }
          append("' is dangerous and requires a reason string")
        },
      )
    }
    val ignoredData = IgnoredData(option.setReason, option.transitive)
    if (artifactId == null) {
      ignoredGroupIds.put(groupId, ignoredData)
    } else {
      ignoredCoordinates.configure(groupId) {
        it.ignoredDatas.put(artifactId, ignoredData)
      }
    }
  }

  override fun violationAction(level: ViolationAction) {
    violationAction.set(level)
  }

  override fun unusedAction(level: UnusedAction) {
    unusedAction.set(level)
  }
}

private fun <T, L, R, V> Provider<T>.zip(left: Provider<L>, right: Provider<R>, merge: (T, L, R) -> V): Provider<V> {
  return zip(left) { t, l ->
    t to l
  }.zip(right) { (t, l), r ->
    merge(t, l, r)
  }
}

private fun <T> NamedDomainObjectContainer<T>.configure(name: String, config: Action<T>) {
  if (name in names) {
    named(name, config)
  } else {
    register(name, config)
  }
}
