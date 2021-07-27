# Change Log

## [Unreleased]


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



[Unreleased]: https://github.com/cashapp/licensee/compare/1.2.0...HEAD
[1.2.0]: https://github.com/cashapp/licensee/releases/tag/1.2.0
[1.1.0]: https://github.com/cashapp/licensee/releases/tag/1.1.0
[1.0.2]: https://github.com/cashapp/licensee/releases/tag/1.0.2
[1.0.1]: https://github.com/cashapp/licensee/releases/tag/1.0.1
[1.0.0]: https://github.com/cashapp/licensee/releases/tag/1.0.0
