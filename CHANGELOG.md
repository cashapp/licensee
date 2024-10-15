# Change Log

## [1.12.0] - 2024-10-15

**Changed**

- Update SPDX database to version 3.25 (2024-08-19).
- Add fallback URL for popular MIT variant.


## [1.11.0] - 2024-03-29

**New**

- `LicenseeTask` exposes `jsonOutput` and `validationOutput` properties which are providers of the generated file that you can use to copy elsewhere or bundle into binaries.

**Changed**

- Invalid SPDX identifiers passed to `allow` will now throw an exception.
- Update SPDX database to version 3.23 (2024-02-08).


## [1.10.0] - 2024-03-28

**New**

- Gradle platform dependencies (also sometimes called BOMs) are now ignored since they only contribute version constraints and no code.


## [1.9.1] - 2024-02-09

**Fixed**

- Avoid task name showing up in logs when logging is disabled but unused licenses are present.


## [1.9.0] - 2024-01-29

**Added**

- Add configuration option for behavior on unused license. By default we log, but you can now choose to ignore.

   ```kotlin
   licensee {
     unusedAction(IGNORE)
   }
   ```

- New fallback URL for ISC.

**Changed**

- Update SPDX database to version 3.22 (2023-10-05).


## [1.8.0] - 2023-09-26

**Changed**

- Fallback URLs now map to multiple license SPDX IDs where appropriate.
  For example, https://opensource.org/license/gpl-2-0 matches both GPL-2.0 and GPL-2.0-or-later.

- Remove the use of `afterEvalute` internally. This causes some task names to slightly change and the output folders
  to slightly change when used with the Android Gradle plugin.

- Update SPDX database to version 3.21 (2023-06-18).

- Minimum Gradle version is now 8.0


## [1.7.0] - 2023-05-05

**Added**

- When allowing a URL, a reason can now be provided using the `because` method.

    ```kotlin
    allowUrl("https://example.com/license.html") {
      because("is Apache-2.0")
    }
    ```

- Custom tasks can be created to check custom configurations or language plugins which do not have first-party support.

    ```groovy
    tasks.register('licenseeFoo', app.cash.licensee.LicenseeTask) {
      configurationToCheck configurations.foo
      outputDir.set(layout.buildDirectory.dir('reports/licenseeFoo'))
    }
    ```

- Version catalog references are now supported by `allowDependency`.

    ```kotlin
    allowDependency(libs.exam) {
      because("there are reasons!")
    }
    ```

**Changed**

 -  License URLs which map to multiple SPDX identifiers will now match against any of those identifiers.

    For example, if a license URL matches both `EXAMPLE` and `EXAMPLE-with-exemption` you can mark either of those
    IDs as allowed and the dependency will be allowed.

**Fixed**

- Support reading Maven pom files which use property substitution (`${something}`) in their XML.
- Support for Gradle configuration cache.
- `LicenseeTask` is now cachable.


## [1.6.0] - 2022-10-26

**Added**

- Support for Android dynamic feature module plugin.
- New fallback URL for EPL-2.0.

**Changed**

- Update SPDX database to version 3.18 (2022-08-12).


## [1.5.0] - 2022-07-12

**Changed**

- Upgrade AGP dependency to 7.2 which requires JDK 11 to run.

**Fixed**

- Include Gradle variant attributes when resolving POMs. This should fix issues with Kotlin JS and Kotlin multiplatform artifacts.


## [1.4.1] - 2022-06-03

**Fixed**

- Track SPDX database format change which resulted in JSON license URLs being used instead of HTML license URLs.


## [1.4.0] - 2022-05-26

**Changed**

- Update SPDX database to version 3.17 (2022-05-08).

**Fixed**

- Do not require accepting all licenses defined in a Maven POM. The specification says that multiple licenses are a logical OR, not a logical AND.
- Licenses defined in parent poms are only used when the child contains no licenses. Previously both would be honored, but the Maven POM spec defines them as overriding.
- Disable dependency verification on the Gradle configuration through which the Maven POMs are resolved.


## [1.3.1] - 2021-10-26

**Fixed**

- Support Kotlin multiplatform projects with the 'application' plugin applied.


## [1.3.0] - 2021-10-26

**Added**

- `violationAction` build DSL which allows you to choose whether to fail, log, or ignore license
  validation problems.
- New fallback URLs for popular licenses such as MIT, Apache 2, LGPL, GPL, BSD, and EPL.

**Fixed**

- Ignore flat-dir repositories which contain no artifact metadata.
- Support Kotlin multiplatform projects whose JVM target uses `withJava()`.


## [1.2.0] - 2021-07-27

**Added**

 - If the license information in a Maven POM is missing a URL, fallback to matching the name against the SPDX identifier list.


## [1.1.0] - 2021-06-25

**Added**

 - Include SCM URL in the JSON output if available from an artifacts POM.

**Fixed**

 - Support older versions of Gradle because they leak ancient versions of the Kotlin stdlib onto the plugin classpath.


## [1.0.2] - 2021-06-09

**Changed**

 - Report the offending project when the plugin fails to apply due to a missing sibling language/platform plugin.


## [1.0.1] - 2021-06-08

**Changed**

 - Include Gradle-internal resolution exception in `--info` log when a POM fails to resolve.
 - Introduce determinism when dealing with multiple license identifiers which use the same license URL. For now, the shortest identifier is selected rather than relying on order of the SPDX license JSON.


## [1.0.0] - 2021-05-21

Initial release.



[Unreleased]: https://github.com/cashapp/licensee/compare/1.12.0...HEAD
[1.12.0]: https://github.com/cashapp/licensee/releases/tag/1.12.0
[1.11.0]: https://github.com/cashapp/licensee/releases/tag/1.11.0
[1.10.0]: https://github.com/cashapp/licensee/releases/tag/1.10.0
[1.9.1]: https://github.com/cashapp/licensee/releases/tag/1.9.1
[1.9.0]: https://github.com/cashapp/licensee/releases/tag/1.9.0
[1.8.0]: https://github.com/cashapp/licensee/releases/tag/1.8.0
[1.7.0]: https://github.com/cashapp/licensee/releases/tag/1.7.0
[1.6.0]: https://github.com/cashapp/licensee/releases/tag/1.6.0
[1.5.0]: https://github.com/cashapp/licensee/releases/tag/1.5.0
[1.4.1]: https://github.com/cashapp/licensee/releases/tag/1.4.1
[1.4.0]: https://github.com/cashapp/licensee/releases/tag/1.4.0
[1.3.1]: https://github.com/cashapp/licensee/releases/tag/1.3.1
[1.3.0]: https://github.com/cashapp/licensee/releases/tag/1.3.0
[1.2.0]: https://github.com/cashapp/licensee/releases/tag/1.2.0
[1.1.0]: https://github.com/cashapp/licensee/releases/tag/1.1.0
[1.0.2]: https://github.com/cashapp/licensee/releases/tag/1.0.2
[1.0.1]: https://github.com/cashapp/licensee/releases/tag/1.0.1
[1.0.0]: https://github.com/cashapp/licensee/releases/tag/1.0.0
