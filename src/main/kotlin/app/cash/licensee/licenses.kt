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
  licenseSources: List<LicenseSource>,
): List<ArtifactDetail> {
  val artifactDetails = mutableListOf<ArtifactDetail>()
  for ((id, pomInfo) in coordinateToPomInfo) {
    val spdxLicenses = mutableSetOf<SpdxLicense>()
    val unknownLicenses = mutableSetOf<UnknownLicense>()
    for (license in pomInfo.licenses) {
      val spdxLicense = license.toSpdx(licenseSources)
      if (spdxLicense.isNotEmpty()) {
        spdxLicenses += spdxLicense
      } else {
        unknownLicenses += UnknownLicense(license.name, license.url)
      }
    }

    artifactDetails += ArtifactDetail(
      id.group,
      id.artifact,
      id.version,
      pomInfo.name,
      spdxLicenses,
      unknownLicenses,
      pomInfo.scm?.url?.let(::ArtifactScm),
    )
  }

  artifactDetails.sortWith(detailsComparator)
  return artifactDetails
}

private val detailsComparator =
  compareBy(ArtifactDetail::groupId, ArtifactDetail::artifactId, ArtifactDetail::version)

private fun PomLicense.toSpdx(
  sources: List<LicenseSource>,
): List<SpdxLicense> {
  for (source in sources) {
    return when (source) {
      LicenseSource.LICENSE_NAME -> {
        if (name == null) continue
        val license = SpdxId.findByIdentifier(name) ?: continue
        listOf(license.toSpdxLicense())
      }
      LicenseSource.LICENSE_URL -> {
        if (url == null) continue
        SpdxId.findByUrl(url).map(SpdxId::toSpdxLicense)
      }
    }
  }

  return emptyList()
}

internal fun SpdxId.toSpdxLicense() = SpdxLicense(
  identifier = id,
  name = name,
  url = url,
)
