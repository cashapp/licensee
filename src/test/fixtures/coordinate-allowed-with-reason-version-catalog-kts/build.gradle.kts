plugins {
  id("java-library")
  alias(libs.plugins.licensee)
}

dependencies {
  implementation(libs.exam)
}

licensee {
  allowDependency(libs.exam) {
    because("there are reasons!")
  }
}
