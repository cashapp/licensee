import app.cash.licensee.ViolationAction.IGNORE

buildscript {
  apply(from = "../../../../dependencies.gradle")
}

plugins {
  `java-library`
  id("app.cash.licensee") version property("licenseeVersion").toString()
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
