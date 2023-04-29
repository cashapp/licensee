plugins {
  `java-library`
  id("app.cash.licensee")
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
