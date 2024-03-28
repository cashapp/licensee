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

import java.io.Serializable
import org.apache.maven.model.Model
import org.apache.maven.model.Scm
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.Category.ENFORCED_PLATFORM
import org.gradle.api.attributes.Category.REGULAR_PLATFORM
import org.gradle.api.logging.Logger

internal data class DependencyConfig(
  val ignoredGroupIds: Map<String, IgnoredData>,
  val ignoredCoordinates: Map<String, Map<String, IgnoredData>>,
) : Serializable

internal data class IgnoredData(
  val reason: String?,
  val transitive: Boolean,
) : Serializable

internal fun loadDependencyCoordinates(
  logger: Logger,
  root: ResolvedComponentResult,
  config: DependencyConfig,
): DependencyResolutionResult {
  val warnings = mutableListOf<String>()

  val unusedGroupIds = config.ignoredGroupIds.keys.toMutableSet()
  val unusedCoordinates = mutableSetOf<Pair<String, String>>()
  for ((groupId, artifacts) in config.ignoredCoordinates) {
    val redundant = groupId in config.ignoredGroupIds
    for (artifactId in artifacts.keys) {
      if (redundant) {
        warnings += "Ignore for $groupId:$artifactId is redundant as $groupId is also ignored"
      } else {
        unusedCoordinates += groupId to artifactId
      }
    }
  }

  val coordinates = mutableSetOf<DependencyCoordinates>()
  loadDependencyCoordinates(logger, root, config, unusedGroupIds, unusedCoordinates, coordinates, mutableSetOf(), depth = 1)

  for (unusedGroupId in unusedGroupIds) {
    warnings += "Dependency ignore for $unusedGroupId is unused"
  }
  for ((groupId, artifactId) in unusedCoordinates) {
    warnings += "Dependency ignore for $groupId:$artifactId is unused"
  }

  return DependencyResolutionResult(coordinates, warnings)
}

internal data class DependencyResolutionResult(
  val coordinates: Set<DependencyCoordinates>,
  val configWarnings: List<String>,
)

internal fun ModuleComponentIdentifier.toDependencyCoordinates() = DependencyCoordinates(group, module, version)

private fun loadDependencyCoordinates(
  logger: Logger,
  root: ResolvedComponentResult,
  config: DependencyConfig,
  unusedGroupIds: MutableSet<String>,
  unusedCoordinates: MutableSet<Pair<String, String>>,
  destination: MutableSet<DependencyCoordinates>,
  seen: MutableSet<ComponentIdentifier>,
  depth: Int,
) {
  val id = root.id

  var processTransitiveDependencies = true
  var ignoreSuffix: String? = null
  when {
    id is ProjectComponentIdentifier -> {
      // Local dependency, do nothing.
      ignoreSuffix = " ignoring because project dependency"
    }

    root.isPlatform() -> {
      // Platform (POM) dependency, do nothing.
      ignoreSuffix = " ignoring because platform dependency"
    }

    id is ModuleComponentIdentifier -> {
      if (id.group == "" && id.version == "") {
        // Assuming flat-dir repository dependency, do nothing.
        ignoreSuffix = " ignoring because flat-dir repository artifact has no metadata"
      } else {
        val ignoredData = null
          ?: config.ignoredGroupIds[id.group]
            ?.also { unusedGroupIds -= id.group }
          ?: config.ignoredCoordinates[id.group]?.get(id.module)
            ?.also { unusedCoordinates -= id.group to id.module }
        if (ignoredData != null) {
          ignoreSuffix = buildString {
            append(" ignoring")
            if (ignoredData.transitive) {
              append(" [transitive=true]")
            }
            if (ignoredData.reason != null) {
              append(" because ")
              append(ignoredData.reason)
            }
          }
          processTransitiveDependencies = !ignoredData.transitive
        } else {
          destination += id.toDependencyCoordinates()
        }
      }
    }

    else -> error("Unknown dependency ${id::class.java}: $id")
  }

  if (logger.isInfoEnabled) {
    logger.info(
      buildString {
        repeat(depth) {
          append("  ")
        }
        append(id)
        if (ignoreSuffix != null) {
          append(ignoreSuffix)
        }
      },
    )
  }

  if (processTransitiveDependencies) {
    for (dependency in root.dependencies) {
      if (dependency is ResolvedDependencyResult) {
        val selected = dependency.selected
        if (seen.add(selected.id)) {
          loadDependencyCoordinates(
            logger,
            selected,
            config,
            unusedGroupIds,
            unusedCoordinates,
            destination,
            seen,
            depth + 1,
          )
        }
      }
    }
  }
}

private fun ResolvedComponentResult.isPlatform(): Boolean {
  val singleVariant = variants.singleOrNull() ?: return false
  // https://github.com/gradle/gradle/issues/8854
  val stringAttribute = Attribute.of(CATEGORY_ATTRIBUTE.name, String::class.java)
  val category = singleVariant.attributes.getAttribute(stringAttribute) ?: return false
  return when (category) {
    ENFORCED_PLATFORM, REGULAR_PLATFORM -> true
    else -> false
  }
}

internal fun loadPomInfo(
  pom: Model,
  getRawModel: (String) -> Model?,
): PomInfo {
  val parentRawModel = pom.parent?.let {
    getRawModel("${it.groupId}:${it.artifactId}:${it.version}")
  }
  val pomScm: Scm? = pom.scm
  val url = if (parentRawModel != null) {
    // https://maven.apache.org/ref/3.6.1/maven-model-builder/
    // When depending on a parent, Maven adds a /artifactId to the url of the child,
    // if the pom file does not opt out of this behavior:
    // <scm child.scm.url.inherit.append.path="false">
    // We don't want to handle the /artifactId, so we use the parent clean url instead.
    val parentScm: Scm? = parentRawModel.scm
    when (parentScm?.childScmUrlInheritAppendPath?.toBoolean()) {
      // No opt-out, use pom first, then parent pom because we don't want to use the /artifactId.
      null -> pomScm?.url?.removeSuffix("/${pom.artifactId}") ?: parentScm?.url
      // Explicit opt-out, so use the parent url.
      false -> parentScm.url
      // Explicit opt-in, the model already appends the path.
      true -> pomScm?.url
    }
  } else {
    pomScm?.url
  }

  return PomInfo(
    name = pom.name ?: parentRawModel?.name,
    licenses = (pom.licenses.takeUnless { it.isEmpty() } ?: parentRawModel?.licenses)?.mapTo(mutableSetOf()) {
      PomLicense(it.name, it.url)
    } ?: emptySet(),
    scm = PomScm(url),
  )
}
