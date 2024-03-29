import app.cash.licensee.UnusedAction.LOG

plugins {
  id("java-library")
  alias(libs.plugins.licensee)
}

dependencies {
  implementation("com.example:example:1.0.0")
}

licensee {
  allowDependency("com.example", "example", "1.0.0")
  allowDependency("com.example", "example2", "1.0.0")
  unusedAction(LOG)
}
