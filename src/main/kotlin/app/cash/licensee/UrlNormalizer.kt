/*
 * Copyright (C) 2025 Square, Inc.
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

import java.net.URI
import java.net.URISyntaxException

internal fun normalizeUrl(url: String): String {
  return try {
    var uri = URI(url.lowercase())

    // Add trailing slash if it's a base URL or a path
    if (uri.path.isEmpty() || (!uri.path.endsWith("/") && uri.fragment.isNullOrEmpty() && uri.query.isNullOrEmpty())) {
      uri = URI(uri.scheme, uri.userInfo, uri.host, uri.port, if (uri.path.isEmpty()) "/" else uri.path + "/", uri.query, uri.fragment)
    }

    uri.toString()
  } catch (_: URISyntaxException) {
    url
  }
}
