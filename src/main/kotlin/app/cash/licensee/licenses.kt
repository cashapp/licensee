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

import java.net.URL

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

    val lowerUrl = URL(url.toLowerCase())
    val host = lowerUrl.host.removePrefix("www.")
    val path = lowerUrl.path.removeSuffix("/")
      .removeSuffix(".txt")
      .removeSuffix(".php")
      .removeSuffix(".html")
    val fixedUrl = host + path

    val fallbackId = when (fixedUrl) {
      "apache.org/licenses/license-2.0",
      "api.github.com/licenses/apache-2.0",
      "opensource.org/licenses/apache2.0",
      "opensource.org/license/apache-2-0",
      -> "Apache-2.0"

      "api.github.com/licenses/cc0-1.0",
      "creativecommons.org/publicdomain/zero/1.0",
      -> "CC0-1.0"

      "api.github.com/licenses/lgpl-2.1",
      "gnu.org/licenses/old-licenses/lgpl-2.1",
      "opensource.org/licenses/lgpl-2.1",
      -> "LGPL-2.1-only"

      "opensource.org/licenses/mit-license",
      "api.github.com/licenses/mit",
      -> "MIT"

      "api.github.com/licenses/bsd-2-clause",
      "opensource.org/licenses/bsd-license",
      -> "BSD-2-Clause"

      "api.github.com/licenses/bsd-3-clause",
      "opensource.org/licenses/BSD-3-Clause",
      -> "BSD-3-Clause"

      "gnu.org/software/classpath/license",
      -> "GPL-2.0-with-classpath-exception"

      "api.github.com/licenses/gpl-2.0",
      "choosealicense.com/licenses/gpl-2.0",
      "gnu.org/licenses/old-licenses/gpl-2.0",
      "opensource.org/license/gpl-2-0",
      -> "GPL-2.0-or-later"

      "api.github.com/licenses/epl-1.0",
      "eclipse.org/org/documents/epl-v10",
      -> "EPL-1.0"

      "api.github.com/licenses/epl-2.0",
      "eclipse.org/legal/epl-2.0",
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
