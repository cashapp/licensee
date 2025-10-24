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

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class UrlNormalizerTest {

  @Test
  fun `normalizeUrl lowercase`() {
    assertThat(normalizeUrl("HTTP://EXAMPLE.COM")).isEqualTo("http://example.com/")
  }

  @Test
  fun `normalizeUrl adds trailing slash`() {
    assertThat(normalizeUrl("http://example.com")).isEqualTo("http://example.com/")
  }

  @Test
  fun `normalizeUrl keeps existing trailing slash`() {
    assertThat(normalizeUrl("http://example.com/")).isEqualTo("http://example.com/")
  }

  @Test
  fun `normalizeUrl handles path`() {
    assertThat(normalizeUrl("http://example.com/path")).isEqualTo("http://example.com/path/")
  }

  @Test
  fun `normalizeUrl handles path with trailing slash`() {
    assertThat(normalizeUrl("http://example.com/path/")).isEqualTo("http://example.com/path/")
  }

  @Test
  fun `normalizeUrl handles query parameters`() {
    assertThat(normalizeUrl("http://example.com/path?query=param")).isEqualTo("http://example.com/path?query=param")
  }

  @Test
  fun `normalizeUrl handles fragment`() {
    assertThat(normalizeUrl("http://example.com/path#fragment")).isEqualTo("http://example.com/path#fragment")
  }

  @Test
  fun `normalizeUrl handles complex URL`() {
    assertThat(normalizeUrl("HTTP://EXAMPLE.COM/PATH/?query=param#fragment")).isEqualTo("http://example.com/path/?query=param#fragment")
  }

  @Test
  fun `normalizeUrl handles invalid URL`() {
    assertThat("invalid url").isEqualTo(normalizeUrl("invalid url"))
  }
}
