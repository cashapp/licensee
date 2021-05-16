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
  allowDependency("com.example", "example", "1.0.0") {
    because("there are reasons!")
  }
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
