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

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.logging.Logger
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.Serializable
import javax.xml.parsers.DocumentBuilderFactory

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
  when (id) {
    is ProjectComponentIdentifier -> {} // Local dependency, do nothing
    is ModuleComponentIdentifier -> {
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
        destination += DependencyCoordinates(id.group, id.module, id.version)
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
      }
    )
  }

  if (processTransitiveDependencies) {
    for (dependency in root.dependencies) {
      if (dependency is ResolvedDependencyResult) {
        val selected = dependency.selected
        if (seen.add(selected.id)) {
          loadDependencyCoordinates(
            logger, selected, config, unusedGroupIds, unusedCoordinates, destination, seen, depth + 1
          )
        }
      }
    }
  }
}

internal fun loadPomInfo(
  project: Project,
  logger: Logger,
  id: DependencyCoordinates,
  depth: Int = 0,
): PomInfo {
  val pomCoordinates = with(id) { "$group:$artifact:$version@pom" }
  if (logger.isInfoEnabled) {
    logger.info(
      buildString {
        repeat(depth) {
          append("  ")
        }
        append(pomCoordinates)
      }
    )
  }

  val pomDependency = project.dependencies.create(pomCoordinates)
  val pomConfiguration = project.configurations.detachedConfiguration(pomDependency)
  val pomFile =
    pomConfiguration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.flatMap { it.allModuleArtifacts }.singleOrNull()?.file
  if (pomFile == null) {
    return PomInfo(emptySet())
  }

  val factory = DocumentBuilderFactory.newInstance()
  val pomDocument = factory.newDocumentBuilder().parse(pomFile)

  var licensesNode: Node? = null
  var parentNode: Node? = null
  for (childNode in pomDocument.documentElement.childNodes) {
    when (childNode.nodeName) {
      "licenses" -> licensesNode = childNode
      "parent" -> parentNode = childNode
    }
  }

  val licenses = mutableSetOf<PomLicense>()
  if (licensesNode != null) {
    for (licenseNode in licensesNode.childNodes) {
      if (licenseNode.nodeName == "license") {
        var name: String? = null
        var url: String? = null
        for (propertyNode in licenseNode.childNodes) {
          when (propertyNode.nodeName) {
            "name" -> name = propertyNode.textContent
            "url" -> url = propertyNode.textContent
          }
        }
        if (name != null || url != null) {
          licenses += PomLicense(name, url)
        }
      }
    }
  }
  if (parentNode != null) {
    var group: String? = null
    var artifact: String? = null
    var version: String? = null
    for (propertyNode in parentNode.childNodes) {
      when (propertyNode.nodeName) {
        "groupId" -> group = propertyNode.textContent
        "artifactId" -> artifact = propertyNode.textContent
        "version" -> version = propertyNode.textContent
      }
    }
    if (group != null && artifact != null && version != null) {
      val parentPomInfo = loadPomInfo(
        project = project,
        logger = logger,
        id = DependencyCoordinates(group, artifact, version),
        depth = depth + 1
      )
      licenses += parentPomInfo.licenses
    }
  }

  return PomInfo(licenses)
}

private operator fun NodeList.iterator(): Iterator<Node> = iterator {
  for (i in 0 until length) {
    yield(item(i))
  }
}
