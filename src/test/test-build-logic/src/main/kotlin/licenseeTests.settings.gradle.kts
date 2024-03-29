pluginManagement {
  repositories {
    maven(url = "file://${settingsDir.absolutePath}/../../../../build/localMaven")
    mavenCentral()
    google()
  }
}

dependencyResolutionManagement {
  versionCatalogs.register("libs") {
    from(files("../../../../gradle/libs.versions.toml"))

    // This version is set in the GradleRunner during test setup using the current (SNAPSHOT) version.
    // If you want to use the test projects as standalone samples, link the Gradle project in IntelliJ
    // and overwrite this version.
    val licenseeVersion: String by settings
    plugin("licensee", "app.cash.licensee").version(licenseeVersion)
  }

  repositories {
    maven(url = "file://${rootDir.absolutePath}/repo")
    mavenCentral()
    google()
  }
}
