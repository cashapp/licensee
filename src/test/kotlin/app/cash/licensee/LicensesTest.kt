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

import org.junit.Assert.assertEquals
import org.junit.Test

class LicensesTest {

  @Suppress("HttpUrlsUsage")
  @Test fun fallbackId() {
    assertEquals("Apache-2.0", getFallbackId("http://www.apache.org/licenses/LICENSE-2.0.txt"))
    assertEquals("Apache-2.0", getFallbackId("http://www.apache.org/licenses/LICENSE-2.0.html"))
    assertEquals("Apache-2.0", getFallbackId("https://www.opensource.org/licenses/apache2.0.php"))
    assertEquals("Apache-2.0", getFallbackId("https://opensource.org/license/apache-2-0"))

    assertEquals("CC0-1.0", getFallbackId("https://creativecommons.org/publicdomain/zero/1.0"))
    assertEquals("GPL-2.0-or-later", getFallbackId("https://choosealicense.com/licenses/gpl-2.0"))
    assertEquals("EPL-2.0", getFallbackId("https://www.eclipse.org/legal/epl-2.0"))
  }
}
