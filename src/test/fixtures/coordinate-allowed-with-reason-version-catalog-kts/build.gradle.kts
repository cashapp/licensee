plugins {
  `java-library`
  id("app.cash.licensee")
}

dependencies {
  implementation(libs.exam)
}

licensee {
  allowDependency(libs.exam) {
    because("there are reasons!")
  }
}

repositories {
  maven {
    setUrl("file://${rootDir.absolutePath}/repo")
  }
}
