plugins {
  `java-library`
  id("app.cash.licensee")
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  allowUrl("https://example.com/license")
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
