/*
 * Copyright (C) 2023 Square, Inc.
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

internal val defaultFallbackUrls: FallbackBuilder.() -> Unit = {
  putLicense("Apache-2.0") {
    add("http://www.apache.org/licenses/LICENSE-2.0.txt")
    add("https://www.apache.org/licenses/LICENSE-2.0.txt")
    add("http://www.apache.org/licenses/LICENSE-2.0.html")
    add("https://www.apache.org/licenses/LICENSE-2.0.html")
    add("http://www.opensource.org/licenses/apache2.0.php")
    add("https://www.opensource.org/licenses/apache2.0.php")
    add("http://www.apache.org/licenses/LICENSE-2.0")
    add("http://api.github.com/licenses/apache-2.0")
    add("https://api.github.com/licenses/apache-2.0")
  }
  putLicense("CC0-1.0") {
    add("http://creativecommons.org/publicdomain/zero/1.0/")
    add("https://creativecommons.org/publicdomain/zero/1.0/")
    add("http://api.github.com/licenses/cc0-1.0")
    add("https://api.github.com/licenses/cc0-1.0")
  }
  putLicense("LGPL-2.1-only") {
    add("http://www.opensource.org/licenses/LGPL-2.1")
    add("https://www.opensource.org/licenses/LGPL-2.1")
    add("http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
    add("https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html")
    add("http://api.github.com/licenses/lgpl-2.1")
    add("https://api.github.com/licenses/lgpl-2.1")
  }
  putLicense("MIT") {
    add("http://opensource.org/licenses/mit-license")
    add("https://opensource.org/licenses/mit-license")
    add("http://www.opensource.org/licenses/mit-license.php")
    add("https://www.opensource.org/licenses/mit-license.php")
    add("http://opensource.org/licenses/MIT")
    add("https://opensource.org/licenses/MIT")
    add("http://api.github.com/licenses/mit")
    add("https://api.github.com/licenses/mit")
  }
  putLicense("BSD-2-Clause") {
    add("http://www.opensource.org/licenses/bsd-license")
    add("https://www.opensource.org/licenses/bsd-license")
    add("http://www.opensource.org/licenses/bsd-license.php")
    add("https://www.opensource.org/licenses/bsd-license.php")
    add("http://api.github.com/licenses/bsd-2-clause")
    add("https://api.github.com/licenses/bsd-2-clause")
  }
  putLicense("BSD-3-Clause") {
    add("http://opensource.org/licenses/BSD-3-Clause")
    add("http://api.github.com/licenses/bsd-3-clause")
    add("https://api.github.com/licenses/bsd-3-clause")
  }
  putLicense("GPL-2.0-with-classpath-exception") {
    add("http://www.gnu.org/software/classpath/license.html")
  }
  putLicense("GPL-2.0", "GPL-2.0-or-later") {
    add("http://choosealicense.com/licenses/gpl-2.0")
    add("https://choosealicense.com/licenses/gpl-2.0")
    add("http://opensource.org/license/gpl-2-0")
    add("https://opensource.org/license/gpl-2-0")
    add("http://www.gnu.org/licenses/old-licenses/gpl-2.0.html")
    add("https://www.gnu.org/licenses/old-licenses/gpl-2.0.html")
    add("http://api.github.com/licenses/gpl-2.0")
    add("https://api.github.com/licenses/gpl-2.0")
  }
  putLicense("EPL-1.0") {
    add("http://www.eclipse.org/org/documents/epl-v10.php")
    add("https://www.eclipse.org/org/documents/epl-v10.php")
    add("http://api.github.com/licenses/epl-1.0")
    add("https://api.github.com/licenses/epl-1.0")
  }
  putLicense("EPL-2.0") {
    add("http://www.eclipse.org/legal/epl-2.0/")
    add("https://www.eclipse.org/legal/epl-2.0/")
    add("http://api.github.com/licenses/epl-2.0")
    add("https://api.github.com/licenses/epl-2.0")
  }
  putLicense("ISC") {
    add("https://opensource.org/licenses/isc-license.txt")
  }
}
