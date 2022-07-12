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

import app.cash.licensee.ViolationAction.FAIL
import app.cash.licensee.ViolationAction.IGNORE
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel.ERROR
import org.gradle.api.logging.LogLevel.INFO
import org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.logging.LogLevel.WARN
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import kotlin.properties.Delegates

internal open class LicenseeTask : DefaultTask() {
  @get:Input
  lateinit var dependencyConfig: DependencyConfig

  @get:Input
  lateinit var validationConfig: ValidationConfig

  @get:Input
  lateinit var violationAction: ViolationAction

  @get:Classpath
  var classpath: FileCollection by Delegates.notNull()
    private set

  @get:Internal
  var configuration: Configuration by Delegates.notNull()
    private set

  fun setClasspath(configuration: Configuration, usage: String) {
    this.configuration = configuration

    classpath = configuration.incoming.artifactView {
      it.attributes {
        it.attribute(USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, usage))
      }
    }.artifacts.artifactFiles
  }

  @get:OutputDirectory
  lateinit var outputDir: File

  private val _logger: Logger = Logging.getLogger(LicenseeTask::class.java)

  @Internal
  override fun getLogger() = _logger

  @TaskAction
  fun execute() {
    if (logger.isInfoEnabled) {
      logger.info("")
      logger.info("STEP 1: Load dependency coordinates")
      logger.info("")
      logger.info("Ignored:")
      if (dependencyConfig.ignoredGroupIds.isEmpty() && dependencyConfig.ignoredCoordinates.isEmpty()) {
        logger.info("  None")
      } else {
        for ((groupId, ignoreData) in dependencyConfig.ignoredGroupIds) {
          logger.info(
            buildString {
              append("    ")
              append(groupId)
              if (ignoreData.transitive) {
                append(" [transitive=true]")
              }
              if (ignoreData.reason != null) {
                append(" because ")
                append(ignoreData.reason)
              }
            }
          )
        }
        for ((groupId, artifactIds) in dependencyConfig.ignoredCoordinates) {
          for ((artifactId, ignoreData) in artifactIds) {
            logger.info(
              buildString {
                append("    ")
                append(groupId)
                append(':')
                append(artifactId)
                if (ignoreData.transitive) {
                  append(" [transitive=true]")
                }
                if (ignoreData.reason != null) {
                  append(" because ")
                  append(ignoreData.reason)
                }
              }
            )
          }
        }
      }
      logger.info("")
    }
    val resolutionResult = configuration.incoming.resolutionResult
    val rootCoordinate = resolutionResult.root
    val dependencyResult = loadDependencyCoordinates(logger, rootCoordinate, dependencyConfig)
    for (configWarning in dependencyResult.configWarnings) {
      logger.warn("WARNING: $configWarning")
    }

    if (logger.isInfoEnabled) {
      logger.info("")
      logger.info("STEP 2: Load dependency pom info")
      logger.info("")
    }
    val coordinatesToPomInfo =
      dependencyResult.coordinates.associateWith { loadPomInfo(project, logger, it, rootCoordinate.variants) }

    if (logger.isInfoEnabled) {
      logger.info("")
      logger.info("STEP 3: Normalize license information")
      logger.info("")
    }
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
          }
        )
      }
    }

    val artifactsJson = outputFormat.encodeToString(listOfArtifactDetail, artifactDetails)

    val artifactsJsonFile = File(outputDir, "artifacts.json")
    artifactsJsonFile.parentFile.mkdirs()
    artifactsJsonFile.writeText(artifactsJson)
    if (!artifactsJson.endsWith("\n")) {
      // Force a trailing newline because it makes editing expected files easier.
      artifactsJsonFile.appendText("\n")
    }

    if (logger.isInfoEnabled) {
      logger.info("")
      logger.info("STEP 4: Validate license information")
      logger.info("")
      logger.info("Allowed identifiers:")
      logger.info(
        validationConfig.allowedIdentifiers.ifEmpty { listOf("None") }.joinToString(prefix = "  ")
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
            }
          )
        }
      }
      logger.info("")
    }
    val validationResult = validateArtifacts(validationConfig, artifactDetails)

    val validationReport = StringBuilder()

    val errorLevel = if (violationAction == IGNORE) INFO else ERROR
    val warningLevel = if (violationAction == IGNORE) INFO else WARN
    val lifecycleLevel = if (violationAction == IGNORE) INFO else LIFECYCLE

    fun logResult(configResult: ValidationResult, prefix: String = "") {
      when (configResult) {
        is ValidationResult.Error -> {
          val message = prefix + "ERROR: " + configResult.message
          validationReport.appendLine(message)
          logger.log(errorLevel, message)
        }
        is ValidationResult.Warning -> {
          val message = prefix + "WARNING: " + configResult.message
          validationReport.appendLine(message)
          logger.log(warningLevel, message)
        }
        is ValidationResult.Info -> {
          val message = prefix + configResult.message
          validationReport.appendLine(message)
          logger.info(message)
        }
      }
    }
    for (configResult in validationResult.configResults) {
      logResult(configResult)
    }
    if (validationResult.configResults.isNotEmpty() && validationResult.artifactResults.isNotEmpty()) {
      validationReport.appendLine()
      // We know these are always at warning or error level, so use lifecycle for space.
      logger.log(lifecycleLevel, "")
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
        logResult(result, prefix = " - ")
      }
    }

    val validationReportFile = File(outputDir, "validation.txt")
    validationReportFile.parentFile.mkdirs()
    validationReportFile.writeText(validationReport.toString())

    if (violationAction == FAIL && validationResult.containsErrors) {
      throw RuntimeException("Artifacts failed validation. See output above.")
    }
  }
}

internal data class PomInfo(
  val name: String?,
  val licenses: Set<PomLicense>,
  val scm: PomScm?,
)

internal data class PomLicense(
  val name: String?,
  val url: String?,
)

internal data class PomScm(
  val url: String?,
)

private val outputFormat = Json { prettyPrint = true }
private val listOfArtifactDetail = ListSerializer(ArtifactDetail.serializer())
