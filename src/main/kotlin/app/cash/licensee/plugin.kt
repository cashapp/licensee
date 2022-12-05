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

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import java.io.File
import java.util.Locale.ROOT
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common

private const val baseTaskName = "licensee"
private const val reportFolder = "licensee"

@Suppress("unused") // Instantiated reflectively by Gradle.
class LicenseePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    require(GradleVersion.current() >= GradleVersion.version("7.4")) {
      "Licensee plugin requires Gradle 7.4 or later"
    }

    val extension = project.objects.newInstance(MutableLicenseeExtension::class.java)
    project.extensions.add(LicenseeExtension::class.java, "licensee", extension)

    project.afterEvaluate {
      val androidPlugin = if (project.plugins.hasPlugin("com.android.application")) {
        AndroidPlugin.Application
      } else if (project.plugins.hasPlugin("com.android.library")) {
        AndroidPlugin.Library
      } else if (project.plugins.hasPlugin("com.android.dynamic-feature")) {
        AndroidPlugin.DynamicFeature
      } else {
        null
      }

      var rootTask: TaskProvider<Task>? = null
      if (project.plugins.hasPlugin("org.jetbrains.kotlin.js")) {
        // The JS plugin uses the same runtime configuration name as the Java plugin.
        configureJavaPlugin(project, extension)
      } else if (project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        rootTask = project.tasks.register(baseTaskName) {
          it.group = VERIFICATION_GROUP
          it.description = taskDescription("all Kotlin targets")
        }
        if (androidPlugin != null) {
          configureKotlinMultiplatformTargets(project, extension, rootTask, skipAndroid = true)
          configureAndroidVariants(project, extension, rootTask, androidPlugin, prefix = true)
        } else {
          configureKotlinMultiplatformTargets(project, extension, rootTask)
        }
      } else if (project.plugins.hasPlugin("java")) {
        // Note: java-library applies java so we only need to look for the latter.
        // Note: org.jetbrains.kotlin.jvm applies java so we only need to look for the latter.
        configureJavaPlugin(project, extension)
      } else if (androidPlugin != null) {
        rootTask = project.tasks.register(baseTaskName) {
          it.group = VERIFICATION_GROUP
          it.description = taskDescription("all Android variants")
        }
        configureAndroidVariants(project, extension, rootTask, androidPlugin)
      } else {
        val name = if (project === project.rootProject) {
          "root project"
        } else {
          "project ${project.path}"
        }
        throw IllegalStateException(
          "'app.cash.licensee' requires compatible language/platform plugin to be applied ($name)",
        )
      }

      if (rootTask != null) {
        project.tasks.named(CHECK_TASK_NAME).configure {
          it.dependsOn(rootTask)
        }
      }
    }
  }
}

private enum class AndroidPlugin {
  Application,
  Library,
  DynamicFeature,
}

private fun configureAndroidVariants(
  project: Project,
  extension: MutableLicenseeExtension,
  rootTask: TaskProvider<Task>,
  android: AndroidPlugin,
  prefix: Boolean = false,
) {
  val extensions = project.extensions
  val variants = when (android) {
    AndroidPlugin.Application, AndroidPlugin.DynamicFeature -> extensions.getByType(AppExtension::class.java).applicationVariants
    AndroidPlugin.Library -> extensions.getByType(LibraryExtension::class.java).libraryVariants
  }
  variants.configureEach { variant ->
    val suffix = variant.name.capitalize(ROOT)
    val taskName = buildString {
      append(baseTaskName)
      if (prefix) {
        append("Android")
      }
      append(suffix)
    }
    val task = project.tasks.register(taskName, LicenseeTask::class.java) {
      it.group = VERIFICATION_GROUP
      it.description = taskDescription("Android ${variant.name} variant")

      it.dependencyConfig.set(extension.toDependencyTreeConfig())
      it.validationConfig.set(extension.toLicenseValidationConfig())
      it.violationAction.set(extension.violationAction)
      it.addPomFileDependencies(variant.runtimeConfiguration)

      val reportBase = project.extensions.getByType(ReportingExtension::class.java).file(reportFolder)
      it.outputDir.set(File(reportBase, if (prefix) "android$suffix" else variant.name))
    }

    rootTask.configure {
      it.dependsOn(task)
    }
  }
}

private fun configureKotlinMultiplatformTargets(
  project: Project,
  extension: MutableLicenseeExtension,
  rootTask: TaskProvider<Task>,
  skipAndroid: Boolean = false,
) {
  val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
  val targets = kotlin.targets
  targets.configureEach { target ->
    if (target.platformType == common) {
      return@configureEach // All common dependencies end up in platform targets.
    }
    if (target.platformType == androidJvm) {
      if (skipAndroid) return@configureEach
      throw AssertionError("Found Android Kotlin target but no Android plugin was detected")
    }

    val suffix = target.name.capitalize(ROOT)
    val task = project.tasks.register("$baseTaskName$suffix", LicenseeTask::class.java) {
      it.group = VERIFICATION_GROUP
      it.description = taskDescription("Kotlin ${target.name} target")

      it.dependencyConfig.set(extension.toDependencyTreeConfig())
      it.validationConfig.set(extension.toLicenseValidationConfig())
      it.violationAction.set(extension.violationAction)

      val runtimeConfigurationName =
        target.compilations.getByName("main").compileDependencyConfigurationName
      val runtimeConfiguration = project.configurations.getByName(runtimeConfigurationName)
      it.addPomFileDependencies(runtimeConfiguration)

      val reportBase = project.extensions.getByType(ReportingExtension::class.java).file(reportFolder)
      it.outputDir.set(File(reportBase, target.name))
    }

    rootTask.configure {
      it.dependsOn(task)
    }
  }
}

private fun configureJavaPlugin(
  project: Project,
  extension: MutableLicenseeExtension,
) {
  val task = project.tasks.register(baseTaskName, LicenseeTask::class.java) {
    it.group = VERIFICATION_GROUP
    it.description = taskDescription()

    it.dependencyConfig.set(extension.toDependencyTreeConfig())
    it.validationConfig.set(extension.toLicenseValidationConfig())
    it.violationAction.set(extension.violationAction)

    val configuration = project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
    it.addPomFileDependencies(configuration)

    it.outputDir.set(project.extensions.getByType(ReportingExtension::class.java).file(reportFolder))
  }
  project.tasks.named(CHECK_TASK_NAME).configure {
    it.dependsOn(task)
  }
}

private fun taskDescription(target: String? = null) = buildString {
  append("Run Licensee dependency license validation")
  if (target != null) {
    append(" on ")
    append(target)
  }
}
