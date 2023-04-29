plugins {
  `java-library`
  id("app.cash.licensee")
}

dependencies {
  implementation("com.example:example-a:1.0.0")
  implementation("com.other:other-b:1.0.0")
}

licensee {
  allow("Apache-2.0")
  ignoreDependencies("com.example") {
    transitive = true
    because("reasons")
  }
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
