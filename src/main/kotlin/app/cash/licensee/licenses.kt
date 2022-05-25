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

import io.cloudflight.license.spdx.LicenseQuery
import io.cloudflight.license.spdx.SpdxLicenses

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
      pomInfo.name,
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

private fun PomLicense.toSpdxOrNull(): SpdxLicense? {
  val spdxLicense = SpdxLicenses.findLicense(LicenseQuery(url = url, name = name))
  return if (spdxLicense != null) {
    val licenseUrl = if (spdxLicense.seeAlso.isNotEmpty())
    // Assume an 'https' variant is reachable.
      spdxLicense.seeAlso.first().replace("http://", "https://")
    else
      url ?: spdxLicense.reference
    SpdxLicense(identifier = spdxLicense.licenseId, name = spdxLicense.name, licenseUrl)
  } else {
    null
  }
}
