# Change Log

## [Unreleased]


## [1.0.1] - 2021-06-08

**Changed**

 - Include Gradle-internal resolution exception in `--info` log when a POM fails to resolve.
 - Introduce determinism when dealing with multiple license identifiers which use the same license URL. For now, the shortest identifier is selected rather than relying on order of the SPDX license JSON.


## [1.0.0] - 2021-05-21

Initial release.



[Unreleased]: https://github.com/cashapp/licensee/compare/1.0.1...HEAD
[1.0.1]: https://github.com/cashapp/licensee/releases/tag/1.0.1
[1.0.0]: https://github.com/cashapp/licensee/releases/tag/1.0.0
