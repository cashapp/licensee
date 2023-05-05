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

import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class SpdxLicenses(
  private val identifierToLicense: Map<String, SpdxLicense>,
  private val urlToLicense: Map<String, List<SpdxLicense>>,
) {
  fun findByIdentifier(id: String): SpdxLicense? = identifierToLicense[id]
  fun findByUrl(url: String): List<SpdxLicense>? = urlToLicense[url]

  companion object {
    private val format = Json {
      ignoreUnknownKeys = true
    }

    val embedded by lazy(PUBLICATION) {
      val json = SpdxLicenses::class.java
        .getResourceAsStream("/app/cash/licensee/licenses.json")!!
        .use { it.reader().readText() }
      parseJson(json)
    }

    fun parseJson(json: String): SpdxLicenses {
      val licenses = format.decodeFromString(SpdxLicensesJson.serializer(), json)

      val identifierToLicense = licenses.licenses
        .map { license ->
          val firstUrl = license.otherUrls.firstOrNull() ?: license.spdxUrl
          val targetUrl = if (firstUrl.startsWith("http://")) {
            // Assume an 'https' variant is reachable.
            "https" + firstUrl.substring(4)
          } else {
            firstUrl
          }
          SpdxLicense(license.id, license.name, targetUrl)
        }
        .associateBy { it.identifier }

      val urlToLicense: Map<String, List<SpdxLicense>> = licenses.licenses
        .flatMap { license ->
          val urls = mutableListOf(license.spdxUrl)

          for (otherUrl in license.otherUrls) {
            if (otherUrl.startsWith("http://")) {
              // Assume an 'https' variant is reachable.
              val httpsUrl = "https" + otherUrl.substring(4)
              urls += httpsUrl
            }
            urls += otherUrl
          }

          urls.map { it to identifierToLicense.getValue(license.id) }
        }.groupBy({ (url) ->
          url
        }) { (_, license) ->
          license
        }

      return SpdxLicenses(
        identifierToLicense,
        urlToLicense,
      )
    }
  }
}

@Serializable
private data class SpdxLicensesJson(
  val licenses: List<SpdxLicenseJson>,
)

@Serializable
private data class SpdxLicenseJson(
  @SerialName("licenseId") val id: String,
  val name: String,
  @SerialName("reference") val spdxUrl: String,
  @SerialName("seeAlso") val otherUrls: List<String>,
)
