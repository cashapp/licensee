buildscript {
  apply(from = "../../../../dependencies.gradle")
}

plugins {
  `java-library`
  id("app.cash.licensee") version property("licenseeVersion").toString()
}

dependencies {
  implementation("com.example:example-a:1.0.0")
  implementation("com.example:example-b:1.0.0")
}

licensee {
  allow("Apache-2.0")
  ignoreDependencies("com.example", "example-a") {
    transitive = true
    because("reasons")
  }
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
