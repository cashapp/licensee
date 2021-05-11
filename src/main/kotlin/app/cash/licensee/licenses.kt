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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal fun normalizeLicenseInfo(
  coordinateToPomInfo: Map<DependencyCoordinates, PomInfo>,
): List<ArtifactDetail> {
  val artifactDetails = mutableListOf<ArtifactDetail>()
  for ((id, pomInfo) in coordinateToPomInfo) {
    val spdxLicenses = mutableSetOf<SpdxLicense>()
    val unknownLicenses = mutableSetOf<UnknownLicense>()
    for (license in pomInfo.licenses) {
      val spdxLicense = license.toSpdxOrNull()
      if (spdxLicense != null) {
        spdxLicenses += spdxLicense
      } else {
        unknownLicenses += UnknownLicense(license.name, license.url)
      }
    }

    artifactDetails += ArtifactDetail(
      id.group, id.artifact, id.version, spdxLicenses,
      unknownLicenses
    )
  }
  return artifactDetails
}

@Serializable
private data class SpdxLicensesJson(
  val licenses: List<SpdxLicenseJson>,
)

@Serializable
private data class SpdxLicenseJson(
  @SerialName("licenseId") val id: String,
  val name: String,
  @SerialName("detailsUrl") val spdxUrl: String,
  @SerialName("seeAlso") val otherUrls: List<String>,
)

private val spdxLicensesJson = run {
  val data = SpdxLicensesJson::class.java.getResourceAsStream("/app/cash/licensee/licenses.json")!!.use { it.reader().readText() }
  val format = Json {
    ignoreUnknownKeys = true
  }
  val licenses = format.decodeFromString(SpdxLicensesJson.serializer(), data)
  licenses.licenses
}

private val spdxIdentifierToLicense = spdxLicensesJson.map { json ->
  val firstUrl = json.otherUrls[0]
  val targetUrl = if (firstUrl.startsWith("http://")) {
    // Assume an 'https' variant is reachable.
    "https" + firstUrl.substring(4)
  } else {
    firstUrl
  }
  SpdxLicense(json.id, json.name, targetUrl)
}.associateBy { it.identifier }

private const val spdxBaseUrl = "https://spdx.org/licenses/"
private val licenseUrlToSpdxLicense = spdxLicensesJson
  .flatMap { json ->
    val urls = mutableListOf<String>()

    val spdxRelativeUrl = json.spdxUrl
    check(spdxRelativeUrl.startsWith("./"))
    val spdxUrl = spdxBaseUrl + spdxRelativeUrl.substring(2)
    urls += spdxUrl

    for (otherUrl in json.otherUrls) {
      if (otherUrl.startsWith("http://")) {
        // Assume an 'https' variant is reachable.
        val httpsUrl = "https" + otherUrl.substring(4)
        urls += httpsUrl
      }
      urls += otherUrl
    }

    (json.otherUrls + spdxUrl).map { it to spdxIdentifierToLicense[json.id] }
  }
  // This creates behavior where we do not overwrite keys. Take, for example, MPL-2.0 and
  // MPL-2.0-no-copyleft-exception which use the same URL. For now we'll blanket prefer the first.
  .distinctBy { it.first }
  .toMap()

private fun PomLicense.toSpdxOrNull(): SpdxLicense? {
  licenseUrlToSpdxLicense[url]?.let { license ->
    return license
  }

  val fallbackId = when (url) {
    "http://www.apache.org/licenses/LICENSE-2.0.txt",
    "https://www.apache.org/licenses/LICENSE-2.0.txt",
    -> "Apache-2.0"

    "http://creativecommons.org/publicdomain/zero/1.0/",
    -> "CC0-1.0"

    else -> null
  }
  return fallbackId?.let(spdxIdentifierToLicense::get)
}
