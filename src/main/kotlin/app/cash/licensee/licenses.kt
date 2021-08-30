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
      id.group,
      id.artifact,
      id.version,
      spdxLicenses,
      unknownLicenses,
      pomInfo.scm?.url?.let(::ArtifactScm)
    )
  }

  artifactDetails.sortWith(detailsComparator)
  return artifactDetails
}

private val detailsComparator =
  compareBy(ArtifactDetail::groupId, ArtifactDetail::artifactId, ArtifactDetail::version)

private val spdxLicenses = run {
  val json = SpdxLicenses::class.java.getResourceAsStream("/app/cash/licensee/licenses.json")!!.use { it.reader().readText() }
  SpdxLicenses.parseJson(json)
}

private fun PomLicense.toSpdxOrNull(): SpdxLicense? {
  if (url != null) {
    spdxLicenses.findByUrl(url)?.let { license ->
      return license
    }
    val fallbackId = when (url) {
      "http://www.apache.org/licenses/LICENSE-2.0.txt",
      "https://www.apache.org/licenses/LICENSE-2.0.txt",
      -> "Apache-2.0"

      "http://creativecommons.org/publicdomain/zero/1.0/",
      "http://repository.jboss.org/licenses/cc0-1.0.txt",
      -> "CC0-1.0"

      else -> null
    }
    fallbackId?.let(spdxLicenses::findByIdentifier)?.let { license ->
      return license
    }
  } else if (name != null) {
    // Only fallback to name-based matching if the URL is null.
    spdxLicenses.findByIdentifier(name)?.let { license ->
      return license
    }
  }

  return null
}
