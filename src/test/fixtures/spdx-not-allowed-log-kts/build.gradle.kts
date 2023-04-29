import app.cash.licensee.ViolationAction.LOG

plugins {
  `java-library`
  id("app.cash.licensee")
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  violationAction(LOG)
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
