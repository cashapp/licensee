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
      val spdxLicense = license.toSpdx()
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

private fun PomLicense.toSpdx(): List<SpdxLicense> {
  if (url != null) {
    SpdxLicenses.embedded.findByUrl(url)?.let { license ->
      return license
    }
    @Suppress("HttpUrlsUsage")
    val fallbackId = when (url) {
      "http://www.apache.org/licenses/LICENSE-2.0.txt",
      "https://www.apache.org/licenses/LICENSE-2.0.txt",
      "http://www.apache.org/licenses/LICENSE-2.0.html",
      "https://www.apache.org/licenses/LICENSE-2.0.html",
      "http://www.opensource.org/licenses/apache2.0.php",
      "https://www.opensource.org/licenses/apache2.0.php",
      "http://www.apache.org/licenses/LICENSE-2.0",
      "https://www.apache.org/licenses/LICENSE-2.0",
      -> "Apache-2.0"

      "http://creativecommons.org/publicdomain/zero/1.0/",
      "https://creativecommons.org/publicdomain/zero/1.0/",
      -> "CC0-1.0"

      "http://www.opensource.org/licenses/LGPL-2.1",
      "https://www.opensource.org/licenses/LGPL-2.1",
      "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html",
      "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html",
      -> "LGPL-2.1-only"

      "http://opensource.org/licenses/mit-license",
      "https://opensource.org/licenses/mit-license",
      "http://www.opensource.org/licenses/mit-license.php",
      "https://www.opensource.org/licenses/mit-license.php",
      "http://opensource.org/licenses/MIT",
      "https://opensource.org/licenses/MIT",
      -> "MIT"

      "http://www.opensource.org/licenses/bsd-license",
      "https://www.opensource.org/licenses/bsd-license",
      "http://www.opensource.org/licenses/bsd-license.php",
      "https://www.opensource.org/licenses/bsd-license.php",
      -> "BSD-2-Clause"

      "http://opensource.org/licenses/BSD-3-Clause",
      "https://opensource.org/licenses/BSD-3-Clause",
      -> "BSD-3-Clause"

      "http://www.gnu.org/software/classpath/license.html",
      "https://www.gnu.org/software/classpath/license.html",
      -> "GPL-2.0-with-classpath-exception"

      "http://www.eclipse.org/org/documents/epl-v10.php",
      "https://www.eclipse.org/org/documents/epl-v10.php",
      -> "EPL-1.0"

      "http://www.eclipse.org/legal/epl-2.0/",
      "https://www.eclipse.org/legal/epl-2.0/",
      -> "EPL-2.0"

      else -> null
    }
    fallbackId?.let(SpdxLicenses.embedded::findByIdentifier)?.let { license ->
      return listOf(license)
    }
  } else if (name != null) {
    // Only fallback to name-based matching if the URL is null.
    SpdxLicenses.embedded.findByIdentifier(name)?.let { license ->
      return listOf(license)
    }
  }

  return emptyList()
}
