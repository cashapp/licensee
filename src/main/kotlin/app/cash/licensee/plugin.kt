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
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.publishing.AndroidArtifacts.ArtifactType.CLASSES
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Locale.ROOT

@Suppress("unused") // Instantiated reflectively by Gradle.
class LicenseePlugin : Plugin<Project> {
  override fun apply(project: Project) {
    val extension = MutableLicenseeExtension()
    project.extensions.add(LicenseeExtension::class.java, "licensee", extension)

    var foundCompatiblePlugin = false
    project.afterEvaluate {
      check(foundCompatiblePlugin) {
        "'app.cash.licensee' plugin only works with 'com.android.application' or 'com.android.library' plugin"
      }
    }

    project.plugins.withId("com.android.application") {
      foundCompatiblePlugin = true
      configureAndroidApplicationPlugin(project, extension)
    }
    project.plugins.withId("com.android.library") {
      foundCompatiblePlugin = true
      configureAndroidLibraryPlugin(project, extension)
    }
  }
}

private fun configureAndroidApplicationPlugin(
  project: Project,
  extension: MutableLicenseeExtension,
) {
  configureAndroidPlugin(project, extension, AppExtension::applicationVariants)
}

private fun configureAndroidLibraryPlugin(
  project: Project,
  extension: MutableLicenseeExtension,
) {
  configureAndroidPlugin(project, extension, LibraryExtension::libraryVariants)
}

private inline fun <reified T : BaseExtension> configureAndroidPlugin(
  project: Project,
  extension: MutableLicenseeExtension,
  variants: T.() -> DomainObjectCollection<out BaseVariant>
) {
  val rootTask = project.tasks.register("licensee")
  project.tasks.named("check").configure {
    it.dependsOn(rootTask)
  }

  val android = project.extensions.getByType(T::class.java)
  android.variants().all { variant ->
    val suffix = variant.name.capitalize(ROOT)
    val task = project.tasks.register("licensee$suffix", LicenseeTask::class.java) {
      it.dependencyConfig = extension.toDependencyTreeConfig()
      it.validationConfig = extension.toLicenseValidationConfig()
      it.setClasspath(variant.runtimeConfiguration, CLASSES.type)

      it.outputDir =
        project.buildDir.resolve("reports/licensee/${variant.name}/")
    }

    rootTask.configure {
      it.dependsOn(task)
    }
  }
}
