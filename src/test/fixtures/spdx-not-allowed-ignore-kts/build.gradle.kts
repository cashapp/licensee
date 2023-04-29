import app.cash.licensee.ViolationAction.IGNORE

plugins {
  `java-library`
  id("app.cash.licensee")
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  violationAction(IGNORE)
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
