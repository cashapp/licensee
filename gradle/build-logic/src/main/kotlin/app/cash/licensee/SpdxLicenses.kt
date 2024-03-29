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

import kotlinx.serialization.json.Json

internal class SpdxLicenses(
  val identifierToLicense: Map<String, SpdxLicenseJson>,
  private val urlToLicense: Map<String, List<SpdxLicenseJson>>,
) {
  fun findByIdentifier(id: String): SpdxLicenseJson? = identifierToLicense[id]
  fun findByUrl(url: String): List<SpdxLicenseJson>? = urlToLicense[url]

  val simplified: List<Pair<List<String>, List<SpdxLicenseJson>>> = urlToLicense.simplify()

  companion object {
    private val format: Json = Json {
      ignoreUnknownKeys = true
    }

    internal fun parseJson(
      json: String,
      withFallbackUrls: FallbackBuilder.() -> Unit,
    ): SpdxLicenses {
      val licenses = format.decodeFromString(
        SpdxLicensesJson.serializer(),
        json,
      ).licenses.sortedBy {
        it.id
      }

      val identifierToLicense = licenses.associateBy { it.id }

      val mapUrls = licenses.mapUrls()
      val urlToLicense = mapUrls.mapValues {
        it.value.toList()
      }
      val withFallbacks = mapUrls.addFallbackUrls(
        identifierToLicense,
        urlToLicense,
        withFallbackUrls,
      )

      return SpdxLicenses(
        identifierToLicense,
        withFallbacks,
      )
    }
  }
}

internal fun List<SpdxLicenseJson>.mapUrls(): Map<String, List<SpdxLicenseJson>> {
  val urlToIds: MutableMap<String, MutableList<SpdxLicenseJson>> = mutableMapOf()

  for (license in this) {
    urlToIds.computeIfAbsent(license.spdxUrl) { mutableListOf() }.add(license)

    for (otherUrl in license.otherUrls) {
      if (otherUrl.startsWith("http://")) {
        // Assume an 'https' variant is reachable.
        val httpsUrl = "https" + otherUrl.substring(4)
        urlToIds.computeIfAbsent(httpsUrl) { mutableListOf() }.add(license)
      }
      urlToIds.computeIfAbsent(otherUrl) { mutableListOf() }.add(license)
    }
  }
  return urlToIds.mapValues { it.value.toList() }
}

internal fun Map<String, List<SpdxLicenseJson>>.addFallbackUrls(
  idToLicense: Map<String, SpdxLicenseJson>,
  urlToLicenses: Map<String, List<SpdxLicenseJson>>,
  action: FallbackBuilder.() -> Unit,
): Map<String, List<SpdxLicenseJson>> = toMutableMap().apply {
  FallbackBuilder(
    idToLicense,
    urlToLicenses,
    this,
  ).action()
}

internal class FallbackBuilder(
  private val findByIdentifier: Map<String, SpdxLicenseJson>,
  private val findByUrl: Map<String, List<SpdxLicenseJson>>,
  private val result: MutableMap<String, List<SpdxLicenseJson>>,
) {
  fun putLicense(vararg spdxIds: String, urls: MutableList<String>.() -> Unit) {
    val licenses = spdxIds.map {
      requireNotNull(findByIdentifier[it]) {
        "No SPDX identifier '$it' in the embedded set"
      }
    }

    for (url in buildList(urls)) {
      require(findByUrl[url].orEmpty().isEmpty()) {
        "$url is canonical and does not need to be a fallback"
      }
      require(result.put(url, licenses) == null) {
        "$url specified twice"
      }
    }
  }
}

internal fun Map<String, List<SpdxLicenseJson>>.simplify(): List<Pair<List<String>, List<SpdxLicenseJson>>> =
  entries.groupBy({ it.value }, { it.key }).toList().map {
    it.second.sorted() to it.first
  }.sortedBy {
    it.first.first()
  }

internal val SpdxLicenseJson.targetUrl get() = (otherUrls.firstOrNull() ?: spdxUrl).let { firstUrl ->
  if (firstUrl.startsWith("http://")) {
    // Assume an 'https' variant is reachable.
    "https" + firstUrl.substring(4)
  } else {
    firstUrl
  }
}
