# Licensee 📜👀

A Gradle plugin which validates the licenses of your dependency graph match what you expect, or it
fails your build!

Jump to:
[Introduction](#Introduction) |
[Usage](#Usage) |
[Configuration](#Configuration) |
[Development](#Development) |
[License](#License)

## Introduction

Imagine your closed-source product depends on an Apache 2-licensed artifact.
```groovy
implementation 'com.example:example:1.0'
```

The developer releases a new version and you dutifully upgrade.
```diff
-implementation 'com.example:example:1.0'
+implementation 'com.example:example:1.1'
```

This new version has a transitive dependency on a GPL-licensed artifact which is incompatible with
your closed-source product. Did you catch it?

You may have seen the new dependency in a [dependency tree diff][dtd], but did you check its license?

[dtd]: https://github.com/JakeWharton/dependency-tree-diff

```diff
-+--- com.example:example:1.0
++--- com.example:example:1.1
+     \--- com.other:other:2.5
```

With the Licensee plugin applied, you don't have to.
First, configure it to allow Apache 2-licensed dependencies.

```groovy
licensee {
  allow('Apache-2.0')
}
```

Now attempting to upgrade the dependency will fail the build.

```
> Task :app:licensee FAILED
com.other:other:2.5
 - SPDX identifier 'GPL-3.0-or-later' is NOT allowed
```

Crisis averted!


## Usage

Add the dependency and apply the plugin to the module whose dependency graph you want to check.

```groovy
buildscript {
  repositories {
    mavenCentral()
  }
  dependencies {
    classpath 'app.cash.licensee:licensee-gradle-plugin:1.12.0'
  }
}

apply plugin: 'app.cash.licensee'
```

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
buildscript {
  repositories {
    mavenCentral()
    maven {
      url 'https://oss.sonatype.org/content/repositories/snapshots/'
    }
  }
  dependencies {
    classpath 'app.cash.licensee:licensee-gradle-plugin:1.13.0-SNAPSHOT'
  }
}

apply plugin: 'app.cash.licensee'
```

</p>
</details>

The plugin's tasks will walk the entire transitive dependency graph, so it only needs to be applied
to "leaf" modules (those without downstream dependencies) such as applications and services.
If you are building a library, however, apply it to each library module.

Licensee requires a supported language/platform plugin to also be applied to the same module:
 * `java-library`
 * `java`
 * `com.android.application`
 * `com.android.library`
 * `org.jetbrains.kotlin.jvm`
 * `org.jetbrains.kotlin.js`
 * `org.jetbrains.kotlin.multiplatform`

Configure the list of allowed licenses using the `licensee` DSL:
```groovy
licensee {
  allow('Apache-2.0')
}
```

For more configuration options, see the [configuration section](#Configuration).

The `licensee` task will be added to your build and automatically added as a dependency of the
`check` task. Android and Kotlin multiplatform modules will have variant-specific versions of this
task (such as `licenseeDebug` and `licenseeJs`) with the `licensee` task merely aggregating them
together.

In addition to failing the build on detection of a disallowed license,
the plugin will always generate some report files.

 1. `artifacts.json`

    A JSON file with license information on every artifact in the dependency graph.

 2. `validation.txt`

    A plain text report containing each artifact and whether its licenses are allowed or disallowed.

These files are generated into `<buildDir>/reports/licensee`,
or for Android modules `<buildDir>/reports/licensee/<variant name>/`.

## Configuration

The following functions are available on the `licensee` DSL and/or
`app.cash.licensee.LicenseeExtension` type.

### `allow`

Allow artifacts with a license that matches a SPDX identifier.

```groovy
licensee {
  allow('Apache-2.0')
  allow('MIT')
}
```

A full list of supported identifiers is available at [spdx.org/licenses/](https://spdx.org/licenses/).

### `allowUrl`

Allow artifacts with an unknown (non-SPDX) license which matches a URL.

```groovy
licensee {
  allowUrl('https://example.com/license.html')
}
```

A reason string can be supplied to document why the URL is allowed.

```groovy
licensee {
  allowUrl('https://example.com/license.html') {
    because 'Apache-2.0, but self-hosted copy of the license'
  }
}
```

### `allowDependency`

Allow an artifact with a specific groupId, artifactId, and version.
This is useful for artifacts which contain no license data or have invalid/incorrect license data.

```groovy
licensee {
  allowDependency('com.example', 'example', '1.0')
}
```

A reason string can be supplied to document why the dependency is being allowed despite missing or invalid license data.

```groovy
licensee {
  allowDependency('com.jetbrains', 'annotations', '16.0.1') {
    because 'Apache-2.0, but typo in license URL fixed in newer versions'
  }
}
```

Reason strings will be included in validation reports.

### `ignoreDependencies`

Ignore a single dependency or group of dependencies during dependency graph resolution.
Artifacts targeted with this method will not be analyzed for license information and will not show up in any report files.

This function can be used to ignore internal, closed-source libraries and commercial libraries for which you've purchased a license.

There are overloads which accept either a groupId or a groupId:artifactId pair.

```groovy
licensee {
  ignoreDependencies('com.mycompany.internal')
  ignoreDependencies('com.mycompany.utils', 'utils')
}
```

A reason string can be supplied to document why the dependencies are being ignored.

```groovy
licensee {
  ignoreDependencies('com.example.sdk', 'sdk') {
    because "commercial SDK"
  }
}
```

An ignore can be marked as transitive which will ignore an entire branch of the dependency tree.
This will ignore the target artifact's dependencies regardless of the artifact coordinates or license info.
Since it is especially dangerous, a reason string is required.

```groovy
licensee {
  ignoreDependencies('com.other.sdk', 'sdk') {
    transitive = true
    because "commercial SDK"
  }
}
```


## Development

### Updating embedded license info

Run the `updateLicenses` task to update the embedded SPDX license file in `src/main/resources/`.


# License

    Copyright 2021 Square, Inc.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
