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
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.CLASSES
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.attributes.Usage.JAVA_RUNTIME
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common
import java.io.File
import java.util.Locale.ROOT

private const val baseTaskName = "licensee"
private const val reportFolder = "licensee"

@Suppress("unused") // Instantiated reflectively by Gradle.
class LicenseePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = MutableLicenseeExtension()
    project.extensions.add(LicenseeExtension::class.java, "licensee", extension)

    var foundCompatiblePlugin = false
    project.afterEvaluate {
      check(foundCompatiblePlugin) {
        val name = if (project === project.rootProject) {
          "root project"
        } else {
          "project ${project.path}"
        }
        "'app.cash.licensee' requires compatible language/platform plugin to be applied ($name)"
      }
    }

    project.plugins.withId("org.jetbrains.kotlin.js") {
      foundCompatiblePlugin = true
      // The JS plugin uses the same runtime configuration name as the Java plugin.
      configureJavaPlugin(project, extension)
    }

    // Note: java-library applies java so we only need to look for the latter.
    // Note: org.jetbrains.kotlin.jvm applies java so we only need to look for the latter.
    project.plugins.withId("java") {
      foundCompatiblePlugin = true
      configureJavaPlugin(project, extension)
    }

    // The Android and Kotlin MPP plugins interact. Therefore we defer handling either until after
    // evaluation where we can determine whether neither, one, or both are in use.
    var androidPlugin: AndroidPlugin? = null
    var kotlinMppPlugin = false
    project.plugins.withId("com.android.application") {
      foundCompatiblePlugin = true
      androidPlugin = AndroidPlugin.Application
    }
    project.plugins.withId("com.android.library") {
      foundCompatiblePlugin = true
      androidPlugin = AndroidPlugin.Library
    }
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
      foundCompatiblePlugin = true
      kotlinMppPlugin = true
    }
    project.afterEvaluate {
      @Suppress("NAME_SHADOWING") // Local read for smart cast.
      val androidPlugin = androidPlugin

      var rootTask: TaskProvider<Task>? = null
      if (kotlinMppPlugin) {
        rootTask = project.tasks.register(baseTaskName)
        if (androidPlugin != null) {
          configureKotlinMultiplatformTargets(project, extension, rootTask, skipAndroid = true)
          configureAndroidVariants(project, extension, rootTask, androidPlugin, prefix = true)
        } else {
          configureKotlinMultiplatformTargets(project, extension, rootTask)
        }
      } else if (androidPlugin != null) {
        rootTask = project.tasks.register(baseTaskName)
        configureAndroidVariants(project, extension, rootTask, androidPlugin)
      }

      if (rootTask != null) {
        project.tasks.named("check").configure {
          it.dependsOn(rootTask)
        }
      }
    }
  }
}

private enum class AndroidPlugin {
  Application,
  Library,
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
    AndroidPlugin.Application -> extensions.getByType(AppExtension::class.java).applicationVariants
    AndroidPlugin.Library -> extensions.getByType(LibraryExtension::class.java).libraryVariants
  }
  variants.all { variant ->
    val suffix = variant.name.capitalize(ROOT)
    val taskName = buildString {
      append(baseTaskName)
      if (prefix) {
        append("Android")
      }
      append(suffix)
    }
    val task = project.tasks.register(taskName, LicenseeTask::class.java) {
      it.dependencyConfig = extension.toDependencyTreeConfig()
      it.validationConfig = extension.toLicenseValidationConfig()
      it.setClasspath(variant.runtimeConfiguration, CLASSES.type)

      val reportBase = project.extensions.getByType(ReportingExtension::class.java).file(reportFolder)
      it.outputDir = File(reportBase, if (prefix) "android$suffix" else variant.name)
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
  targets.all { target ->
    if (target.platformType == common) {
      return@all // All common dependencies end up in platform targets.
    }
    if (target.platformType == androidJvm) {
      if (skipAndroid) return@all
      throw AssertionError("Found Android Kotlin target but no Android plugin was detected")
    }

    val suffix = target.name.capitalize(ROOT)
    val task = project.tasks.register("$baseTaskName$suffix", LicenseeTask::class.java) {
      it.dependencyConfig = extension.toDependencyTreeConfig()
      it.validationConfig = extension.toLicenseValidationConfig()

      val runtimeConfigurationName =
        target.compilations.getByName("main").compileDependencyConfigurationName
      val runtimeConfiguration = project.configurations.getByName(runtimeConfigurationName)
      it.setClasspath(runtimeConfiguration, JAVA_RUNTIME)

      val reportBase = project.extensions.getByType(ReportingExtension::class.java).file(reportFolder)
      it.outputDir = File(reportBase, target.name)
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
    it.dependencyConfig = extension.toDependencyTreeConfig()
    it.validationConfig = extension.toLicenseValidationConfig()

    val configuration = project.configurations.getByName(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
    it.setClasspath(configuration, JAVA_RUNTIME)

    it.outputDir = project.extensions.getByType(ReportingExtension::class.java).file(reportFolder)
  }
  project.tasks.named("check").configure {
    it.dependsOn(task)
  }
}
