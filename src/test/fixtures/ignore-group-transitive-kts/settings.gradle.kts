pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "app.cash.licensee") {
        useVersion(settings.extra["licenseeVersion"].toString())
      }
    }
  }

  repositories {
    maven {
      setUrl("file://${rootDir.absolutePath}/../../../../build/localMaven")
    }
    mavenCentral()
  }
}

include(":")
