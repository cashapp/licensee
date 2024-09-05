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

import com.android.build.api.variant.AndroidComponentsExtension
import java.util.Locale.ROOT
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin.CHECK_TASK_NAME
import org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.androidJvm
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.common

private const val BASE_TASK_NAME = "licensee"
private const val REPORT_FOLDER = "licensee"

@Suppress("unused") // Instantiated reflectively by Gradle.
class LicenseePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    // HEY! If you update the minimum-supported Gradle version check to see if the Kotlin language version
    // can be bumped. See https://docs.gradle.org/current/userguide/compatibility.html#kotlin.
    require(GradleVersion.current() >= GradleVersion.version("8.0")) {
      "Licensee plugin requires Gradle 8.0 or later. Found ${GradleVersion.current()}"
    }

    val extension = project.objects.newInstance(MutableLicenseeExtension::class.java)
    project.extensions.add(LicenseeExtension::class.java, "licensee", extension)

    project.tasks.withType(LicenseeTask::class.java).configureEach {
      it.dependencyConfig.convention(extension.toDependencyTreeConfig())
      it.validationConfig.convention(extension.toLicenseValidationConfig())
      it.violationAction.convention(extension.violationAction)
      it.unusedAction.convention(extension.unusedAction)

      it.outputDir.convention(
        project.extensions.getByType(ReportingExtension::class.java).baseDirectory.dir(
          REPORT_FOLDER,
        ),
      )
    }

    // Note: java-library applies java so we only need to look for the latter.
    // Note: org.jetbrains.kotlin.jvm applies java so we only need to look for the latter.
    project.pluginManager.withPlugin("org.gradle.java") {
      // Special case: KMP with JVM withJava():
      // withKotlinMultiPlatformPlugin did already run, so the jvm target is already set, ignore another setup.
      if (!project.pluginManager.hasPlugin("org.jetbrains.kotlin.multiplatform")) {
        configureJavaPlugin(project)
      }
    }
    project.pluginManager.withPlugin("org.jetbrains.kotlin.js") {
      // The JS plugin uses the same runtime configuration name as the Java plugin.
      configureJavaPlugin(project)
    }

    withKotlinMultiPlatformPlugin(project, withAndroid = false) // see android logic below

    project.pluginManager.withPlugin("com.android.application") {
      configureAndroidPlugin(project)
    }
    project.pluginManager.withPlugin("com.android.library") {
      configureAndroidPlugin(project)
    }
    project.pluginManager.withPlugin("com.android.dynamic-feature") {
      configureAndroidPlugin(project)
    }

    project.afterEvaluate {
      require(BASE_TASK_NAME in project.tasks.names) {
        val name = if (project.path == ":") {
          "root project"
        } else {
          "project ${project.path}"
        }
        "'app.cash.licensee' requires compatible language/platform plugin to be applied ($name)"
      }
    }
  }
}

private fun configureAndroidPlugin(
  project: Project,
) {
  val rootTask = registerRootTask(project, "all Android variants")
  configureAndroidVariants(project, rootTask)
  withKotlinMultiPlatformPlugin(project, withAndroid = true)
}

private fun withKotlinMultiPlatformPlugin(
  project: Project,
  withAndroid: Boolean,
) {
  project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    val rootTask = registerRootTask(project, "all Kotlin targets")
    configureKotlinMultiplatformTargets(project, rootTask)
    if (withAndroid) {
      configureAndroidVariants(project, rootTask)
    }
  }
}

private fun registerRootTask(
  project: Project,
  target: String,
): TaskProvider<Task> {
  val rootTask = if (BASE_TASK_NAME in project.tasks.names) {
    project.tasks.named(BASE_TASK_NAME)
  } else {
    project.tasks.register(BASE_TASK_NAME)
  }

  rootTask.configure {
    it.group = VERIFICATION_GROUP
    it.description = taskDescription(target)
  }
  project.tasks.named(CHECK_TASK_NAME).configure {
    it.dependsOn(rootTask)
  }
  return rootTask
}

private fun configureAndroidVariants(
  project: Project,
  rootTask: TaskProvider<Task>,
) {
  val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)
  androidComponents.onVariants { variant ->
    val suffix = variant.name.replaceFirstChar { it.titlecase(ROOT) }
    val taskName = "${BASE_TASK_NAME}Android$suffix"

    val task = project.tasks.configure(taskName) {
      it.group = VERIFICATION_GROUP
      it.description = taskDescription("Android ${variant.name} variant")

      it.configurationToCheck(variant.runtimeConfiguration)

      val reportBase = project.extensions.getByType(ReportingExtension::class.java).baseDirectory.dir(REPORT_FOLDER)
      it.outputDir.set(reportBase.map { it.dir("android$suffix") })
    }

    rootTask.configure {
      it.dependsOn(task)
    }
  }
}

private fun configureKotlinMultiplatformTargets(
  project: Project,
  rootTask: TaskProvider<Task>,
) {
  val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
  val targets = kotlin.targets
  targets.configureEach { target ->
    if (target.platformType == common) {
      return@configureEach // All common dependencies end up in platform targets.
    }
    if (target.platformType == androidJvm) {
      return@configureEach // handled by android logic.
    }

    val suffix = target.name.replaceFirstChar { it.titlecase(ROOT) }
    val task = project.tasks.configure("$BASE_TASK_NAME$suffix") {
      it.group = VERIFICATION_GROUP
      it.description = taskDescription("Kotlin ${target.name} target")

      val compilation = target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
      // Fallback to compile dependencies when runtime isn't supported, e.g. Kotlin/Native.
      val runtimeConfigurationName = compilation.runtimeDependencyConfigurationName ?: compilation.compileDependencyConfigurationName

      val runtimeConfiguration = project.configurations.named(runtimeConfigurationName)
      it.configurationToCheck(runtimeConfiguration)

      val reportBase = project.extensions.getByType(ReportingExtension::class.java).baseDirectory.dir(REPORT_FOLDER)
      it.outputDir.set(reportBase.map { it.dir(target.name) })
    }

    rootTask.configure {
      it.dependsOn(task)
    }
  }
}

private fun configureJavaPlugin(
  project: Project,
) {
  val task = project.tasks.configure(BASE_TASK_NAME) {
    it.group = VERIFICATION_GROUP
    it.description = taskDescription()

    val configuration = project.configurations.named(RUNTIME_CLASSPATH_CONFIGURATION_NAME)
    it.configurationToCheck(configuration)
  }
  project.tasks.named(CHECK_TASK_NAME).configure {
    it.dependsOn(task)
  }
}

private fun TaskContainer.configure(name: String, config: (LicenseeTask) -> Unit): TaskProvider<LicenseeTask> = if (name in names) {
  named(name, LicenseeTask::class.java, config)
} else {
  register(name, LicenseeTask::class.java, config)
}

private fun taskDescription(target: String? = null) = buildString {
  append("Run Licensee dependency license validation")
  if (target != null) {
    append(" on ")
    append(target)
  }
}
