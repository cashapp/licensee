pluginManagement {
  repositories {
    maven {
      setUrl("file://${rootDir.absolutePath}/../../../../build/localMaven")
    }
    mavenCentral()
  }
}

include(":")
