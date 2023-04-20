pluginManagement {
  resolutionStrategy.eachPlugin {
    if (requested.id.id == "app.cash.licensee") {
      val licenseeVersion: String by settings
      useVersion(licenseeVersion)
    }
  }
  repositories {
    maven(url = "file://${settingsDir.absolutePath}/../../../../build/localMaven")
    mavenCentral()
  }
}

include(":")
