plugins {
  `java-library`
  id("app.cash.licensee")
}

dependencies {
  implementation("com.example:example-a:1.0.0")
}

licensee {
  allow("Apache-2.0")
  ignoreDependencies("com.example")
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
