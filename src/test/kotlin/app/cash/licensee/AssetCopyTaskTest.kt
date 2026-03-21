/*
 * Copyright (C) 2026 Square, Inc.
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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import kotlin.io.path.readText
import kotlin.io.path.toPath
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.Test

class AssetCopyTaskTest {
  @Test
  fun minifyJson() {
    val inputFile = requireNotNull(this::class.java.getResource("licenses.json")).toURI().toPath()
    val originalJson = inputFile.readText()
    val minifiedJson = minifyJson(originalJson)

    // Verify compressed size is smaller.
    assertThat(minifiedJson.length).isLessThan(originalJson.length)

    // Verify data integrity.
    val originalElement = Json.decodeFromString<JsonElement>(originalJson)
    val minifiedElement = Json.decodeFromString<JsonElement>(minifiedJson)
    assertThat(minifiedElement).isEqualTo(originalElement)
  }
}
