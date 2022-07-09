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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.LenientConfiguration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.logging.Logger
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
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
    is ProjectComponentIdentifier -> {
      // Local dependency, do nothing.
      ignoreSuffix = " ignoring because project dependency"
    }
    is ModuleComponentIdentifier -> {
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
          destination += DependencyCoordinates(id.group, id.module, id.version)
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

private fun Project.pomConfiguration(
  pomDependency: Dependency,
  variants: List<ResolvedVariantResult>
) = configurations
  .detachedConfiguration(pomDependency)
  .apply {
    for (variant in variants) {
      attributes {
        val variantAttrs = variant.attributes
        for (attrs in variantAttrs.keySet()) {
          @Suppress("UNCHECKED_CAST")
          it.attribute(attrs as Attribute<Any?>, variantAttrs.getAttribute(attrs)!!)
        }
      }
    }
    // See https://docs.gradle.org/current/userguide/dependency_verification.html#sub:disabling-specific-verification.
    resolutionStrategy.disableDependencyVerification()
  }
  .resolvedConfiguration
  .lenientConfiguration

private val LenientConfiguration.resolvedFiles get() = allModuleDependencies
  .flatMap { it.allModuleArtifacts }
  .map { it.file }

private fun getPomFile(
  project: Project,
  pomCoordinates: String,
  variants: List<ResolvedVariantResult>
): Pair<LenientConfiguration, List<File>> {
  val pomDependency = project.dependencies.create(pomCoordinates)

  val pomConfigurationWithoutVariants = project.pomConfiguration(
    pomDependency = pomDependency,
    variants = emptyList()
  )
  val resolvedFilesWithoutVariants = pomConfigurationWithoutVariants.resolvedFiles

  return if (resolvedFilesWithoutVariants.isNotEmpty()) {
    pomConfigurationWithoutVariants to resolvedFilesWithoutVariants
  } else {
    val pomConfigurationWithVariants = project.pomConfiguration(
      pomDependency = pomDependency,
      variants = variants
    )
    pomConfigurationWithVariants to pomConfigurationWithVariants.resolvedFiles
  }
}

internal fun loadPomInfo(
  project: Project,
  logger: Logger,
  id: DependencyCoordinates,
  variants: List<ResolvedVariantResult>,
  depth: Int = 0,
): PomInfo {
  val pomCoordinates = with(id) { "$group:$artifact:$version@pom" }
  val (pomConfiguration, resolvedFiles) = getPomFile(
    project = project,
    pomCoordinates = pomCoordinates,
    variants = variants
  )

  if (logger.isInfoEnabled) {
    logger.info(
      buildString {
        repeat(depth) {
          append("  ")
        }
        append(pomCoordinates)
        append(' ')
        append(resolvedFiles)
      },
      pomConfiguration.unresolvedModuleDependencies.singleOrNull()?.problem,
    )
  }

  if (resolvedFiles.isEmpty()) {
    return PomInfo(null, emptySet(), null)
  }

  val factory = DocumentBuilderFactory.newInstance()
  val pomDocument = factory.newDocumentBuilder().parse(resolvedFiles.single())

  var licensesNode: Node? = null
  var scmNode: Node? = null
  var parentNode: Node? = null
  var artifactName: String? = null
  for (childNode in pomDocument.documentElement.childNodes) {
    when (childNode.nodeName) {
      "licenses" -> licensesNode = childNode
      "scm" -> scmNode = childNode
      "parent" -> parentNode = childNode
      "name" -> artifactName = childNode.textContent
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

  var scm: PomScm? = null
  if (scmNode != null) {
    for (propertyNode in scmNode.childNodes) {
      if (propertyNode.nodeName == "url") {
        scm = PomScm(propertyNode.textContent)
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
        variants = variants,
        depth = depth + 1
      )
      if (licenses.isEmpty()) {
        licenses += parentPomInfo.licenses
      }
      if (scm == null) {
        scm = parentPomInfo.scm
      }
      if (artifactName == null) {
        artifactName = parentPomInfo.name
      }
    }
  }

  return PomInfo(artifactName, licenses, scm)
}

private operator fun NodeList.iterator(): Iterator<Node> = iterator {
  for (i in 0 until length) {
    yield(item(i))
  }
}
