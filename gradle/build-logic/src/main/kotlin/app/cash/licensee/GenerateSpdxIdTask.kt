/*
 * Copyright (C) 2024 Square, Inc.
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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MemberName.Companion.member
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class GenerateSpdxIdTask : DefaultTask() {
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val inputJson: RegularFileProperty

  @get:OutputDirectory
  abstract val generatedSpdx: DirectoryProperty

  init {
    group = "generateSpdx"
    generatedSpdx.convention(project.layout.buildDirectory.dir("generated/spdx"))
  }

  @TaskAction
  fun write() {
    val parsed = SpdxLicenses.parseJson(inputJson.asFile.get().readText(), defaultFallbackUrls)
    parsed.generate().writeTo(generatedSpdx.asFile.get())
  }
}

private fun SpdxLicenses.generate(): FileSpec {
  val fileSpec = FileSpec.builder("app.cash.licensee", "licensesSpdx")

  fileSpec.addType(addSpdxIdInterface())

  return fileSpec.build()
}

private val SpdxId: ClassName = ClassName("app.cash.licensee", "SpdxId")
private val SpdxIdCompanion: ClassName = SpdxId.nestedClass("Companion")

private val SpdxLicenseJson.identifier: String
  get() = when (id) {
    // Special-case IDs which start with a digit:
    "0BSD" -> "ZeroBSD"
    "3D-Slicer-1.0" -> "ThreeD_Slicer_10"
    else -> {
      id
        .replace("-", "_")
        .replace(".", "")
        .replace("+", "Plus")
    }
  }

private fun SpdxLicenses.addSpdxIdInterface(): TypeSpec = TypeSpec.classBuilder("SpdxId").apply {
  primaryConstructor(
    FunSpec.constructorBuilder()
      .addParameter("id", STRING)
      .addParameter("name", STRING)
      .addParameter("url", STRING)
      .build(),
  )
  addProperty(PropertySpec.builder("id", STRING).initializer("id").build())
  addProperty(PropertySpec.builder("name", STRING).initializer("name").build())
  addProperty(PropertySpec.builder("url", STRING).initializer("url").build())
  addSuperinterface(java.io.Serializable::class)

  addType(spdxIdCompanion())
}.build()

private fun SpdxLicenses.spdxIdCompanion(): TypeSpec = TypeSpec.companionObjectBuilder().apply {
  for ((_, license) in identifierToLicense) {
    addProperty(
      PropertySpec.builder(license.identifier, SpdxId)
        .addAnnotation(JvmStatic::class)
        .addKdoc(license.name)
        .initializer("%T(%S, %S, %S)", SpdxId, license.id, license.name, license.targetUrl)
        .build(),
    )
  }

  addFunction(findByIdentifier())
  addFunction(findByUrl())
}.build()

private fun SpdxLicenses.findByIdentifier(): FunSpec = FunSpec.builder("findByIdentifier").apply {
  addAnnotation(JvmStatic::class)
  addParameter("id", STRING)
  returns(SpdxId.copy(nullable = true))

  beginControlFlow("return when (id)")
  for ((_, license) in identifierToLicense) {
    addCode("%S -> %M\n", license.id, SpdxIdCompanion.member(license.identifier))
  }
  addCode("else -> null\n")
  endControlFlow()
}.build()

private fun SpdxLicenses.findByUrl(): FunSpec = FunSpec.builder("findByUrl").apply {
  addParameter("url", STRING)
  addModifiers(KModifier.INTERNAL)
  returns(LIST.parameterizedBy(SpdxId))

  beginControlFlow("return when (url)")
  for ((urls, licenses) in simplified) {
    for (url in urls) {
      addCode("%S,\n", url)
    }
    addCode(" -> listOf(")
    for (license in licenses) {
      addCode("\n%M,", SpdxIdCompanion.member(license.identifier))
    }
    addCode("\n)\n")
  }
  addCode("else -> emptyList()\n")
  endControlFlow()
}.build()
