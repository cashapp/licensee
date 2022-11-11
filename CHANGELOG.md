# Change Log

## [Unreleased]

**Added**

- Make unused allowed SPDX-identifier, license-url and allowed dependency warnings configurable

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



[Unreleased]: https://github.com/cashapp/licensee/compare/1.6.0...HEAD
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
