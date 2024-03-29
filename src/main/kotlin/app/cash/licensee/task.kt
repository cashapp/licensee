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

import java.io.File
import java.io.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.apache.maven.model.Dependency
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.DefaultModelBuilderFactory
import org.apache.maven.model.building.DefaultModelBuildingRequest
import org.apache.maven.model.building.FileModelSource
import org.apache.maven.model.building.ModelBuildingRequest
import org.apache.maven.model.building.ModelSource2
import org.apache.maven.model.resolution.ModelResolver
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.LogLevel.ERROR
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.logging.LogLevel.WARN
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class LicenseeTask : DefaultTask() {
  @get:Input
  internal abstract val dependencyConfig: Property<DependencyConfig>

  @get:Input
  internal abstract val validationConfig: Property<ValidationConfig>

  @get:Input
  internal abstract val violationAction: Property<ViolationAction>

  @get:Input
  internal abstract val unusedAction: Property<UnusedAction>

  @get:Input
  internal abstract val coordinatesToPomInfo: MapProperty<DependencyCoordinates, PomInfo>

  fun configurationToCheck(configuration: Configuration) {
    loadDependenciesFromConfiguration(configuration.incoming.resolutionResult.rootComponent)
  }

  fun configurationToCheck(configuration: Provider<Configuration>) {
    loadDependenciesFromConfiguration(configuration.flatMap { it.incoming.resolutionResult.rootComponent })
  }

  private fun loadDependenciesFromConfiguration(root: Provider<ResolvedComponentResult>) {
    val dependencies = project.dependencies
    val configurations = project.configurations
    val pomInfos: Provider<Map<DependencyCoordinates, PomInfo>> = root.zip(dependencyConfig) { root, depConfig ->
      val directDependencies = loadDependencyCoordinates(
        logger,
        root,
        depConfig,
      )
      val directPomFiles = directDependencies.coordinates.fetchPomFiles(
        root.variants,
        dependencies,
        configurations,
      )
      directPomFiles.getPomInfo(
        root.variants,
        dependencies,
        configurations,
      )
    }

    this.coordinatesToPomInfo.set(pomInfos)
  }

  private fun Iterable<DependencyCoordinatesWithPomFile>.getPomInfo(
    variants: List<ResolvedVariantResult>,
    dependencies: DependencyHandler,
    configurations: ConfigurationContainer,
  ): Map<DependencyCoordinates, PomInfo> {
    val builder = DefaultModelBuilderFactory().newInstance()
    val resolver = object : ModelResolver {
      fun resolve(dependencyCoordinates: DependencyCoordinates): FileModelSource {
        val pomFile =
          setOf(dependencyCoordinates).fetchPomFiles(
            variants,
            dependencies,
            configurations,
          ).single().pomFile
        return FileModelSource(pomFile)
      }

      override fun resolveModel(groupId: String, artifactId: String, version: String): ModelSource2 =
        resolve(DependencyCoordinates(groupId, artifactId, version))

      override fun resolveModel(parent: Parent): ModelSource2 =
        resolve(DependencyCoordinates(parent.groupId, parent.artifactId, parent.version))

      override fun resolveModel(dependency: Dependency): ModelSource2 =
        resolve(DependencyCoordinates(dependency.groupId, dependency.artifactId, dependency.version))

      override fun addRepository(repository: Repository) { }
      override fun addRepository(repository: Repository, replace: Boolean) { }
      override fun newCopy(): ModelResolver = this
    }

    return associate { (coordinates, file) ->
      val req = DefaultModelBuildingRequest().apply {
        isProcessPlugins = false
        pomFile = file
        isTwoPhaseBuilding = true
        modelResolver = resolver
        validationLevel = ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL
      }
      val result = builder.build(req)
      coordinates to loadPomInfo(result.effectiveModel) { modelId ->
        result.getRawModel(modelId)
      }
    }
  }

  private fun Set<DependencyCoordinates>.fetchPomFiles(
    variants: List<ResolvedVariantResult>,
    dependencies: DependencyHandler,
    configurations: ConfigurationContainer,
  ): List<DependencyCoordinatesWithPomFile> {
    val pomDependencies = map {
      dependencies.create(it.pomCoordinate())
    }.toTypedArray()

    val withVariants = configurations.detachedConfiguration(*pomDependencies).apply {
      for (variant in variants) {
        attributes {
          val variantAttrs = variant.attributes
          for (attrs in variantAttrs.keySet()) {
            @Suppress("UNCHECKED_CAST")
            it.attribute(attrs as Attribute<Any?>, variantAttrs.getAttribute(attrs)!!)
          }
        }
      }
    }.artifacts()

    val withoutVariants = configurations.detachedConfiguration(*pomDependencies).artifacts()

    return (withVariants + withoutVariants).map {
      // Cast is safe because all resolved artifacts are pom files.
      val coordinates = (it.id.componentIdentifier as ModuleComponentIdentifier).toDependencyCoordinates()
      DependencyCoordinatesWithPomFile(coordinates, it.file)
    }.distinctBy { it.dependencyCoordinates }
  }

  private fun Configuration.artifacts() = resolvedConfiguration.lenientConfiguration.allModuleDependencies.flatMap { it.allModuleArtifacts }

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @Internal
  val jsonOutput: Provider<RegularFile> = outputDir.file("artifacts.json")

  @Internal
  val validationOutput: Provider<RegularFile> = outputDir.file("validation.txt")

  private val logger: Logger = Logging.getLogger(LicenseeTask::class.java)

  @Internal
  override fun getLogger() = logger

  @TaskAction
  fun execute() {
    if (logger.isInfoEnabled) {
      logger.info("")
      logger.info("STEP 1: Normalize license information")
      logger.info("")
    }
    val coordinatesToPomInfo = coordinatesToPomInfo.get()
    val artifactDetails = normalizeLicenseInfo(coordinatesToPomInfo)
    if (logger.isInfoEnabled) {
      for (artifactDetail in artifactDetails) {
        logger.info(
          buildString {
            append(artifactDetail.groupId)
            append(':')
            append(artifactDetail.artifactId)
            append(':')
            append(artifactDetail.version)
            append(' ')
            append(artifactDetail.spdxLicenses)
            append(' ')
            append(artifactDetail.unknownLicenses)
          },
        )
      }
    }

    val artifactsJson = outputFormat.encodeToString(listOfArtifactDetail, artifactDetails)

    val artifactsJsonFile = jsonOutput.get().asFile
    artifactsJsonFile.writeText(artifactsJson)
    if (!artifactsJson.endsWith("\n")) {
      // Force a trailing newline because it makes editing expected files easier.
      artifactsJsonFile.appendText("\n")
    }

    val validationConfig = validationConfig.get()
    if (logger.isInfoEnabled) {
      logger.info("")
      logger.info("STEP 2: Validate license information")
      logger.info("")
      logger.info("Allowed identifiers:")
      logger.info(
        validationConfig.allowedIdentifiers.ifEmpty { listOf("None") }.joinToString(prefix = "  "),
      )
      logger.info("Allowed URLs:")
      if (validationConfig.allowedUrls.isEmpty()) {
        logger.info("  None")
      } else {
        for (allowedUrl in validationConfig.allowedUrls) {
          logger.info("  $allowedUrl")
        }
      }
      logger.info("Allowed coordinates:")
      if (validationConfig.allowedCoordinates.isEmpty()) {
        logger.info("  None")
      } else {
        for ((coordinate, reason) in validationConfig.allowedCoordinates) {
          logger.info(
            buildString {
              append("  ")
              append(coordinate.group)
              append(':')
              append(coordinate.artifact)
              append(':')
              append(coordinate.version)
              if (reason != null) {
                append(" because ")
                append(reason)
              }
            },
          )
        }
      }
      logger.info("")
    }

    val validationResult = validateArtifacts(validationConfig, artifactDetails)

    val validationReport = StringBuilder()

    val violationAction = violationAction.get()
    val violationErrorLevel = if (violationAction == ViolationAction.IGNORE) INFO else ERROR
    val violationWarningLevel = if (violationAction == ViolationAction.IGNORE) INFO else WARN

    val unusedAction = unusedAction.get()
    val unusedErrorLevel = if (unusedAction == UnusedAction.IGNORE) INFO else ERROR
    val unusedWarningLevel = if (unusedAction == UnusedAction.IGNORE) INFO else WARN

    val lifecycleLevel = if (violationAction == ViolationAction.IGNORE) INFO else LIFECYCLE

    fun logResult(configResult: ValidationResult, error: LogLevel, warning: LogLevel, prefix: String = "") {
      when (configResult) {
        is ValidationResult.Error -> {
          val message = prefix + "ERROR: " + configResult.message
          validationReport.appendLine(message)
          logger.log(error, message)
        }

        is ValidationResult.Warning -> {
          val message = prefix + "WARNING: " + configResult.message
          validationReport.appendLine(message)
          logger.log(warning, message)
        }

        is ValidationResult.Info -> {
          val message = prefix + configResult.message
          validationReport.appendLine(message)
          logger.info(message)
        }
      }
    }
    for (configResult in validationResult.configResults) {
      logResult(configResult, unusedErrorLevel, unusedWarningLevel)
    }
    if (validationResult.configResults.isNotEmpty() && validationResult.artifactResults.isNotEmpty()) {
      validationReport.appendLine()
      // We know these are always at warning or error level, so use lifecycle for space.
      if (unusedWarningLevel > INFO || unusedErrorLevel > INFO || logger.isInfoEnabled) {
        logger.log(lifecycleLevel, "")
      }
    }
    for ((artifactDetail, results) in validationResult.artifactResults) {
      val coordinateHeader = buildString {
        append(artifactDetail.groupId)
        append(':')
        append(artifactDetail.artifactId)
        append(':')
        append(artifactDetail.version)
      }
      validationReport.appendLine(coordinateHeader)

      val hasNonInfo = results.any { it !is ValidationResult.Info }
      if (hasNonInfo) {
        logger.log(lifecycleLevel, coordinateHeader)
      } else {
        logger.info(coordinateHeader)
      }
      for (result in results) {
        logResult(result, violationErrorLevel, violationWarningLevel, prefix = " - ")
      }
    }

    val validationReportFile = validationOutput.get().asFile
    validationReportFile.writeText(validationReport.toString())

    if (violationAction == ViolationAction.FAIL && validationResult.containsErrors) {
      throw RuntimeException("Artifacts failed validation. See output above.")
    }
  }
}

private data class DependencyCoordinatesWithPomFile(
  val dependencyCoordinates: DependencyCoordinates,
  val pomFile: File,
)

internal data class PomInfo(
  val name: String?,
  val licenses: Set<PomLicense>,
  val scm: PomScm?,
) : Serializable

internal data class PomLicense(
  val name: String?,
  val url: String?,
) : Serializable

internal data class PomScm(
  val url: String?,
) : Serializable

private val outputFormat = Json { prettyPrint = true }
private val listOfArtifactDetail = ListSerializer(ArtifactDetail.serializer())
