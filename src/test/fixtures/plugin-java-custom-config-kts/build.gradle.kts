plugins {
  id("java")
  alias(libs.plugins.licensee)
}

val foo by configurations.registering

dependencies {
  foo("com.example:example:1.0.0")
}

licensee {
  allow("Apache-2.0")
}

tasks.register<app.cash.licensee.LicenseeTask>("licenseeFoo") {
  configurationToCheck(foo)
  outputDir.set(layout.buildDirectory.dir("reports/licenseeFoo"))
}
