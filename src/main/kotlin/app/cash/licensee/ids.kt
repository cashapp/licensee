/*
 * Copyright (C) 2022 Square, Inc.
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

import java.io.Serializable

sealed class Id : Serializable {
  internal abstract operator fun contains(other: Id): Boolean
  internal abstract fun matches(id: String): Boolean
}

private class LiteralId(val id: String) : Id() {
  override fun contains(other: Id): Boolean {
    return when (other) {
      is LiteralId -> this.id == other.id
      is RegexId -> Regex.escape(this.id) == other.id.pattern // Best effort.
    }
  }

  override fun matches(id: String): Boolean {
    return this.id == id
  }

  override fun toString(): String {
    return this.id
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as LiteralId

    if (id != other.id) return false

    return true
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }
}

private class RegexId(val id: Regex) : Id() {
  override fun contains(other: Id): Boolean {
    return when (other) {
      is LiteralId -> this.id.matches(other.id)
      is RegexId -> this.id.pattern == other.id.pattern // Best effort.
    }
  }

  override fun matches(id: String): Boolean {
    return this.id.matches(id)
  }

  override fun toString(): String {
    return "/${this.id}/"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RegexId

    if (id.pattern != other.id.pattern) return false

    return true
  }

  override fun hashCode(): Int {
    return id.pattern.hashCode()
  }
}

internal fun String.toLiteralId(): Id = LiteralId(this)
internal fun String.toRegexId(): Id = RegexId(this.toRegex())

// Intentionally specific key type.
internal inline fun <V> Map<Id, V>.firstOrNull(predicate: (key: Id) -> Boolean): V? {
  return entries.firstOrNull { (key, _) -> predicate(key) }?.value
}

internal fun Pair<Id, Id>.matches(groupId: String, moduleId: String): Boolean {
  return first.matches(groupId) && second.matches(moduleId)
}
