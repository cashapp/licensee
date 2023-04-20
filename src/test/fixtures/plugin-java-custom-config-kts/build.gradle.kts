plugins {
  java
  id("app.cash.licensee")
}

val foo by configurations.registering

dependencies {
  // https://github.com/gradle/gradle/issues/24503
  foo.name("com.example:example:1.0.0")
}

licensee {
  allow("Apache-2.0")
}

tasks.register<app.cash.licensee.LicenseeTask>("licenseeFoo") {
  configurationToCheck(foo)
  outputDir.set(layout.buildDirectory.dir("reports/licenseeFoo"))
}

repositories {
  maven(url = "file://${rootDir.absolutePath}/repo")
}
